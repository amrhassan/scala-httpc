package httpc.support.circe

import httpc.http.{Entity, Header, Response}
import httpc.net.Bytes
import io.circe.{Json, ParsingFailure}
import io.circe.jawn.parseByteBuffer

trait CirceSupport {

  implicit class ResponseWithJson(response: Response) {
    def json: Either[ParsingFailure, Json] =
      parseByteBuffer(response.body.toByteBuffer)
  }

  implicit val toRequestJson: Entity[Json] =
    Entity(Header.contentType("application/json"))(json => Bytes.fromUtf8(json.noSpaces))
}
