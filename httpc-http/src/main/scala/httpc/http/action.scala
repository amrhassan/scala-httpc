package httpc.http

import cats.data.Kleisli
import cats.implicits._
import httpc.net
import httpc.net.NetAction


object HttpAction {

  /** Evaluates the given [[HttpAction]] program realizing all its effects */
  def run[A](command: HttpAction[A], netInt: net.Interpreter): Either[HttpError, A] =
    command.run(netInt)

  def fromNetIo[A](command: NetAction[A]): HttpAction[A] = Kleisli {
    (netInt: net.Interpreter) ⇒
      NetAction.run(command, netInt) leftMap (e ⇒ HttpError.NetworkError(e.show))
  }

  def pure[A](a: A): HttpAction[A] =
    Kleisli(_ ⇒ Either.right(a))

  def error[A](e: HttpError): HttpAction[A] =
    Kleisli(_ ⇒ Either.left(e))

  def either[A](fa: Either[HttpError, A]): HttpAction[A] =
    Kleisli(_ ⇒ fa)
}
