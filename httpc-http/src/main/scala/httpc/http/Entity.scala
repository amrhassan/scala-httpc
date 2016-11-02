package httpc.http

import httpc.net.Bytes
import scodec.bits.ByteVector

/** Content that can be converted to a [[Request]] */
trait Entity[A] {

  /** Body of request */
  def body(a: A): ByteVector

  /** Fallback headers in case they are not defined in the request */
  def fallbackHeaders: Headers
}

object Entity {

  def apply[A: Entity]: Entity[A] =
    implicitly[Entity[A]]

  def apply[A](headers: Header*)(f: (A ⇒ ByteVector)): Entity[A] = new Entity[A] {
    def body(a: A): ByteVector = f(a)
    def fallbackHeaders: Headers = Headers(headers)
  }

  implicit val toRequestUtf8: Entity[String] =
    Entity(Header.contentType("text/plain; charset=utf-8"))(Bytes.fromUtf8)

  implicit val toRequestByteVector: Entity[ByteVector] =
    Entity()(identity)
}
