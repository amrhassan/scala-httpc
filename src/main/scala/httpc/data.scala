package httpc

import cats.implicits._

/** An HTTP header */
case class Header(name: String, value: String)

object Header {

  /** Renders a Header into bytes */
  def render(header: Header): Vector[Byte] =
    s"${header.name}: ${header.value}".getBytes.toVector
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

  case object Get extends Method
  case object Put extends Method

  def render(m: Method): Vector[Byte] = (m match {
    case Get ⇒ "GET"
    case Put ⇒ "PUT"
  }).getBytes.toVector
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
  private val http = "HTTP/1.1".getBytes.toVector

  def render(r: Request): Vector[Byte] =
    Method.render(r.method) :+ space |+| Path.render(r.path) :+ space |+| http :+ newline |+| Message.render(r.message)
}

///** An HTTP response */
//case class Response(message: Message)
