package httpc.http

import httpc.net.Bytes
import scodec.bits.ByteVector

/** Content that can be converted to a [[Request]] */
trait ToRequest[A] {

  /** Body of request */
  def body(a: A): ByteVector

  /** Fallback headers in case they are not defined in the request */
  def fallbackHeaders: List[Header]
}

object ToRequest {

  def apply[A: ToRequest]: ToRequest[A] =
    implicitly[ToRequest[A]]

  def apply[A](headers: Header*)(f: (A ⇒ ByteVector)): ToRequest[A] = new ToRequest[A] {
    def body(a: A): ByteVector = f(a)
    def fallbackHeaders: List[Header] = headers.toList
  }

  implicit val toRequestUtf8: ToRequest[String] =
    ToRequest(Header.contentType("text/plain; charset=utf-8"))(Bytes.fromUtf8)

  implicit val toRequestByteVector: ToRequest[ByteVector] =
    ToRequest()(identity)
}
