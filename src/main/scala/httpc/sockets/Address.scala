package httpc.sockets

import java.net.{InetAddress, UnknownHostException}
import cats.implicits._
import cats.data.Xor

/** An Internet address */
case class Address(inet: InetAddress)

object Address {

  /** Looks up an Address by a hostname */
  def lookup(hostname: String): SocketError Xor Option[Address] =
    Xor.catchNonFatal(Address(InetAddress.getByName(hostname)).some) recover {
      case _: UnknownHostException ⇒ None
    } leftMap(_ ⇒ SocketError.notAllowed)
}
