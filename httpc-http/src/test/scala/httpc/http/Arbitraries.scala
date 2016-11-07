package httpc.http

import org.scalacheck.Arbitrary
import org.scalacheck.Gen._
import Arbitrary._
import cats.implicits._
import scodec.bits.ByteVector
import cats.implicits._

object Arbitraries {

  // httpbin.org-specific arbitraries

  val scheme = "https"
  val hostname = "httpbin.org"
  def resource(method: Method) = Resource(Option(method match {
    case Method.Get ⇒ s"/get"
    case Method.Put ⇒ s"/put"
    case Method.Post ⇒ s"/post"
    case Method.Delete ⇒ s"/delete"
    case Method.Patch ⇒ s"/patch"
    case _ ⇒ ???
  }))

  def urlFor(method: Method): Option[Url] =
    Url.parse(s"$scheme://$hostname${resource(method).show}")

  implicit val arbMethod: Arbitrary[Method] = Arbitrary {
    oneOf(Method.Put, Method.Post, Method.Delete, Method.Patch)
    // Get responses fro Http Bin can sometimes not contain Content-Length request header
  }

  implicit val arbRequest: Arbitrary[Request] = Arbitrary {
    for {
      method ← arbitrary[Method]
      message ← arbitrary[Message]
    } yield Request(method, resource(method), message)
  }

  implicit val arbResource: Arbitrary[Resource] = Arbitrary {
    arbitrary[List[String]] map (segments ⇒ Resource(("/" + segments.mkString("/")).some))
  }

  implicit val arbByteVector: Arbitrary[ByteVector] = Arbitrary {
    arbitrary[Array[Byte]] map ByteVector.apply
  }

  implicit val arbMessage: Arbitrary[Message] = Arbitrary {
    for {
      body ← arbitrary[ByteVector]
      headers =
        (if (body.nonEmpty) Headers(Header.contentLength(body.length)) else Headers.empty) |+|
        Headers(Header.host(hostname))
    } yield Message(headers, body)
  }
}
