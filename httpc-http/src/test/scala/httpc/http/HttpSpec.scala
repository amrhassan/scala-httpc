package httpc.http

import java.nio.ByteBuffer
import java.util.Base64

import httpc._
import httpc.http.Arbitraries._
import httpc.net.Bytes
import io.circe._
import io.circe.generic.auto._
import io.circe.jawn._
import org.specs2.matcher.Matcher
import org.specs2.{ScalaCheck, Specification}
import scodec.bits.ByteVector

class HttpSpec extends Specification with ScalaCheck { def is = s2"""
  A requests yield appropriate response $checkCycle
  """

  def checkCycle = prop { (method: Method, message: Message) ⇒

    val url = urlFor(method).getOrElse(throw new RuntimeException("Generating malformed URLs"))
    val request = Request(method, url.resource, message)
    val response = http.dispatch(url, request)

    response must (endIn200Ok and haveSentCorrectHeaders(message.headers) and haveSentCorrectBody(message.body))

  }.set(minTestsOk = 5)

  val endIn200Ok = actionMatcher {
    case Right(resp) ⇒ resp.status.value == 200
    case _ ⇒ false
  } (err="Response is not 200")

  def haveSentCorrectBody(body: ByteVector) = requestMetadataMatcher { bin ⇒
    bin.decodedDataBytes === body
  } ("Request did not send correct body")

  def haveSentCorrectHeaders(headers: Headers) = requestMetadataMatcher { bin ⇒
    headers.toList forall { header ⇒
      bin.headers(header.name) == header.value
    }
  } ("Request did not send all the headers")

  def requestMetadataMatcher(p: RequestMetadata ⇒ Boolean)(err: ⇒ String) = actionMatcher {
    case Right(resp) ⇒ (RequestMetadata.parse(resp.body) map p).getOrElse(false)
    case _ ⇒ false
  } (err)

  def actionMatcher(p: Either[http.HttpError, http.Response] ⇒ Boolean)(err: ⇒ String): Matcher[HttpAction[http.Response]] = {
    action: HttpAction[http.Response] ⇒
      if (p(run(action)))
        (true, "")
      else
        (false, err)
  }

  def noop[A]: Matcher[A] = {a: A ⇒ (true, "")}

  /** httpbin.org response which contains request metadata */
  case class RequestMetadata(
    origin: String,
    url: String,
    headers: Map[String, String],
    data: String
  ) {
    def decodedDataBytes: ByteVector =
      if (data.isEmpty)
        ByteVector.empty
      else if (data.startsWith("data:application/octet-stream;base64,")) {
        val s = data.split(',')(1)
        ByteVector(Base64.getDecoder.decode(s))
      } else
        Bytes.fromUtf8(data)
  }

  object RequestMetadata {
    def parse(bytes: ByteVector): Decoder.Result[RequestMetadata] =
      parseByteBuffer(ByteBuffer.wrap(bytes.toArray)).getOrElse(Json.Null).as[RequestMetadata]
  }
}
