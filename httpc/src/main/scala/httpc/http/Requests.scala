package httpc.http

import httpc.http.HttpError.UnsupportedProtocol
import httpc.http.HttpAction._


/** Request construction */
trait Requests {

  /** Builds an HTTP request */
  def request[A: RequestData](method: Method, url: Url, data: A): Request = {
    val dataTc = implicitly[RequestData[A]]
    val headers = List(Header(HeaderNames.Host, url.host)) ++ dataTc.fallbackHeaders
    Request(method, Path(url.path), Message(headers, dataTc.body(data)))
  }

  /** Figures out the HTTP protocol from the URL */
  def protocol(url: Url): HttpAction[Protocol] = url.protocol.toLowerCase match {
    case "http" ⇒ pure(Protocol.http)
    case "https" ⇒ pure(Protocol.https)
    case _ ⇒ error(UnsupportedProtocol(url.protocol))
  }
}

object Requests extends Requests

/** Request content */
trait RequestData[A] {

  /** Body of request */
  def body(a: A): Array[Byte]

  /** Fallback headers in case they are not defined in the request */
  def fallbackHeaders: List[Header]
}

object RequestData {

  def apply[A](headers: Header*)(f: (A ⇒ Array[Byte])): RequestData[A] = new RequestData[A] {
    def body(a: A): Array[Byte] = f(a)
    def fallbackHeaders: List[Header] = headers.toList
  }

  implicit val requestDataString: RequestData[String] =
    RequestData(Headers.contentType("text/plain; charset=utf-8"))(_.getBytes("UTF-8"))

  implicit val requestDataBytes: RequestData[Array[Byte]] =
    RequestData()(identity)
}
