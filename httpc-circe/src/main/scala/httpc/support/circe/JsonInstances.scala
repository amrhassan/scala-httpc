package httpc.support.circe

import httpc.http.{Headers, RequestData}
import io.circe.Json

object JsonInstances {
  implicit val jsonRequestData: RequestData[Json] =
    RequestData(Headers.contentType("application/json"))(_.noSpaces.getBytes)
}
