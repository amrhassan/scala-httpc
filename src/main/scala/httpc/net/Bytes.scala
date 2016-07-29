package httpc.net

object Bytes {

  def isWhitespace(bytes: Vector[Byte]): Boolean =
    toString(bytes).trim.isEmpty

  def toString(bytes: Vector[Byte]): String =
    new String(bytes.toArray)

  def split(bytes: Vector[Byte], sep: Byte): Vector[Vector[Byte]] =
    if (bytes.isEmpty)
      Vector.empty
    else {
      bytes.takeWhile(_ != sep) +: split(bytes.dropWhile(_ != sep).drop(1), sep)
    }
}
