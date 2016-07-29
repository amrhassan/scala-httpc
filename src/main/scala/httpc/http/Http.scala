package httpc.http

import scala.concurrent.ExecutionContext
import cats.data.Xor
import cats.implicits._
import HttpIo._
import httpc.net.{ConnectionId, NetIo, Bytes}
import httpc.net


object Http {

  val Version = "HTTP/1.1".getBytes.toVector

  val Port = net.Port.fromInt(80).getOrElse(throw new RuntimeException("Invalid HTTP port"))

  /** Executes an HTTP request */
  def execute(address: net.Address, r: Request, port: net.Port)(implicit ec: ExecutionContext): HttpIo[Response] =
    for {
      con ← fromNetIo(NetIo.connect(address, port))
      _ ← fromNetIo(NetIo.write(con, Request.render(r).toArray))
      status ← readStatus(con)
      headers ← readHeaders(con)
      bodySize ← bodySizeFromHeaders(headers)
      body ← fromNetIo(NetIo.read(con, bodySize))
    } yield Response(status, headers, body.toArray)

  private def bodySizeFromHeaders(headers: List[Header]): HttpIo[net.Length] = xor {
    for {
      header ← headers.find(_.name == HeaderNames.ContentLength).toRightXor(HttpError.MissingContentLength)
      value ← Xor.catchNonFatal(header.value.toInt).leftMap(_ ⇒ HttpError.MissingContentLength)
      size ← net.length(value).toRightXor(HttpError.MissingContentLength)
    } yield size
  }

  private def readHeaders(con: ConnectionId)(implicit ec: ExecutionContext): HttpIo[List[Header]] = {
    def readHeader(line: Vector[Byte]): HttpIo[Header] = xor {
      Header.read(line).toRightXor(HttpError.MalformedHeader(new String(line.toArray).trim))
    }
    readLine(con) >>= { line ⇒
      if (Bytes.isWhitespace(line)) {
        HttpIo.pure(List.empty)
      } else {
        readHeader(line) >>= { header ⇒
          readHeaders(con) map (header :: _)
        }
      }
    }
  }

  private def readStatus(con: ConnectionId)(implicit ec: ExecutionContext): HttpIo[Status] =
    readLine(con) >>= { line ⇒
      val parts = Bytes.split(line, ' '.toByte)
      val status = Status.read(parts(1)).toRightXor(HttpError.MalformedStatus(Bytes.toString(line).trim))
      HttpIo.xor(status)
    }

  private def readLine(con: ConnectionId)(implicit ec: ExecutionContext): HttpIo[Vector[Byte]] = fromNetIo {
    NetIo.readUntil(con, '\n'.toByte)
  }
}
