package httpc

import scala.concurrent.Future
import cats.data.{Kleisli, XorT}

/** HTTP protocol */
package object http extends Http {
  type HttpAction[A] = Kleisli[XorT[Future, HttpError, ?], net.Interpreter, A]
}
