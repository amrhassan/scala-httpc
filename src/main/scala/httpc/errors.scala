package httpc

import cats.Show
import cats.implicits._

sealed trait HttpError

object HttpError {
  case class MalformedHeader(header: String) extends HttpError
  case class NetworkError(message: String) extends HttpError

  implicit val showHttpError: Show[HttpError] = Show.show(render)

  def render(err: HttpError): String = "HTTP error: " |+| (err match {
    case MalformedHeader(header) ⇒ s"malformed header: $header"
    case NetworkError(message) ⇒ s"network error: $message"
  })
}
