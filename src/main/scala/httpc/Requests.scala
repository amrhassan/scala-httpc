package httpc

import java.net.URL
import scala.concurrent.ExecutionContext
import cats.data.Xor
import httpc.HttpError.MalformedUrl
import HttpIo._
import httpc.net.NetIo
import cats.implicits._

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
      request = Request(method, Path(parsedUrl.getPath), Message(extraHeaders, implicitly[RequestData[A]].bytes(data)))
    } yield (address, request, Http.Port)
}

trait RequestData[A] {
  def bytes(a: A): Array[Byte]
}

object RequestData {

  def apply[A](f: (A ⇒ Array[Byte])): RequestData[A] = new RequestData[A] {
    def bytes(a: A): Array[Byte] = f(a)
  }

  implicit val requestDataString: RequestData[String] =
    RequestData(_.getBytes)
}
