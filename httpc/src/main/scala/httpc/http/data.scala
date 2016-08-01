package httpc.http

import cats.data.Xor
import cats.implicits._
import httpc.net.Bytes
import httpc.net
import HttpAction._
import httpc.http.HttpError.MalformedUrl


object HeaderNames {
  val ContentLength = "Content-Length"
  val Host = "Host"
  val ContentType = "Content-Type"
}

/** An HTTP header */
case class Header(name: String, value: String)

object Header {

  /** Renders a Header into bytes */
  def render(header: Header): Vector[Byte] =
    s"${header.name}: ${header.value}".getBytes.toVector

  /** Reads a Header from a sequence of bytes */
  def read(bytes: Vector[Byte]): Option[Header] = {
    val line = new String(bytes.toArray)
    if (line contains ':') {
      val (key, value) = line.splitAt(line.indexOf(':'))
      Some(Header(key.trim, value.drop(1).trim))
    } else None
  }
}

object Headers {

  /** Content-Type header */
  def contentType(value: String): Header =
    Header(HeaderNames.ContentType, value)
}

/** An HTTP message */
case class Message(headers: List[Header], body: Array[Byte])

object Message {

  /** Renders a message into bytes */
  def render(r: Message): Vector[Byte] =
    renderHeaders(r) |+| renderBody(r)

  /** Renders headers into bytes */
  def renderHeaders(r: Message): Vector[Byte] =
    r.headers.map(h ⇒ Header.render(h) :+ '\n'.toByte).foldK :+ '\n'.toByte

  /** Renders body */
  def renderBody(r: Message): Vector[Byte] =
    r.body.toVector
}

sealed trait Method

object Method {

  import Methods._

  def render(m: Method): Vector[Byte] = (m match {
    case Get ⇒ "GET"
    case Put ⇒ "PUT"
    case Post ⇒ "POST"
    case Delete ⇒ "DELETE"
    case Options ⇒ "OPTIONS"
    case Trace ⇒ "Trace"
    case Head ⇒ "HEAD"
  }).getBytes.toVector
}

object Methods {
  case object Get extends Method
  case object Put extends Method
  case object Post extends Method
  case object Delete extends Method
  case object Options extends Method
  case object Trace extends Method
  case object Head extends Method
}

case class Path(path: String)

object Path {
  def render(p: Path): Vector[Byte] =
    p.path.getBytes.toVector
}

/** An HTTP request */
case class Request(method: Method, path: Path, message: Message)

object Request {

  private val space = ' '.toByte
  private val newline = '\n'.toByte

  def render(r: Request): Vector[Byte] =
    Method.render(r.method) :+ space |+| Path.render(r.path) :+ space |+|
      HttpVersion :+ newline |+| Message.render(r.message)
}

case class Status(value: Int)

object Status {
  def read(bytes: Vector[Byte]): Option[Status] =
    Xor.catchNonFatal(Bytes.toString(bytes).toInt).toOption map Status.apply
}

/** An HTTP response */
case class Response(status: Status, headers: List[Header], body: Array[Byte]) {

  /** Decodes the body as UTF-8 text */
  def text: Option[String] =
    Xor.catchNonFatal(new String(body, "UTF-8")).toOption
}

case class Url(protocol: String, host: String, port: Option[net.Port], path: String)

object Url {
  def parse(url: String): HttpAction[Url] = xor {
    Xor.catchNonFatal(new java.net.URL(url)).leftMap(_ ⇒ MalformedUrl(url)) map { parsed ⇒
      Url(parsed.getProtocol, parsed.getHost, net.Port.fromInt(parsed.getPort), parsed.getPath)
    }
  }
}
