package httpc.support

import scala.language.implicitConversions
import httpc.http.Response

package object circe {
  implicit def responsWithJson(response: Response): ResponseWithJson =
    ResponseWithJson(response)
  implicit val jsonRequestData = JsonInstances.jsonRequestData
}
