package httpc.net

import java.util.concurrent.locks.ReentrantReadWriteLock
import scala.collection.mutable
import httpc.net.NetError.ConnectionNotFound
import httpc.net.sockets.{Addresses, Socket, SocketError}
import Locks._
import cats.implicits._

/** Interpreter for TCP language using OS sockets */
private [net] class SocketsInterpreter(socketStore: SocketStore) extends Interpreter {

  def apply[A](fa: NetOp[A]): Either[NetError, A] = fa match {
    case AddrLookup(hostname) ⇒
      Addresses.lookup(hostname) leftMap errorTranslate
    case Connect(address, port) ⇒
      for {
        socket ← Socket.connect(address, port) leftMap errorTranslate
      } yield socketStore.add(socket)
    case ConnectSsl(address, port) ⇒
      for {
        socket ← Socket.connectSsl(address, port) leftMap errorTranslate
      } yield socketStore.add(socket)
    case Disconnect(id) ⇒
      for {
        socket ← socketStore.remove(id).toRight[NetError](ConnectionNotFound(id))
        _ ← socket.disconnect() leftMap errorTranslate
      } yield ()
    case Read(id, length) ⇒
      for {
        socket ← socketStore.get(id).toRight[NetError](ConnectionNotFound(id))
        data ← socket.read(length) leftMap errorTranslate
      } yield data
    case Write(id, data) ⇒
      for {
        socket ← socketStore.get(id).toRight[NetError](ConnectionNotFound(id))
        _ ← socket.write(data) leftMap errorTranslate
      } yield ()
  }

  def errorTranslate(socketError: SocketError): NetError = socketError match {
    case SocketError.UnknownHost(hostname) ⇒ NetError.HostNotFound(hostname)
    case e ⇒ NetError.UnexpectedError(e.show)
  }
}

/** Mutable storage of [[Socket]]s */
class SocketStore {
  private val lock = new ReentrantReadWriteLock
  private val data = mutable.Map.empty[ConnectionId, Socket]

  def add(socket: Socket): ConnectionId = syncWrite(lock) {
    val id = SocketStore.key(socket)
    data.put(id, socket)
    id
  }

  def get(id: ConnectionId): Option[Socket] = syncRead(lock) {
    data.get(id)
  }

  def remove(id: ConnectionId): Option[Socket] = syncWrite(lock) {
    data.remove(id)
  }
}

object SocketStore {
  def key(socket: Socket): ConnectionId =
    ConnectionId(socket.socket.getPort | (socket.socket.getLocalPort << 16))
}
