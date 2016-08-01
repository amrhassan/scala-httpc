package httpc.net

import scala.concurrent.{ExecutionContext, Future}
import cats.data.XorT
import cats.free.Free
import cats.implicits._
import cats.~>
import httpc.net.NetError.ConnectionNotFound
import httpc.net.sockets.{Addresses, Socket, SocketError}

/** Networking operations */
private [net] trait NetOp[A]
private [net] case class AddrLookup(hostname: String) extends NetOp[Address]
private [net] case class Connect(address: Address, port: Port) extends NetOp[ConnectionId]
private [net] case class ConnectSsl(address: Address, port: Port) extends NetOp[ConnectionId]
private [net] case class Read(conId: ConnectionId, length: Length) extends NetOp[Array[Byte]]
private [net] case class Write(conId: ConnectionId, data: Array[Byte]) extends NetOp[Unit]
private [net] case class Disconnect(conId: ConnectionId) extends NetOp[Unit]


object NetIo {

  /** The interpreter can be used with the `run()` function to produce a result out of a description of an action */
  type Interpreter = NetOp ~> XorT[Future, NetError, ?]

  def pure[A](a: A): NetIo[A] =
    Free.pure(a)

  /** Executes a TCP program based on the interpreter */
  def run[A](prog: NetIo[A], interpreter: Interpreter)(implicit ec: ExecutionContext): XorT[Future, NetError, A] =
    prog.foldMap(interpreter)
}

/** Interpreters for network communication */
object NetInterpreters {

  /** Interpreter for TCP language using OS sockets */
  def socketsInterpreter(implicit ec: ExecutionContext) = new (NetOp ~> XorT[Future, NetError, ?]) {

    var cons = Map.empty[ConnectionId, Socket]

    def apply[A](fa: NetOp[A]): XorT[Future, NetError, A] = XorT.fromXor[Future](fa match {
      case AddrLookup(hostname) ⇒
        Addresses.lookup(hostname) leftMap errorTranslate
      case Connect(address, port) ⇒
        for {
          socket <- Socket.connect(address, port) leftMap errorTranslate
          id = conId(socket)
        } yield { cons = cons.updated(id, socket); id }
      case ConnectSsl(address, port) ⇒
        for {
          socket ← Socket.connectSsl(address, port) leftMap errorTranslate
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
