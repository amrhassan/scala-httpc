package httpc.tcp

import cats.Show
import cats.implicits._

sealed trait TcpError

object TcpError {

  case class ConnectionNotFound(connectionId: ConnectionId) extends TcpError
  case class HostNotFound(hostname: String) extends TcpError
  case class UnexpectedError(message: String) extends TcpError

  implicit val tcpErrorShow: Show[TcpError] = Show.show(show)

  def show(e: TcpError): String = "TCP error: " |+| (e match {
    case ConnectionNotFound(id) ⇒ s"$id is not an open connection"
    case HostNotFound(hostname) ⇒ s"$hostname not found"
    case UnexpectedError(message) ⇒ message
  })
}
