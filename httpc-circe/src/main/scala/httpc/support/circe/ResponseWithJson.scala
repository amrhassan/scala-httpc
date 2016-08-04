package httpc.support.circe

import java.nio.ByteBuffer
import cats.data.Xor
import httpc.http.Response
import io.circe.{Json, ParsingFailure}
import io.circe.jawn._

case class ResponseWithJson(response: Response) {
  def json: ParsingFailure Xor Json =
    parseByteBuffer(ByteBuffer.wrap(response.body))
}
