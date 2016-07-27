package httpc.tcp

import scala.concurrent.{ExecutionContext, Future}
import cats.data.XorT
import cats.free.Free.liftF
import cats.free.Free
import cats.implicits._
import cats.~>
import eu.timepit.refined._


/** Networking operations */
private [tcp] trait TcpNetOp[A]
private [tcp] case class AddrLookup(hostname: String) extends TcpNetOp[Address]
private [tcp] case class Connect(address: Address, port: Port) extends TcpNetOp[ConnectionId]
private [tcp] case class Read(conId: ConnectionId, length: Length) extends TcpNetOp[Array[Byte]]
private [tcp] case class Write(conId: ConnectionId, data: Array[Byte]) extends TcpNetOp[Unit]
private [tcp] case class Disconnect(conId: ConnectionId) extends TcpNetOp[Unit]


/** Monadic TCP operations implemented as a Free monad with the [[TcpNet.Interpreter]] interpreter type */
object TcpNet {

  /** The interpreter can be used with the `run()` function to produce a result out of a description of an action */
  type Interpreter = TcpNetOp ~> XorT[Future, TcpError, ?]

  /** Looks up an address by its hostname */
  def lookupAddress(hostname: String): TcpNet[Address] =
    liftF(AddrLookup(hostname))

  /** Opens a connection to the given address and port */
  def connect(address: Address, port: Port): TcpNet[ConnectionId] =
    liftF(Connect(address, port))

  /** Reads data from a connection */
  def read(connectionId: ConnectionId, length: Length): TcpNet[Array[Byte]] =
    liftF(Read(connectionId, length))

  /** Reads bytes until the specified marker byte and returns all bytes including the marker suffix */
  def readUntil(conId: ConnectionId, marker: Byte): TcpNet[Vector[Byte]] =
    read(conId, refineMV(1)).map(_.toVector) >>= { bytes ⇒
      if (bytes(0) === marker)
        pure(bytes)
      else
        readUntil(conId, marker) map (bytes ++ _)
    }

  /** Writes data to a connection */
  def write(connectionId: ConnectionId, data: Array[Byte]): TcpNet[Unit] =
    liftF(Write(connectionId, data))

  /** Disconnects from a connection */
  def disconnect(connectionId: ConnectionId): TcpNet[Unit] =
    liftF(Disconnect(connectionId))

  def pure[A](a: A): TcpNet[A] =
    Free.pure(a)

  /** Executes a TCP program based on the interpreter */
  def run[A](prog: TcpNet[A], interpreter: Interpreter)(implicit ec: ExecutionContext): XorT[Future, TcpError, A] =
    prog.foldMap(interpreter)
}
