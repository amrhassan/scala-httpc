package httpc.http

import scala.concurrent.{ExecutionContext, Future}
import cats.data.{Kleisli, Xor, XorT}
import cats.implicits._
import httpc.net.NetIo


object HttpAction {

  /** Evaluates the given [[HttpAction]] program realizing all its effects */
  def run[A](command: HttpAction[A], netInt: NetIo.Interpreter): XorT[Future, HttpError, A] =
    command.run(netInt)

  def fromNetIo[A](command: NetIo[A])(implicit ec: ExecutionContext): HttpAction[A] = Kleisli {
    (netInt: NetIo.Interpreter) ⇒
      NetIo.run(command, netInt) leftMap (e ⇒ HttpError.NetworkError(e.show))
  }

  def pure[A](a: A): HttpAction[A] =
    Kleisli(_ ⇒ XorT(Future.successful(Xor.right(a))))

  def error[A](e: HttpError): HttpAction[A] =
    Kleisli(_ ⇒ XorT(Future.successful(Xor.left(e))))

  def xor[A](fa: HttpError Xor A): HttpAction[A] =
    Kleisli(_ ⇒ XorT(Future.successful(fa)))
}
