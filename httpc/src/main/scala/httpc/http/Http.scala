package httpc.http

import scala.concurrent.ExecutionContext
import cats.data.Xor
import cats.implicits._
import HttpAction._
import httpc.net.{Bytes, ConnectionId}
import httpc.net


/** Module API */
trait Http {

  val HttpVersion = "HTTP/1.1".getBytes.toVector

  /** Dispatches an HTTP request and yields a response for it */
  def dispatch(con: net.ConnectionId, r: Request)(implicit ec: ExecutionContext): HttpAction[Response] =
    for {
      status ← readStatus(con)
      headers ← readHeaders(con)
      bodySize ← bodySizeFromHeaders(headers)
      body ← fromNetIo(net.read(con, bodySize))
    } yield Response(status, headers, body)

  private def bodySizeFromHeaders(headers: List[Header]): HttpAction[net.Length] = xor {
    for {
      header ← headers.find(_.name == HeaderNames.ContentLength).toRightXor(HttpError.MissingContentLength)
      value ← Xor.catchNonFatal(header.value.toInt).leftMap(_ ⇒ HttpError.MissingContentLength)
      size ← net.length(value).toRightXor(HttpError.MissingContentLength)
    } yield size
  }

  private def readHeaders(con: ConnectionId)(implicit ec: ExecutionContext): HttpAction[List[Header]] = {
    def readHeader(line: Vector[Byte]): HttpAction[Header] = xor {
      Header.read(line).toRightXor(HttpError.MalformedHeader(new String(line.toArray).trim))
    }
    readLine(con) >>= { line ⇒
      if (Bytes.isWhitespace(line)) {
        HttpAction.pure(List.empty)
      } else {
        readHeader(line) >>= { header ⇒
          readHeaders(con) map (header :: _)
        }
      }
    }
  }

  private def readStatus(con: ConnectionId)(implicit ec: ExecutionContext): HttpAction[Status] =
    readLine(con) >>= { line ⇒
      val parts = Bytes.split(line, ' '.toByte)
      val status = Status.read(parts(1)).toRightXor(HttpError.MalformedStatus(Bytes.toString(line).trim))
      HttpAction.xor(status)
    }

  private def readLine(con: ConnectionId)(implicit ec: ExecutionContext): HttpAction[Vector[Byte]] = fromNetIo {
    net.readUntil(con, '\n'.toByte)
  }
}
