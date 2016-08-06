package httpc.http

import httpc.http.HttpError.UnsupportedProtocol
import httpc.http.HttpAction._


/** Request construction */
trait Requests {

  /** Builds an HTTP request */
  def request[A: RequestData](method: Method, url: Url, data: A): Request = {
    val requestData = implicitly[RequestData[A]]
    val dataBytes = requestData.body(data)

    val requiredHeaders = List(Headers.host(url.host), Headers.contentLength(dataBytes.length))
    val customHeaders = requestData.fallbackHeaders

    Request(method, url.path, Message(headers(requiredHeaders, customHeaders), dataBytes))
  }

  private def headers(requiredHeaders: List[Header], customHeaders: List[Header]): List[Header] = {
    // Required headers overridden by custom headers
    val z = requiredHeaders.map(h ⇒ (h.name, h)).toMap
    customHeaders.foldRight(z)((header, headers) ⇒ headers.updated(header.name, header)).values.toList
  }

  /** Figures out the networking protocol from the URL */
  def netProtocol(url: Url): HttpAction[NetProtocol] = url.protocol.toLowerCase match {
    case "http" ⇒ pure(NetProtocol.http)
    case "https" ⇒ pure(NetProtocol.https)
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
