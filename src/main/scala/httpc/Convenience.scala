package httpc

import scala.concurrent.{ExecutionContext, Future}
import cats.data.Xor
import httpc.net.NetInterpreters
import cats.implicits._
import httpc.http._

/** Convenience construction and dispatching of requests */
private [httpc] trait Convenience {

  def doRequest[A: RequestData](
    method: Method,
    url: String,
    data: A = ""
    )(implicit ec: ExecutionContext): Future[HttpError Xor Response] = {

    val response = Requests.request(method, url, data) >>= { case (address, request, port) ⇒
      Http.execute(address, request, port)
    }

    HttpIo.run(response, netInterpreter).value
  }

  def get[A: RequestData](url: String, data: A = "")(implicit ec: ExecutionContext): Future[HttpError Xor Response] =
    doRequest(Methods.Get, url)

  def put[A: RequestData](url: String, data: A)(implicit ec: ExecutionContext): Future[HttpError Xor Response] =
    doRequest(Methods.Put, url, data)

  def netInterpreter(implicit ec: ExecutionContext) = NetInterpreters.socketsInterpreter
}
