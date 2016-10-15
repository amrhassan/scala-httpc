package httpc.net

import scodec.bits.ByteVector

object Bytes {

  def fromUtf8(text: String): ByteVector =
    ByteVector(text.getBytes("UTF-8"))

  def isWhitespace(bytes: ByteVector): Boolean =
    toString(bytes).trim.isEmpty

  def toString(bytes: ByteVector): String =
    new String(bytes.toArray)

  def split(bytes: ByteVector, sep: Byte): Vector[ByteVector] =
    if (bytes.isEmpty)
      Vector.empty
    else {
      bytes.takeWhile(_ != sep) +: split(bytes.dropWhile(_ != sep).drop(1), sep)
    }
}
