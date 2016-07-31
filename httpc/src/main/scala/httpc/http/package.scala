package httpc

import scala.concurrent.Future
import cats.data.{Kleisli, XorT}
import httpc.net.NetIo

/** HTTP protocol */
package object http extends Http {
  type HttpAction[A] = Kleisli[XorT[Future, HttpError, ?], NetIo.Interpreter, A]
}
