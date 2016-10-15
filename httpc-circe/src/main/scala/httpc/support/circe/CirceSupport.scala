package httpc.support.circe

import httpc.http.{Header, Response, ToRequest}
import httpc.net.Bytes
import io.circe.{Json, ParsingFailure}
import io.circe.jawn.parseByteBuffer

trait CirceSupport {

  implicit class ResponseWithJson(response: Response) {
    def json: Either[ParsingFailure, Json] =
      parseByteBuffer(response.body.toByteBuffer).toEither
  }

  implicit val toRequestJson: ToRequest[Json] =
    ToRequest(Header.contentType("application/json"))(json => Bytes.fromUtf8(json.noSpaces))
}
