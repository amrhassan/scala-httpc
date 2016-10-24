package httpc.convenient

import cats.data.Kleisli
import cats.implicits._
import httpc.http._
import httpc.net.NetInterpreters
import httpc.{convenient, http, net}
import scodec.bits.ByteVector

/** Convenience construction and dispatching of requests */
private [httpc] trait Convenient {

  type Httpc[A] = Kleisli[Either[HttpcError, ?], net.Interpreter, A]

  def request[A: ToRequest](method: Method, url: String, data: A = "", headers: List[Header] = List.empty): Httpc[Response] =
    for {
      goodUrl ← fromEither(Url.parse(url).toRight[HttpcError](MalformedUrl(url)))
      request = http.request(method, goodUrl, data, headers)
      response ← fromHttpAction(dispatch(goodUrl, request))
    } yield response

  def get[A: ToRequest](url: String, data: A = ByteVector.empty, headers: List[Header] = List.empty): Httpc[Response] =
    request(Method.Get, url, data, headers)

  def put[A: ToRequest](url: String, data: A = ByteVector.empty, headers: List[Header] = List.empty): Httpc[Response] =
    request(Method.Put, url, data, headers)

  def patch[A: ToRequest](url: String, data: A = ByteVector.empty, headers: List[Header] = List.empty): Httpc[Response] =
    request(Method.Patch, url, data, headers)

  def head[A: ToRequest](url: String, data: A = ByteVector.empty, headers: List[Header] = List.empty): Httpc[Response] =
    request(Method.Head, url, data, headers)

  def post[A: ToRequest](url: String, data: A = ByteVector.empty, headers: List[Header] = List.empty): Httpc[Response] =
    request(Method.Post, url, data, headers)

  def delete[A: ToRequest](url: String, data: A = ByteVector.empty, headers: List[Header] = List.empty): Httpc[Response] =
    request(Method.Delete, url, data, headers)

  def trace[A: ToRequest](url: String, data: A = ByteVector.empty, headers: List[Header] = List.empty): Httpc[Response] =
    request(Method.Trace, url, data, headers)

  def options[A: ToRequest](url: String, data: A = ByteVector.empty, headers: List[Header] = List.empty): Httpc[Response] =
    request(Method.Options, url, data, headers)

  def run[A](action: Httpc[A]): Either[HttpcError, A] =
    action.run(NetInterpreters.socketsInterpreter)

  private [httpc] def fromHttpAction[A](a: HttpAction[A]): Httpc[A] =
    Kleisli(interp => a.run(interp).left.map(convenient.HttpError.apply))

  private [httpc] def fromEither[A](either: Either[HttpcError, A]): Httpc[A] =
    Kleisli(_ => either)
}

