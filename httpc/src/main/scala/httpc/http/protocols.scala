package httpc.http

import httpc.net
import httpc.net.{Address, ConnectionId, NetIo, Port}

trait Protocol {
  def port: net.Port
  def lookupAddress(hostname: String): net.NetIo[net.Address]
  def connect(address: net.Address, port: net.Port): net.NetIo[ConnectionId]
}

object Protocol {

  /** Non-encrypted HTTP/1.1 */
  val http = new Protocol {
    def port = HttpPort
    def lookupAddress(hostname: String): net.NetIo[net.Address] = net.lookupAddress(hostname)
    def connect(address: net.Address, port: net.Port): net.NetIo[ConnectionId] = net.connect(address, port)
  }

  /** Secure HTTP/1.1 */
  val https = new Protocol {
    def lookupAddress(hostname: String): NetIo[Address] = net.lookupAddress(hostname)
    def connect(address: Address, port: Port): NetIo[ConnectionId] = net.connectSsl(address, port)
    def port: Port = HttpsPort
  }
}
