package httpc.http

import java.nio.ByteBuffer
import com.github.marklister.base64.Base64
import httpc._
import httpc.http.Arbitraries._
import io.circe._
import io.circe.generic.auto._
import io.circe.jawn._
import org.specs2.matcher.Matcher
import org.specs2.{ScalaCheck, Specification}


class HttpSpec extends Specification with ScalaCheck { def is = s2"""
  A requests yield appropriate response $checkCycle
  """

  def checkCycle = prop { (method: Method, message: Message) ⇒

    val url = urlFor(method).getOrElse(throw new RuntimeException("Generating malformed URLs"))
    val request = Request(method, url.path, message)
    val response = http.dispatch(url, request, Options.default)

    response must (endIn200Ok and haveSentCorrectHeaders(message.headers) and haveSentCorrectBody(message.body))

  }.set(minTestsOk = 5)

  val endIn200Ok = actionMatcher {
    case Right(resp) ⇒ resp.status.value == 200
    case _ ⇒ false
  } (err="Response is not 200")

  def haveSentCorrectBody(body: Array[Byte]) = requestMetadataMatcher { bin ⇒
    bin.decodedDataBytes.sameElements(body)
  } ("Request did not send correct body")

  def haveSentCorrectHeaders(headers: List[Header]) = requestMetadataMatcher { bin ⇒
    headers forall { header ⇒
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
    def decodedDataBytes: Array[Byte] =
      if (data.isEmpty)
        Array.empty
      else if (data.startsWith("data:application/octet-stream;base64,")) {
        val s = data.split(',')(1)
        Base64.Decoder(s).toByteArray
      } else
        data.getBytes
  }

  object RequestMetadata {
    def parse(bytes: Array[Byte]): Decoder.Result[RequestMetadata] =
      parseByteBuffer(ByteBuffer.wrap(bytes)).getOrElse(Json.Null).as[RequestMetadata]
  }
}
