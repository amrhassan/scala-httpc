package httpc.net

import scala.collection.mutable
import cats.free.Free
import cats.implicits._
import NetError.ConnectionNotFound
import httpc.net.sockets.{Addresses, Socket, SocketError}


/** Networking operations */
private [net] trait NetOp[A]
private [net] case class AddrLookup(hostname: String) extends NetOp[Address]
private [net] case class Connect(address: Address, port: Port) extends NetOp[ConnectionId]
private [net] case class ConnectSsl(address: Address, port: Port) extends NetOp[ConnectionId]
private [net] case class Read(conId: ConnectionId, length: Int) extends NetOp[Vector[Byte]]
private [net] case class Write(conId: ConnectionId, data: Array[Byte]) extends NetOp[Unit]
private [net] case class Disconnect(conId: ConnectionId) extends NetOp[Unit]


object NetAction {

  def pure[A](a: A): NetAction[A] =
    Free.pure(a)

  /** Executes a TCP program based on the interpreter */
  def run[A](prog: NetAction[A], interpreter: Interpreter): Either[NetError, A] =
    prog.foldMap(interpreter)
}

/** Interpreters for network communication */
object NetInterpreters {

  /** Interpreter for TCP language using OS sockets */
  val socketsInterpreter = new Interpreter {

    // Warning: Scary imperative land ahead

    val conns = mutable.Map.empty[ConnectionId, Socket]

    def addConn(connectionId: ConnectionId, socket: Socket): ConnectionId = conns.synchronized {
      conns(connectionId) = socket
      connectionId
    }

    def dropConn(conId: ConnectionId): Unit = conns.synchronized {
      conns.remove(conId)
    }

    def apply[A](fa: NetOp[A]): Either[NetError, A] = fa match {
      case AddrLookup(hostname) ⇒
        Addresses.lookup(hostname) leftMap errorTranslate
      case Connect(address, port) ⇒
        for {
          socket ← Socket.connect(address, port) leftMap errorTranslate
          id = connId(socket)
        } yield { addConn(id, socket); id }
      case ConnectSsl(address, port) ⇒
        for {
          socket ← Socket.connectSsl(address, port) leftMap errorTranslate
          id = connId(socket)
        } yield { addConn(id, socket) }
      case Disconnect(id) ⇒
        for {
          socket ← conns.get(id).toRight[NetError](ConnectionNotFound(id))
          _ ← socket.disconnect() leftMap errorTranslate
        } yield { dropConn(id) }
      case Read(id, length) ⇒
        for {
          socket ← conns.get(id).toRight[NetError](ConnectionNotFound(id))
          data ← socket.read(length) leftMap errorTranslate
        } yield data.toVector
      case Write(id, data) ⇒
        for {
          socket ← conns.get(id).toRight[NetError](ConnectionNotFound(id))
          _ ← socket.write(data) leftMap errorTranslate
        } yield ()
    }

    def connId(socket: Socket): ConnectionId =
      ConnectionId(socket.socket.getPort | (socket.socket.getLocalPort << 16))

    def errorTranslate(socketError: SocketError): NetError = socketError match {
      case SocketError.UnknownHost(hostname) ⇒ NetError.HostNotFound(hostname)
      case e ⇒ NetError.UnexpectedError(e.show)
    }
  }
}
