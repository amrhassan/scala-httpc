package httpc.convenient

import cats.Show
import httpc.http
import cats.implicits._

sealed trait HttpcError
case class HttpError(e: http.HttpError) extends HttpcError
case class MalformedUrl(url: String) extends HttpcError
case class UnsupportedCompression(name: String) extends HttpcError
case object CorruptedCompressedResponse extends HttpcError
case object CorruptedUtf8Response extends HttpcError

object HttpcError {
  implicit val httpcErrorShow: Show[HttpcError] = Show.show {
    case HttpError(err) => err.show
    case MalformedUrl(url) => s"Malformed url: $url"
    case UnsupportedCompression(name) => s"unsupported compression $name"
    case CorruptedCompressedResponse => s"corrupted compressed response body"
    case CorruptedUtf8Response => s"corrupted UTF8 response body"
  }
}
