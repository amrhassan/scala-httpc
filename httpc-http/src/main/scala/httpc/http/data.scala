package httpc.http

import cats.implicits._
import httpc.net.{Bytes, Port}
import enumeratum._
import Render.ops._
import Render._
import cats.{Monoid, Show}
import scodec.bits.ByteVector
import httpc.net.ScodecInstances._
import cats.implicits._


trait HeaderNames {
  val ContentLength = "Content-Length"
  val Host = "Host"
  val ContentType = "Content-Type"
  val TransferEncoding = "Transfer-Encoding"
  val CacheControl = "Cache-Control"
  val UserAgent = "User-Agent"
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

  def userAgent(value: String): Header =
    Header(HeaderNames.UserAgent, value)
}

case class Headers private(data: Map[String, Header]) {

  def overwriteWith(other: Headers): Headers =
    Headers(data ++ other.data)

  lazy val toList: List[Header] =
    data.values.toList
}

object Headers {

  def apply(headers: Header*): Headers =
    Headers(headers)

  def apply(headers: TraversableOnce[Header]): Headers =
    Headers(headers.map(h => (h.name, h)).toMap)

  implicit val monoidHeaders: Monoid[Headers] = new Monoid[Headers] {
    val empty: Headers = Headers.empty
    def combine(x: Headers, y: Headers): Headers = Headers(x.toList |+| y.toList)
  }

  val empty: Headers =
    Headers()
}

/** An HTTP message */
case class Message(headers: Headers, body: ByteVector)

object Message {

  implicit val renderMessage: Render[Message] = Render { message =>
    val headers = (message.headers.toList map (_.render |+| newline)).combineAll |+| newline
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

case class Resource(path: Option[String] = None, query: Option[String] = None, fragment: Option[String] = None)

object Resource {
  implicit val renderPath: Render[Resource] = Render.renderUtf8 contramap (_.show)
  implicit val showPath: Show[Resource] = Show.show {
    case Resource(path, query, fragment) =>
      path.getOrElse("/") |+| query.map("?" |+| _).getOrElse("") |+| fragment.map("#" |+| _).getOrElse("")
  }
}

/** An HTTP request */
case class Request(method: Method, resource: Resource, message: Message)

object Request {

  implicit val renderRequest: Render[Request] = Render { r =>
    r.method.render |+| space |+| r.resource.render |+| space |+| HttpVersion |+| newline |+|
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

case class Url(protocol: String, auth: Option[String], host: String, port: Option[Port], resource: Resource)

object Url {
  def parse(url: String): Option[Url] =
    Either.catchNonFatal(new java.net.URL(url)).toOption map { parsed ⇒
      val resource = Resource(
        Option(parsed.getPath),
        Option(parsed.getQuery),
        Option(parsed.getRef)
      )
      Url(parsed.getProtocol, Option(parsed.getUserInfo), parsed.getHost, Port.fromInt(parsed.getPort), resource)
    }

  implicit val showUrl: Show[Url] = Show.show {
    case Url(protocol, auth, host, port, resource) =>
      (protocol |+| "://")                            |+|
      auth.map(_ |+| "@").getOrElse("")               |+|
      host                                            |+|
      port.map(":" |+| _.show.toString).getOrElse("") |+|
      resource.show
  }
}
