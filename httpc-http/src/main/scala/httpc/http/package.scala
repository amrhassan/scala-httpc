package httpc

import cats.data.Kleisli

/** HTTP protocol */
package object http extends Http {
  type HttpAction[A] = Kleisli[Either[HttpError, ?], net.Interpreter, A]
}
