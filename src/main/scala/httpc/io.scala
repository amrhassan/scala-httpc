package httpc

import scala.concurrent.{ExecutionContext, Future}
import cats.data.{Kleisli, Xor, XorT}
import cats.implicits._
import httpc.net.{ConnectionId, NetIo, Port}


object HttpIo {

  def run[A](command: HttpIo[A], netInt: NetIo.Interpreter, ec: ExecutionContext): XorT[Future, HttpError, A] =
    command.run(netInt)

  def fromNetIo[A](command: NetIo[A])(implicit ec: ExecutionContext): HttpIo[A] = Kleisli {
    (netInt: NetIo.Interpreter) ⇒
      NetIo.run(command, netInt) leftMap (e ⇒ HttpError.NetworkError(e.show))
  }

  def pure[A](a: A): HttpIo[A] =
    Kleisli(_ ⇒ XorT(Future.successful(Xor.right(a))))

  def fromXor[A](fa: HttpError Xor A): HttpIo[A] =
    Kleisli(_ ⇒ XorT(Future.successful(fa)))

  def request(hostname: String, r: Request, port: Port = HttpPort)(implicit ec: ExecutionContext): HttpIo[Response] =
    for {
      address ← fromNetIo(NetIo.lookupAddress(hostname))
      con ← fromNetIo(NetIo.connect(address, port))
      _ ← fromNetIo(NetIo.write(con, Request.render(r).toArray))
      status ← readLine(con)
      headers ← readHeaders(con)
    } yield Response(status.toArray, headers, Array.empty)

  private def readHeaders(con: ConnectionId)(implicit ec: ExecutionContext): HttpIo[List[Header]] =
    readLine(con) >>= { line ⇒
      if (Bytes.isWhitespace(line)) {
        HttpIo.pure(List.empty)
      } else {
        readHeader(line) >>= { header ⇒
          readHeaders(con) map (header :: _)
        }
      }
    }

  private def readHeader(line: Vector[Byte]): HttpIo[Header] = HttpIo.fromXor {
    Header.read(line).toRightXor(HttpError.MalformedHeader(new String(line.toArray).trim))
  }

  private def readLine(con: ConnectionId)(implicit ec: ExecutionContext): HttpIo[Vector[Byte]] = HttpIo.fromNetIo {
    NetIo.readUntil(con, '\n'.toByte)
  }
}
