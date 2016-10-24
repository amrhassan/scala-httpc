package httpc.http

import cats.implicits._
import httpc.net.{Bytes, Port}
import enumeratum._
import Render.ops._
import Render._
import scodec.bits.ByteVector
import httpc.net.ScodecInstances._


trait HeaderNames {
  val ContentLength = "Content-Length"
  val Host = "Host"
  val ContentType = "Content-Type"
  val TransferEncoding = "Transfer-Encoding"
  val CacheControl = "Cache-Control"
}

object HeaderNames extends HeaderNames

/** An HTTP header */
case class Header(name: String, value: String)

object Header extends HeaderConstruction {

  implicit val renderHeader: Render[Header] = Render { header =>
    Bytes.fromUtf8(s"${header.name}: ${header.value}")
  }

  /** Reads a Header from a sequence of bytes */
  private [http] def read(bytes: ByteVector): Option[Header] = {
    val line = Bytes.toString(bytes)
    if (line contains ':') {
      val (key, value) = line.splitAt(line.indexOf(':'))
      Some(Header(key.trim, value.drop(1).trim))
    } else None
  }
}

trait HeaderConstruction {

   /** Content-Type header */
  def contentType(value: String): Header =
    Header(HeaderNames.ContentType, value)

  /** Host header */
  def host(hostname: String): Header =
    Header(HeaderNames.Host, hostname)

  /** Content-Length header */
  def contentLength(length: Long): Header =
    Header(HeaderNames.ContentLength, length.toString)

  val transferEncodingChunked: Header =
    Header(HeaderNames.TransferEncoding, "chunked")

  val cacheControlNoCache: Header =
    Header(HeaderNames.CacheControl, "no-cache")
}

/** An HTTP message */
case class Message(headers: List[Header], body: ByteVector)

object Message {

  implicit val renderMessage: Render[Message] = Render { message =>
    val headers = (message.headers map (_.render |+| newline)).combineAll |+| newline
    val body = message.body
    headers |+| body
  }
}

sealed trait Method extends EnumEntry

object Method extends Enum[Method] {

  implicit val methodRender: Render[Method] =
    Render.renderUtf8 contramap {
      case Get ⇒ "GET"
      case Put ⇒ "PUT"
      case Patch ⇒ "PATCH"
      case Post ⇒ "POST"
      case Delete ⇒ "DELETE"
      case Options ⇒ "OPTIONS"
      case Trace ⇒ "Trace"
      case Head ⇒ "HEAD"
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
  implicit val pathRender: Render[Path] = Render.renderUtf8 contramap (_.value)
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
  private [http] def read(bytes: ByteVector): Option[Status] =
    Either.catchNonFatal(Bytes.toString(bytes).toInt).toOption map Status.apply
}

/** An HTTP response */
case class Response(status: Status, headers: List[Header], body: ByteVector) {

  /** Decodes the body as UTF-8 text */
  def text: Option[String] =
    Either.catchNonFatal(Bytes.toString(body)).toOption
}

case class Url(protocol: String, host: String, port: Option[Port], path: Path)

object Url {
  def parse(url: String): Option[Url] =
    Either.catchNonFatal(new java.net.URL(url)).toOption map { parsed ⇒
      val path = parsed.getPath
      Url(parsed.getProtocol, parsed.getHost, Port.fromInt(parsed.getPort), Path(if (path.isEmpty) "/" else path))
    }
}
