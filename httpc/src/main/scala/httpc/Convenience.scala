package httpc

import scala.concurrent.{ExecutionContext, Future}
import httpc.http._
import HttpAction._
import cats.data.XorT
import cats.implicits._
import httpc.net.{NetInterpreters, NetIo}


/** Convenience construction and dispatching of requests */
private [httpc] trait Convenience {
  
  type Response = http.Response

  def request[A: RequestData](method: Method, url: String, data: A = "")(implicit ec: ExecutionContext): HttpAction[Response] =
    for {
      goodUrl ← Url.parse(url)
      protocol ← Requests.protocol(goodUrl)
      request = Requests.request(method, goodUrl, data)
      address ← fromNetIo(protocol.lookupAddress(goodUrl.host))
      response ← dispatch(address, request, protocol.port)
    } yield response

  def get[A: RequestData](url: String, data: A = "")(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Methods.Get, url, data)

  def put[A: RequestData](url: String, data: A)(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Methods.Put, url, data)

  def head[A: RequestData](url: String, data: A)(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Methods.Head, url, data)

  def post[A: RequestData](url: String, data: A)(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Methods.Post, url, data)

  def delete[A: RequestData](url: String, data: A = "")(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Methods.Delete, url, data)

  def trace[A: RequestData](url: String, data: A = "")(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Methods.Trace, url, data)

  def options[A: RequestData](url: String, data: A = "")(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Methods.Options, url, data)

  def run[A](command: HttpAction[A])(implicit ec: ExecutionContext): XorT[Future, HttpError, A] =
    HttpAction.run(command, netInterpreter)
  
  private def netInterpreter(implicit ec: ExecutionContext): NetIo.Interpreter = NetInterpreters.socketsInterpreter
}
