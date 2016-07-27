import scala.concurrent.Future
import cats.data.{Kleisli, XorT}
import httpc.net._


package object httpc {

  val HttpPort = Port.fromInt(80).getOrElse(throw new RuntimeException("Invalid HTTP port"))

  type HttpIo[A] = Kleisli[XorT[Future, HttpError, ?], NetIo.Interpreter, A]
}
