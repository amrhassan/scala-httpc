package httpc.http

import httpc.http.HttpError.UnsupportedProtocol
import httpc.net.{Address, ConnectionId, NetAction, Port}
import httpc.net
import HttpAction._

/** Operations not defined by this protocol can be done using the net package directly */
trait NetProtocol {
  def defaultPort: Port
  def connect(address: Address, port: Port): NetAction[ConnectionId]
}

object NetProtocol {

  /** Non-encrypted HTTP/1.1 */
  val http = new NetProtocol {
    def defaultPort = httpc.net.Port.fromInt(80).getOrElse(throw new RuntimeException("Invalid HTTP port"))
    def connect(address: Address, port: Port): NetAction[ConnectionId] = net.connect(address, port)
  }

  /** Secure HTTP/1.1 */
  val https = new NetProtocol {
    def connect(address: Address, port: Port): NetAction[ConnectionId] = net.connectSsl(address, port)
    def defaultPort: Port = net.Port.fromInt(443).getOrElse(throw new RuntimeException("Invalid HTTPS port"))
  }

  /** Figures out the networking protocol from the URL */
  def fromUrl(url: Url): HttpAction[NetProtocol] = url.protocol.toLowerCase match {
    case "http" ⇒ pure(NetProtocol.http)
    case "https" ⇒ pure(NetProtocol.https)
    case _ ⇒ error(UnsupportedProtocol(url.protocol))
  }
}
