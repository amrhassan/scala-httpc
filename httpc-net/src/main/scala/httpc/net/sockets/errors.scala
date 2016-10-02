package httpc.net.sockets

import java.io.IOException
import cats.Show
import cats.implicits._


sealed trait SocketError

object SocketError {

  case class UnknownHost(hostname: String) extends SocketError
  case class NotAllowed(message: String) extends SocketError
  case class IO(message: String) extends SocketError

  implicit val socketErrorShow: Show[SocketError] = Show.show {
    case NotAllowed(m) ⇒ "Socket operations not allowed: " + m
    case IO(m) ⇒ "IO Error: " + m
    case UnknownHost(hostname) ⇒ s"Hostname $hostname is unknown"
  }

  def catchIoException[A](a: ⇒ A): Either[SocketError, A] =
    Either.catchOnly[IOException](a) leftMap (t ⇒ IO(t.getMessage))
}
