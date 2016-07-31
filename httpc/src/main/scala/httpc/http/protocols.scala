package httpc.http

import httpc.net

trait Protocol {
  def port: net.Port
  def lookupAddress(hostname: String): net.NetIo[net.Address]
}

object Protocol {

  /** Non-encrypted HTTP/1.1 */
  val http = new Protocol {
    def port = HttpPort
    def lookupAddress(hostname: String): net.NetIo[net.Address] = net.lookupAddress(hostname)
  }
}
