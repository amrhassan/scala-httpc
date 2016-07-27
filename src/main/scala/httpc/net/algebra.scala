package httpc.net

import scala.concurrent.{ExecutionContext, Future}
import cats.data.XorT
import cats.free.Free.liftF
import cats.free.Free
import cats.implicits._
import cats.~>
import eu.timepit.refined._


/** Networking operations */
private [net] trait NetIoOp[A]
private [net] case class AddrLookup(hostname: String) extends NetIoOp[Address]
private [net] case class Connect(address: Address, port: Port) extends NetIoOp[ConnectionId]
private [net] case class Read(conId: ConnectionId, length: Length) extends NetIoOp[Array[Byte]]
private [net] case class Write(conId: ConnectionId, data: Array[Byte]) extends NetIoOp[Unit]
private [net] case class Disconnect(conId: ConnectionId) extends NetIoOp[Unit]


/** Monadic network operations implemented as a Free monad with the [[NetIo.Interpreter]] interpreter type */
object NetIo {

  /** The interpreter can be used with the `run()` function to produce a result out of a description of an action */
  type Interpreter = NetIoOp ~> XorT[Future, NetError, ?]

  /** Looks up an address by its hostname */
  def lookupAddress(hostname: String): NetIo[Address] =
    liftF(AddrLookup(hostname))

  /** Opens a connection to the given address and port */
  def connect(address: Address, port: Port): NetIo[ConnectionId] =
    liftF(Connect(address, port))

  /** Reads data from a connection */
  def read(connectionId: ConnectionId, length: Length): NetIo[Array[Byte]] =
    liftF(Read(connectionId, length))

  /** Reads bytes until the specified marker byte and returns all bytes including the marker suffix */
  def readUntil(conId: ConnectionId, marker: Byte): NetIo[Vector[Byte]] =
    read(conId, refineMV(1)).map(_.toVector) >>= { bytes ⇒
      if (bytes(0) === marker)
        pure(bytes)
      else
        readUntil(conId, marker) map (bytes ++ _)
    }

  /** Writes data to a connection */
  def write(connectionId: ConnectionId, data: Array[Byte]): NetIo[Unit] =
    liftF(Write(connectionId, data))

  /** Disconnects from a connection */
  def disconnect(connectionId: ConnectionId): NetIo[Unit] =
    liftF(Disconnect(connectionId))

  def pure[A](a: A): NetIo[A] =
    Free.pure(a)

  /** Executes a TCP program based on the interpreter */
  def run[A](prog: NetIo[A], interpreter: Interpreter)(implicit ec: ExecutionContext): XorT[Future, NetError, A] =
    prog.foldMap(interpreter)
}
