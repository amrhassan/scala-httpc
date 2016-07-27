package httpc.net

import scala.concurrent.{ExecutionContext, Future}
import cats.data.XorT
import httpc.net.sockets.{Addresses, Socket, SocketError}
import cats.implicits._
import httpc.net.NetError.ConnectionNotFound


/** Interpreters for network communication */
object NetInterpreters {

  /** Interpreter for TCP language using OS sockets */
  def socketsInterpreter(implicit ec: ExecutionContext): NetIo.Interpreter = new NetIo.Interpreter {

    var cons = Map.empty[ConnectionId, Socket]

    def apply[A](fa: NetIoOp[A]): XorT[Future, NetError, A] = XorT.fromXor[Future](fa match {
      case AddrLookup(hostname) ⇒
        Addresses.lookup(hostname) leftMap errorTranslate
      case Connect(address, port) ⇒
        for {
          socket <- Socket.connect(address, port) leftMap errorTranslate
          id = conId(socket)
        } yield { cons = cons.updated(id, socket); id }
      case Disconnect(id) ⇒
        for {
          socket ← cons.get(id).toRightXor(ConnectionNotFound(id))
          _ ← socket.disconnect() leftMap errorTranslate
        } yield { cons = cons - id; () }
      case Read(id, length) ⇒
        for {
          socket ← cons.get(id).toRightXor(ConnectionNotFound(id))
          data ← socket.read(length) leftMap errorTranslate
        } yield data
      case Write(id, data) ⇒
        for {
          socket ← cons.get(id).toRightXor(ConnectionNotFound(id))
          _ ← socket.write(data) leftMap errorTranslate
        } yield ()
    })

    def conId(socket: Socket): ConnectionId =
      ConnectionId(socket.socket.getPort | (socket.socket.getLocalPort << 16))

    def errorTranslate(socketError: SocketError): NetError = socketError match {
      case SocketError.UnknownHost(hostname) ⇒ NetError.HostNotFound(hostname)
      case e ⇒ NetError.UnexpectedError(e.show)
    }
  }
}
