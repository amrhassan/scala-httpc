package httpc.http

import scala.concurrent.{ExecutionContext, Future}
import cats.data.Xor
import cats.implicits._
import HttpAction._
import httpc.net.{Bytes, ConnectionId, NetInterpreters}
import httpc.net


/** Module API */
trait Http {

  val HttpVersion = "HTTP/1.1".getBytes.toVector

  /** Dispatches an HTTP request and yields a response for it */
  def dispatch(url: Url, r: Request)(implicit ec: ExecutionContext): HttpAction[Response] =
    for {
      netProtocol ← Requests.netProtocol(url)
      address ← fromNetIo(net.lookupAddress(url.host))
      con ← fromNetIo(netProtocol.connect(address, url.port.getOrElse(netProtocol.defaultPort)))
      _ ← fromNetIo(net.write(con, Request.render(r).toArray))
      status ← readStatus(con)
      headers ← readHeaders(con)
      bodySize ← bodySizeFromHeaders(headers)
      body ← fromNetIo(net.read(con, bodySize))
      _ ← fromNetIo(net.disconnect(con))
    } yield Response(status, headers, body)

  private def bodySizeFromHeaders(headers: List[Header]): HttpAction[Int] = xor {
    for {
      header ← headers.find(_.name == HeaderNames.ContentLength).toRightXor(HttpError.MissingContentLength)
      value ← Xor.catchNonFatal(header.value.toInt).leftMap(_ ⇒ HttpError.MissingContentLength)
    } yield value
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

  def run[A](command: HttpAction[A])(implicit ec: ExecutionContext): Future[HttpError Xor A] =
    HttpAction.run(command, netInterpreter).value

  private def netInterpreter(implicit ec: ExecutionContext): net.Interpreter = NetInterpreters.socketsInterpreter
}
