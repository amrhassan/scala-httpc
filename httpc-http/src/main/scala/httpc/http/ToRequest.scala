package httpc.http

/** Content that can be converted to a [[Request]] */
trait ToRequest[A] {

  /** Body of request */
  def body(a: A): Array[Byte]

  /** Fallback headers in case they are not defined in the request */
  def fallbackHeaders: List[Header]
}

object ToRequest {

  def apply[A: ToRequest]: ToRequest[A] =
    implicitly[ToRequest[A]]

  def apply[A](headers: Header*)(f: (A ⇒ Array[Byte])): ToRequest[A] = new ToRequest[A] {
    def body(a: A): Array[Byte] = f(a)
    def fallbackHeaders: List[Header] = headers.toList
  }

  implicit val requestDataUtf8: ToRequest[String] =
    ToRequest(Header.contentType("text/plain; charset=utf-8"))(_.getBytes("UTF-8"))

  implicit val requestDataBytes: ToRequest[Array[Byte]] =
    ToRequest()(identity)
}
