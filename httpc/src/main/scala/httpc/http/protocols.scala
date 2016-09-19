package httpc.http

import httpc.net.{Address, ConnectionId, NetIo, Port}
import httpc.net

/** Operations not defined by this protocol can be done using the net package directly */
trait NetProtocol {
  def defaultPort: Port
  def connect(address: Address, port: Port): NetIo[ConnectionId]
}

object NetProtocol {

  /** Non-encrypted HTTP/1.1 */
  val http = new NetProtocol {
    def defaultPort = httpc.net.Port.fromInt(80).getOrElse(throw new RuntimeException("Invalid HTTP port"))
    def connect(address: Address, port: Port): NetIo[ConnectionId] = net.connect(address, port)
  }

  /** Secure HTTP/1.1 */
  val https = new NetProtocol {
    def connect(address: Address, port: Port): NetIo[ConnectionId] = net.connectSsl(address, port)
    def defaultPort: Port = net.Port.fromInt(443).getOrElse(throw new RuntimeException("Invalid HTTPS port"))
  }
}
