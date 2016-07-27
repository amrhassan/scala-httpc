package httpc

object Bytes {

  def isWhitespace(bytes: Vector[Byte]): Boolean =
    new String(bytes.toArray).trim.isEmpty
}
