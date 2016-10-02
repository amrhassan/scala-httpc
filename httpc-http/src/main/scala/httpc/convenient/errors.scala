package httpc.convenient

import cats.Show
import httpc.http
import cats.implicits._

sealed trait HttpcError
case class HttpError(e: http.HttpError) extends HttpcError
case class MalformedUrl(url: String) extends HttpcError

object HttpcError {
  implicit val httpcErrorShow: Show[HttpcError] = Show.show {
    case HttpError(err) => err.show
    case MalformedUrl(url) => s"Malformed url: $url"
  }
}
