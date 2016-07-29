package httpc

import scala.concurrent.{ExecutionContext, Future}
import cats.data.XorT
import httpc.net.NetInterpreters
import cats.implicits._
import httpc.http._


/** Convenience construction and dispatching of requests */
private [httpc] trait Convenience {
  
  type HttpValue[A] = XorT[Future, HttpError, A]

  /** Constructs and dispatches a request */
  def doRequest[A: RequestData](
    method: Method,
    url: String,
    data: A = ""
    )(implicit ec: ExecutionContext): HttpValue[Response] = {

    val response = Requests.request(method, url, data) >>= { case (address, request, port) ⇒
      http.execute(address, request, port)
    }

    HttpIo.run(response, netInterpreter)
  }

  /** Constructs and dispatches a GET request */
  def get[A: RequestData](url: String, data: A = "")(implicit ec: ExecutionContext): HttpValue[Response] =
    doRequest(Methods.Get, url)

  /** Constructs and dispatches a PUT request */
  def put[A: RequestData](url: String, data: A)(implicit ec: ExecutionContext): HttpValue[Response] =
    doRequest(Methods.Put, url, data)

  /** Constructs and dispatches a HEAD request */
  def head[A: RequestData](url: String, data: A)(implicit ec: ExecutionContext): HttpValue[Response] =
    doRequest(Methods.Head, url)

  /** Constructs and dispatches a POST request */
  def post[A: RequestData](url: String, data: A)(implicit ec: ExecutionContext): HttpValue[Response] =
    doRequest(Methods.Post, url, data)

  /** Constructs and dispatches a DELETE request */
  def delete[A: RequestData](url: String, data: A = "")(implicit ec: ExecutionContext): HttpValue[Response] =
    doRequest(Methods.Delete, url)

  /** Constructs and dispatches a TRACE request */
  def trace[A: RequestData](url: String, data: A = "")(implicit ec: ExecutionContext): HttpValue[Response] =
    doRequest(Methods.Trace, url)

  /** Constructs and dispatches an OPTIONS request */
  def options[A: RequestData](url: String, data: A = "")(implicit ec: ExecutionContext): HttpValue[Response] =
    doRequest(Methods.Options, url)

  private def netInterpreter(implicit ec: ExecutionContext) = NetInterpreters.socketsInterpreter
}
