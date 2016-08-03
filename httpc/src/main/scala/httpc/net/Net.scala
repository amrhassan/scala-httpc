package httpc.net

import cats.free.Free.liftF
import cats.implicits._

/** Network operations */
trait Net {

  /** Looks up an address by its hostname */
  def lookupAddress(hostname: String): NetIo[Address] =
    liftF(AddrLookup(hostname))

  /** Opens a connection to the given address and port */
  def connect(address: Address, port: Port): NetIo[ConnectionId] =
    liftF(Connect(address, port))

  /** Opens a secure connection to the given address and port */
  def connectSsl(address: Address, port: Port): NetIo[ConnectionId] =
    liftF(ConnectSsl(address, port))

  /** Reads data from a connection */
  def read(connectionId: ConnectionId, length: Int): NetIo[Array[Byte]] =
    liftF(Read(connectionId, length))

  /** Reads bytes until the specified marker byte and returns all bytes including the marker suffix */
  def readUntil(conId: ConnectionId, marker: Byte): NetIo[Vector[Byte]] =
    read(conId, 1).map(_.toVector) >>= { bytes ⇒
      if (bytes(0) === marker)
        NetIo.pure(bytes)
      else
        readUntil(conId, marker) map (bytes ++ _)
    }

  /** Writes data to a connection */
  def write(connectionId: ConnectionId, data: Array[Byte]): NetIo[Unit] =
    liftF(Write(connectionId, data))

  /** Disconnects from a connection */
  def disconnect(connectionId: ConnectionId): NetIo[Unit] =
    liftF(Disconnect(connectionId))
}
