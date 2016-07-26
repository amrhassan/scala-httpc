package httpc.tcp

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import cats.data.XorT
import httpc.tcp.sockets.{Addresses, Socket, SocketError}
import cats.implicits._
import httpc.tcp.TcpError.ConnectionNotFound


/** Interpreters for TCP communication */
object Interpreters {

  /** Interpreter for TCP language using OS sockets */
  def socketsInterpreter(implicit ec: ExecutionContext): TcpNet.Interpreter =
    new TcpNet.Interpreter {

    val socketMap = mutable.Map.empty[ConnectionId, Socket]

    def apply[A](fa: TcpNetOp[A]): XorT[Future, TcpError, A] = XorT.fromXor[Future](fa match {
      case AddrLookup(hostname) ⇒
        Addresses.lookup(hostname) leftMap errorTranslate
      case Connect(address, port) ⇒
        for {
          socket <- Socket.connect(address, port) leftMap errorTranslate
          id = socketId(socket)
        } yield { socketMap(id) = socket; id }
      case Disconnect(id) ⇒
        for {
          socket ← socketMap.get(id).toRightXor(ConnectionNotFound(id))
          _ ← socket.disconnect() leftMap errorTranslate
        } yield { socketMap.remove(id); () }
      case Read(id, length) ⇒
        for {
          socket ← socketMap.get(id).toRightXor(ConnectionNotFound(id))
          data ← socket.read(length) leftMap errorTranslate
        } yield data
      case Write(id, data) ⇒
        for {
          socket ← socketMap.get(id).toRightXor(ConnectionNotFound(id))
          _ ← socket.write(data) leftMap errorTranslate
        } yield ()
    })

    def socketId(socket: Socket): ConnectionId =
      ConnectionId(socket.socket.getPort | (socket.socket.getLocalPort << 16))

    def errorTranslate(socketError: SocketError): TcpError = socketError match {
      case SocketError.UnknownHost(hostname) ⇒ TcpError.HostNotFound(hostname)
      case e ⇒ TcpError.UnexpectedError(e.show)
    }
  }
}
