package httpc.http

import httpc.net
import httpc.net.{Address, ConnectionId, NetIo, Port}

/** Operations not defined by this protocol can be done using the net package directly */
trait NetProtocol {
  def defaultPort: net.Port
  def connect(address: net.Address, port: net.Port): net.NetIo[ConnectionId]
}

object NetProtocol {

  /** Non-encrypted HTTP/1.1 */
  val http = new NetProtocol {
    def defaultPort = net.Port.fromInt(80).getOrElse(throw new RuntimeException("Invalid HTTP port"))
    def connect(address: net.Address, port: net.Port): net.NetIo[ConnectionId] = net.connect(address, port)
  }

  /** Secure HTTP/1.1 */
  val https = new NetProtocol {
    def connect(address: Address, port: Port): NetIo[ConnectionId] = net.connectSsl(address, port)
    def defaultPort: Port = net.Port.fromInt(443).getOrElse(throw new RuntimeException("Invalid HTTPS port"))
  }
}
