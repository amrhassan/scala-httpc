package httpc

import scala.concurrent.{ExecutionContext, Future}
import cats.data.{Kleisli, Xor, XorT}
import cats.implicits._
import httpc.net.NetIo


object HttpIo {

  /** Evaluates the given [[HttpIo]] program realizing all its effects */
  def run[A](command: HttpIo[A], netInt: NetIo.Interpreter, ec: ExecutionContext): XorT[Future, HttpError, A] =
    command.run(netInt)

  /** Lifts a [[NetIo]] into an [[HttpIo]] */
  def fromNetIo[A](command: NetIo[A])(implicit ec: ExecutionContext): HttpIo[A] = Kleisli {
    (netInt: NetIo.Interpreter) ⇒
      NetIo.run(command, netInt) leftMap (e ⇒ HttpError.NetworkError(e.show))
  }

  /** Lifts a pure value into [[HttpIo]] */
  def pure[A](a: A): HttpIo[A] =
    Kleisli(_ ⇒ XorT(Future.successful(Xor.right(a))))

  /** Lifts an error into [[HttpIo]] */
  def error[A](e: HttpError): HttpIo[A] =
    Kleisli(_ ⇒ XorT(Future.successful(Xor.left(e))))

  /** Lifts an [[Xor]] into an [[HttpIo]] */
  def xor[A](fa: HttpError Xor A): HttpIo[A] =
    Kleisli(_ ⇒ XorT(Future.successful(fa)))
}
