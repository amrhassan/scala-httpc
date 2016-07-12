package httpc.sockets.unsafe

import java.net.{InetAddress, UnknownHostException}
import cats.data.Xor

/** An Internet address */
case class Address private[unsafe](inet: InetAddress)

object Address {

  /** Looks up an Address by a hostname */
  def lookup(hostname: String): SocketError Xor Address =
    Xor.catchNonFatal(Address(InetAddress.getByName(hostname))) leftMap {
      case t: UnknownHostException ⇒ SocketError.UnknownHost(hostname)
      case t: SecurityException ⇒ SocketError.NotAllowed(t.getMessage)
    }
}

case class Port private(number: Int)

object Port {
  def fromInt(value: Int): Option[Port] =
    if (value >= 0 && value <= 65535) Some(Port(value)) else None
}
