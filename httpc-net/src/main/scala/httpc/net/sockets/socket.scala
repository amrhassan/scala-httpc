package httpc.net.sockets

import java.io.{InputStream, OutputStream}
import java.net.{InetAddress, UnknownHostException, Socket => JSocket}
import javax.net.ssl.SSLSocketFactory
import httpc.net.{Address, Port}
import httpc.net.sockets.SocketError._
import cats.implicits._


/** A TCP socket */
case class Socket private[sockets](socket: JSocket, in: InputStream, out: OutputStream) {

   /** Disconnects the given socket and frees its allocated system resources */
  def disconnect(): Either[SocketError, Unit] =
    catchIoException(socket.close())

  /** Reads bytes of the specified length from the socket */
  def read(length: Int): Either[SocketError, Array[Byte]] =
    catchIoException {
      val buffer = Array.ofDim[Byte](length)
      val readCount = in.read(buffer)
      buffer.take(readCount)
    }

  /** Writes bytes to the specified socket */
  def write(bytes: Array[Byte]): Either[SocketError, Unit] =
    catchIoException {
      out.write(bytes)
      out.flush()
    }
}

object Socket {

  /** Connects to the specified address and port via TCP */
  def connect(address: Address, port: Port): Either[SocketError, Socket] =
    catchIoException {
      fromJavaSocket(new JSocket(address.inet, port.number))
    }

  /** Connects to the specified address and port via TCP over SSL */
  def connectSsl(address: Address, port: Port): Either[SocketError, Socket] =
    catchIoException {
      fromJavaSocket(SSLSocketFactory.getDefault.createSocket(address.inet, port.number))
    }

  private def fromJavaSocket(js: JSocket): Socket =
    Socket(js, js.getInputStream, js.getOutputStream)
}

object Addresses {

  /** Looks up an Address by a hostname */
  def lookup(hostname: String): Either[SocketError, Address] =
    Either.catchNonFatal(Address(InetAddress.getByName(hostname))) leftMap {
      case t: UnknownHostException ⇒ SocketError.UnknownHost(hostname)
      case t: SecurityException ⇒ SocketError.NotAllowed(t.getMessage)
    }
}
