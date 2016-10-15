package httpc.net

import cats.free.Free
import cats.implicits._
import scodec.bits.ByteVector


/** Networking operations */
private [net] trait NetOp[A]
private [net] case class AddrLookup(hostname: String) extends NetOp[Address]
private [net] case class Connect(address: Address, port: Port) extends NetOp[ConnectionId]
private [net] case class ConnectSsl(address: Address, port: Port) extends NetOp[ConnectionId]
private [net] case class Read(conId: ConnectionId, length: Int) extends NetOp[ByteVector]
private [net] case class Write(conId: ConnectionId, data: ByteVector) extends NetOp[Unit]
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
  val socketsInterpreter = new SocketsInterpreter(new SocketStore)
}

