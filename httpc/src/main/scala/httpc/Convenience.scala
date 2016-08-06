package httpc

import scala.concurrent.ExecutionContext
import cats.implicits._
import httpc.http._


/** Convenience construction and dispatching of requests */
private [httpc] trait Convenience {
  
  def request[A: RequestData](method: Method, url: String, data: A = "")(implicit ec: ExecutionContext): HttpAction[Response] =
    for {
      goodUrl ← Url.parse(url)
      request = Requests.request(method, goodUrl, data)
      response ← dispatch(goodUrl, request)
    } yield response

  def get[A: RequestData](url: String, data: A = Array.empty[Byte])(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Method.Get, url, data)

  def put[A: RequestData](url: String, data: A = Array.empty[Byte])(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Method.Put, url, data)

  def head[A: RequestData](url: String, data: A = Array.empty[Byte])(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Method.Head, url, data)

  def post[A: RequestData](url: String, data: A = Array.empty[Byte])(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Method.Post, url, data)

  def delete[A: RequestData](url: String, data: A = Array.empty[Byte])(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Method.Delete, url, data)

  def trace[A: RequestData](url: String, data: A = Array.empty[Byte])(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Method.Trace, url, data)

  def options[A: RequestData](url: String, data: A = Array.empty[Byte])(implicit ec: ExecutionContext): HttpAction[Response] =
    request(Method.Options, url, data)
}
