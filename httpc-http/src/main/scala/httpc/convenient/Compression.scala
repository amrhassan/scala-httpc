package httpc.convenient

import enumeratum._

/** Supported HTTP body compression */
sealed abstract class Compression(val name: String) extends EnumEntry
case object GzipCompression extends Compression("gzip")
case object CompressCompression extends Compression("compress")
case object DeflateCompression extends Compression("deflate")
case object IdentityCompression extends Compression("identity")
case object BrCompression extends Compression("br")

object Compression extends Enum[Compression] {

  def decode(compression: Compression)(content: Array[Byte]): Either[HttpError, Array[Byte]] = ???

  val values: Seq[Compression] = findValues

  /** Looks up by HTTP name */
  def forName(name: String): Either[HttpcError, Compression] =
    (values find (_.name == name)).toRight(UnsupportedCompression(name))
}
