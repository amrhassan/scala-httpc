package httpc.convenient

import cats.data.EitherT
import httpc.http.{HeaderNames, Response}
import cats.implicits._

case class ResponseOps(response: Response) extends AnyVal {

  private [httpc] def compression: EitherT[Option, HttpcError, Compression] = EitherT {
    response.headers.find(_.name == HeaderNames.ContentEncoding) map { header =>
      Compression.forName(header.value)
    }
  }

  def uncompressedBody: Either[HttpcError, Array[Byte]] = {
    val compressed = compression >>= (c => EitherT.fromEither(Compression.decode(c)(response.body)))
    compressed.value.getOrElse(Either.right(response.body))
  }

  /** Decodes the body as UTF-8 text */
  def utf8: Either[HttpcError, String] =
    uncompressedBody >>=
      (bytes => Either.catchNonFatal(new String(response.body, "UTF-8")).left.map(_ => CorruptedUtf8Response))

  @deprecated("Use Response.utf8 instead", "0.3.0")
  def text = utf8
}
