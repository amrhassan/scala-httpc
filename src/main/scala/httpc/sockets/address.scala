package httpc.sockets

import java.net.InetAddress

/** An Internet address */
case class Address private[sockets](inet: InetAddress)

case class Port private(number: Int)

object Port {
  def fromInt(value: Int): Option[Port] =
    if (value >= 0 && value <= 65535) Some(Port(value)) else None
}
