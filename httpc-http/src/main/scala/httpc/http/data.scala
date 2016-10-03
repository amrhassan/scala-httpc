package httpc.http

import scala.util.Try
import cats.implicits._
import httpc.net.{Bytes, Port}
import enumeratum._
import Render.ops._
import Render._


object HeaderNames {
  val ContentLength = "Content-Length"
  val Host = "Host"
  val ContentType = "Content-Type"
}

/** An HTTP header */
case class Header(name: String, value: String)

object Header {

  implicit val renderHeader: Render[Header] = Render { header =>
    s"${header.name}: ${header.value}".getBytes.toVector
  }

  /** Reads a Header from a sequence of bytes */
  private [http] def read(bytes: Vector[Byte]): Option[Header] = {
    val line = new String(bytes.toArray)
    if (line contains ':') {
      val (key, value) = line.splitAt(line.indexOf(':'))
      Some(Header(key.trim, value.drop(1).trim))
    } else None
  }

  /** Content-Type header */
  def contentType(value: String): Header =
    Header(HeaderNames.ContentType, value)

  /** Host header */
  def host(hostname: String): Header =
    Header(HeaderNames.Host, hostname)

  /** Content-Length header */
  def contentLength(length: Int): Header =
    Header(HeaderNames.ContentLength, length.toString)
}

object Headers {

  /** Overwrites the base headers with custom headers */
  def overwrite(base: List[Header], custom: List[Header]): List[Header] =
    (toMap(base) ++ toMap(custom)).values.toList

  /** Constructs a mapping using each header's name */
  def toMap(headers: Traversable[Header]): Map[String, Header] =
    Maps.fromTraversable[String, Header](headers, _.name)

  /** Determines response body size given the response headers */
  def determineBodySize(headers: List[Header]): Option[Int] =
    headers.find(_.name == HeaderNames.ContentLength) >>= (header => Try(header.value.toInt).toOption)
}

/** An HTTP message */
case class Message(headers: List[Header], body: Array[Byte])

object Message {

  implicit val renderMessage: Render[Message] = Render { message =>
    val headers = (message.headers map (_.render |+| newline)).foldK |+| newline
    val body = message.body.toVector
    headers |+| body
  }
}

sealed trait Method extends EnumEntry

object Method extends Enum[Method] {

  implicit val methodRender: Render[Method] = Render { method =>
    val str = method match {
      case Get ⇒ "GET"
      case Put ⇒ "PUT"
      case Patch ⇒ "PATCH"
      case Post ⇒ "POST"
      case Delete ⇒ "DELETE"
      case Options ⇒ "OPTIONS"
      case Trace ⇒ "Trace"
      case Head ⇒ "HEAD"
    }
    str.getBytes.toVector
  }


  case object Get extends Method
  case object Put extends Method
  case object Patch extends Method
  case object Post extends Method
  case object Delete extends Method
  case object Options extends Method
  case object Trace extends Method
  case object Head extends Method

  val values: Seq[Method] = findValues
}

case class Path(value: String)

object Path {
  implicit val pathRender: Render[Path] = Render(_.value.getBytes.toVector)
}

/** An HTTP request */
case class Request(method: Method, path: Path, message: Message)

object Request {

  implicit val renderRequest: Render[Request] = Render { r =>
    r.method.render |+| space |+| r.path.render |+| space |+| HttpVersion |+| newline |+|
      r.message.render
  }
}

case class Status(value: Int)

object Status {
  private [http] def read(bytes: Vector[Byte]): Option[Status] =
    Either.catchNonFatal(Bytes.toString(bytes).toInt).toOption map Status.apply
}

/** An HTTP response */
case class Response(status: Status, headers: List[Header], body: Array[Byte]) {

  /** Decodes the body as UTF-8 text */
  def text: Option[String] =
    Either.catchNonFatal(new String(body, "UTF-8")).toOption
}

case class Url(protocol: String, host: String, port: Option[Port], path: Path)

object Url {
  def parse(url: String): Option[Url] =
    Either.catchNonFatal(new java.net.URL(url)).toOption map { parsed ⇒
      val path = parsed.getPath
      Url(parsed.getProtocol, parsed.getHost, Port.fromInt(parsed.getPort), Path(if (path.isEmpty) "/" else path))
    }
}
