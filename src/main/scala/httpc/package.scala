import scala.concurrent.Future
import cats.data.{Kleisli, XorT}
import httpc.net._


package object httpc {
  type HttpIo[A] = Kleisli[XorT[Future, HttpError, ?], NetIo.Interpreter, A]
}
