package httpc.support.circe

import httpc.http.{Entity, Header, Response}
import org.specs2.{ScalaCheck, Specification}
import httpc.http.DataArbitraries._
import httpc.net.Bytes
import io.circe.Json
import io.circe.testing.instances._
import io.circe.parser._

class CirceSupportSpec extends Specification with ScalaCheck { def is = s2"""

  The Circe support component adds JSON functionality using JSON wherever is convenient

  Must be able to deserialize a JSON response       $deserializeResponse
  Must be able to convert JSON type into an Entity  $serializeToEntity
  """

  def deserializeResponse = prop { (response: Response, json: Json) =>
    val jsonResponse = response.copy(body = Bytes.fromUtf8(json.noSpaces))

    jsonResponse.json must beRight(json)
  }

  def serializeToEntity = prop { (json: Json) =>
    val entity = Entity[Json]

    val headerCheck =
      entity.fallbackHeaders.toList must contain((h: Header) => h must be equalTo Header.contentTypeJson)

    val bodyGenerationCheck =
      parse(Bytes.toString(entity.body(json))) must beRight(json)

    headerCheck and bodyGenerationCheck
  }
}
