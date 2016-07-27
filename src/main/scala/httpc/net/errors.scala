package httpc.net

import cats.Show
import cats.implicits._

sealed trait NetError

object NetError {

  case class ConnectionNotFound(connectionId: ConnectionId) extends NetError
  case class HostNotFound(hostname: String) extends NetError
  case class UnexpectedError(message: String) extends NetError

  implicit val tcpErrorShow: Show[NetError] = Show.show(show)

  def show(e: NetError): String = "Network error: " |+| (e match {
    case ConnectionNotFound(id) ⇒ s"$id is not an open connection"
    case HostNotFound(hostname) ⇒ s"$hostname not found"
    case UnexpectedError(message) ⇒ message
  })
}
