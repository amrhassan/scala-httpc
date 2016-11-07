package httpc.net

import java.net.InetAddress
import cats.Show

/** An Internet address */
case class Address private[net](inet: InetAddress)

case class Port private(number: Int)

object Port {
  def fromInt(value: Int): Option[Port] =
    if (value >= 0 && value <= 65535) Some(Port(value)) else None

  implicit val showPort: Show[Port] = Show.show(_.number.toString)
}

case class ConnectionId(id: Int)
