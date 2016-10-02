package httpc.support.circe

import java.nio.ByteBuffer
import httpc.http.{Header, Response, ToRequest}
import io.circe.{Json, ParsingFailure}
import io.circe.jawn.parseByteBuffer

trait CirceSupport {

  implicit class ResponseWithJson(response: Response) {
    def json: Either[ParsingFailure, Json] =
      parseByteBuffer(ByteBuffer.wrap(response.body)).toEither
  }

  implicit val toRequestJson: ToRequest[Json] =
    ToRequest(Header.contentType("application/json"))(_.noSpaces.getBytes)
}
