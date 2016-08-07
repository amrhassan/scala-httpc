package httpc.http

import org.scalacheck.Arbitrary
import org.scalacheck.Gen._
import Arbitrary._
import cats.implicits._

object Arbitraries {

  // httpbin.org-specific arbitraries

  val scheme = "https"
  val hostname = "httpbin.org"
  def path(method: Method) = Path(method match {
    case Method.Get ⇒ s"/get"
    case Method.Put ⇒ s"/put"
    case Method.Post ⇒ s"/post"
    case Method.Delete ⇒ s"/delete"
    case _ ⇒ ???
  })
  def urlFor(method: Method) =
    Url.parse(s"$scheme://$hostname${path(method).value}")

  implicit val arbMethod: Arbitrary[Method] = Arbitrary {
    oneOf(Method.Put, Method.Post, Method.Delete) // Get responses fro Http Bin can sometimes not contain Content-Length request header
  }

  implicit val arbRequest: Arbitrary[Request] = Arbitrary {
    for {
      method ← arbitrary[Method]
      message ← arbitrary[Message]
    } yield Request(method, path(method), message)
  }

  implicit val arbPath: Arbitrary[Path] = Arbitrary {
    arbitrary[List[String]] map (segments ⇒ Path("/" + segments.mkString("/")))
  }

  implicit val arbMessage: Arbitrary[Message] = Arbitrary {
    for {
      body ← arbitrary[Array[Byte]]
      headers =
        (if (body.nonEmpty) List(Headers.contentLength(body.length)) else List.empty) |+|
        List(Headers.host(hostname))
    } yield Message(headers, body)
  }
}
