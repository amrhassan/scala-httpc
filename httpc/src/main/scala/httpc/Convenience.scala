package httpc

import scala.concurrent.{ExecutionContext, Future}
import httpc.http._
import HttpAction._
import cats.data.Xor
import cats.implicits._
import httpc.net.{NetInterpreters, NetIo}


/** Convenience construction and dispatching of requests */
private [httpc] trait Convenience {
  
  type Response = http.Response
  type HttpError = http.HttpError

  def request[A: RequestData](method: Method, url: String, data: A = "")(implicit ec: ExecutionContext): HttpAction[Response] =
    for {
      goodUrl ← Url.parse(url)
      protocol ← Requests.netProtocol(goodUrl)
      request = Requests.request(method, goodUrl, data)
      address ← fromNetIo(net.lookupAddress(goodUrl.host))
      connectionId ← fromNetIo(protocol.connect(address, goodUrl.port.getOrElse(protocol.defaultPort)))
      response ← dispatch(connectionId, request)
      _ ← fromNetIo(net.disconnect(connectionId))
    } yield response

  def get[A: RequestData](url: String, data: A = Array.empty[Byte])(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Methods.Get, url, data)

  def put[A: RequestData](url: String, data: A = Array.empty[Byte])(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Methods.Put, url, data)

  def head[A: RequestData](url: String, data: A = Array.empty[Byte])(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Methods.Head, url, data)

  def post[A: RequestData](url: String, data: A = Array.empty[Byte])(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Methods.Post, url, data)

  def delete[A: RequestData](url: String, data: A = Array.empty[Byte])(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Methods.Delete, url, data)

  def trace[A: RequestData](url: String, data: A = Array.empty[Byte])(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Methods.Trace, url, data)

  def options[A: RequestData](url: String, data: A = Array.empty[Byte])(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Methods.Options, url, data)

  def run[A](command: HttpAction[A])(implicit ec: ExecutionContext): Future[HttpError Xor A] =
    HttpAction.run(command, netInterpreter).value
  
  private def netInterpreter(implicit ec: ExecutionContext): NetIo.Interpreter = NetInterpreters.socketsInterpreter
}
