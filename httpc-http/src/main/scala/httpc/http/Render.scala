package httpc.http

import _root_.cats.functor
import cats.functor.Contravariant
import httpc.net.Bytes
import scodec.bits.ByteVector
import simulacrum.typeclass

/** Rendering of data structures into bytes */
@typeclass trait Render[A] {
  def render(a: A): ByteVector
}

object Render {

  def apply[A](f: A => ByteVector): Render[A] = new Render[A] {
    def render(a: A): ByteVector = f(a)
  }

  implicit val contraFunctorRender: functor.Contravariant[Render] =
    new Contravariant[Render] {
      def contramap[A, B](fa: Render[A])(f: (B) => A): Render[B] = new Render[B] {
        def render(b: B): ByteVector = fa.render(f(b))
      }
    }

  val renderUtf8: Render[String] = Render(Bytes.fromUtf8(_))

  val newline = ByteVector('\n'.toByte)
  val space = ByteVector(' '.toByte)
}
