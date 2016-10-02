package httpc.http

import simulacrum.typeclass

/** Rendering of data structures into bytes */
@typeclass trait Render[A] {
  def render(a: A): Vector[Byte]
}

object Render {
  def apply[A](f: A => Vector[Byte]): Render[A] = new Render[A] {
    def render(a: A): Vector[Byte] = f(a)
  }

  val newline = Vector('\n'.toByte)
  val space = Vector(' '.toByte)
}
