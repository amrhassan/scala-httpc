package httpc.net

import scala.concurrent.{ExecutionContext, Future}
import cats.data.XorT
import cats.free.Free
import cats.implicits._
import cats.~>

/** Networking operations */
private [net] trait NetIoOp[A]
private [net] case class AddrLookup(hostname: String) extends NetIoOp[Address]
private [net] case class Connect(address: Address, port: Port) extends NetIoOp[ConnectionId]
private [net] case class ConnectSsl(address: Address, port: Port) extends NetIoOp[ConnectionId]
private [net] case class Read(conId: ConnectionId, length: Length) extends NetIoOp[Array[Byte]]
private [net] case class Write(conId: ConnectionId, data: Array[Byte]) extends NetIoOp[Unit]
private [net] case class Disconnect(conId: ConnectionId) extends NetIoOp[Unit]


object NetIo {

  /** The interpreter can be used with the `run()` function to produce a result out of a description of an action */
  type Interpreter = NetIoOp ~> XorT[Future, NetError, ?]

  def pure[A](a: A): NetIo[A] =
    Free.pure(a)

  /** Executes a TCP program based on the interpreter */
  def run[A](prog: NetIo[A], interpreter: Interpreter)(implicit ec: ExecutionContext): XorT[Future, NetError, A] =
    prog.foldMap(interpreter)
}
