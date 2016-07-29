package httpc.http

import java.net.URL
import scala.concurrent.ExecutionContext
import cats.data.Xor
import cats.implicits._
import httpc.http.HttpError.MalformedUrl
import httpc.http.HttpIo._
import httpc.net
import httpc.net.NetIo

/** Request building and execution */
object Requests {

  /** Builds and executes an HTTP request */
  def request[A: RequestData](method: Method, url: String,
    data: A)(implicit ec: ExecutionContext): HttpIo[(net.Address, Request, net.Port)] =
    for {
      parsedUrl ← xor(Xor.catchNonFatal(new URL(url)).leftMap(_ ⇒ MalformedUrl(url)))
      hostname = parsedUrl.getHost
      extraHeaders = List(Header(HeaderNames.Host, hostname))
      address ← fromNetIo(NetIo.lookupAddress(hostname))
      request = Request(method, Path(parsedUrl.getPath), Message(extraHeaders, implicitly[RequestData[A]].body(data)))
    } yield (address, request, Http.Port)
}

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
}
