package httpc.sockets.unsafe

import java.io.{IOException, InputStream, OutputStream}
import cats.data.Xor
import java.net.{Socket ⇒ JSocket}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import SocketError.catchIoException

/** A TCP socket */
case class Socket private[unsafe](socket: JSocket, in: InputStream, out: OutputStream) {

   /** Disconnects the given socket and frees its allocated system resources */
  def disconnect(): SocketError Xor Unit =
    catchIoException(socket.close())

  /** Reads bytes of the specified length from the socket */
  def read(length: Int Refined NonNegative): SocketError Xor Array[Byte] =
    catchIoException {
      val buffer = Array.ofDim[Byte](length.get)
      val _ = in.read(buffer)
      buffer
    }

  /** Writes bytes to the specified socket */
  def write(bytes: Array[Byte]): SocketError Xor Unit =
    catchIoException {
      out.write(bytes)
      out.flush()
    }
}

object Socket {

  /** Connects to the specified address and port via TCP */
  def connect(address: Address, port: Port): SocketError Xor Socket =
    catchIoException {
      val js = new JSocket(address.inet, port.number)
      Socket(js, js.getInputStream, js.getOutputStream)
    }
}
