package httpc.sockets

import cats.Show

sealed trait SocketError
case object NotAllowed extends SocketError

object SocketError {

  val socketErrorShow: Show[SocketError] = Show.show {
    case NotAllowed ⇒ "Socket operations not allowed"
  }

  def notAllowed: SocketError = NotAllowed
}
