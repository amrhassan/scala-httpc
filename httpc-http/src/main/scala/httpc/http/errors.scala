package httpc.http

import cats.Show
import cats.implicits._

sealed trait HttpError {
  override def toString: String = this.show
}

object HttpError {

  case class MalformedHeader(header: String) extends HttpError
  case class MalformedStatus(status: String) extends HttpError
  case class NetworkError(message: String) extends HttpError
  case class UnsupportedProtocol(protocol: String) extends HttpError
  case object CorruptedContentLength extends HttpError
  case class UnsupportedTransferEncoding(name: String) extends HttpError
  case object UnspecifiedTransferModeError extends HttpError

  implicit val showHttpError: Show[HttpError] = Show.show(render)

  def render(err: HttpError): String = "HTTP error: " |+| (err match {
    case MalformedHeader(header) ⇒ s"malformed header: $header"
    case NetworkError(message) ⇒ s"$message"
    case MalformedStatus(status) ⇒ s"malformed status: $status"
    case UnsupportedProtocol(protocol) ⇒ s"unsupported protocol $protocol"
    case CorruptedContentLength => s"corrupted Content-Length value"
    case UnsupportedTransferEncoding(name) => s"unsupported Transfer-Encoding protocol $name"
    case UnspecifiedTransferModeError => s"unspecified Content-Length or Transfer-Encoding"
  })
}
