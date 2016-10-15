package httpc.net

import cats.Monoid
import scodec.bits.ByteVector

object ScodecInstances {

  implicit val monoidByteVector: Monoid[ByteVector] =
    new Monoid[ByteVector] {
      def empty: ByteVector = ByteVector.empty
      def combine(x: ByteVector, y: ByteVector): ByteVector = x ++ y
    }
}
