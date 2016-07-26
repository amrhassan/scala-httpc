package httpc.tcp

import java.io.IOException
import cats.Show
import cats.data.Xor


sealed trait SocketError

object SocketError {

  case class UnknownHost(hostname: String) extends SocketError
  case class NotAllowed(message: String) extends SocketError
  case class IO(message: String) extends SocketError

  val socketErrorShow: Show[SocketError] = Show.show {
    case NotAllowed(m) ⇒ "Socket operations not allowed: " + m
    case IO(m) ⇒ "IO Error: " + m
    case UnknownHost(hostname) ⇒ s"Hostname $hostname is unknown"
  }

  def catchIoException[A](a: ⇒ A): SocketError Xor A =
    Xor.catchOnly[IOException](a) leftMap (t ⇒ IO(t.getMessage))
}
