package httpc.net

import cats.free.Free.liftF
import cats.implicits._
import scodec.bits.ByteVector

/** Network operations */
trait Net {

  /** Looks up an address by its hostname */
  def lookupAddress(hostname: String): NetAction[Address] =
    liftF(AddrLookup(hostname))

  /** Opens a connection to the given address and port */
  def connect(address: Address, port: Port): NetAction[ConnectionId] =
    liftF(Connect(address, port))

  /** Opens a secure connection to the given address and port */
  def connectSsl(address: Address, port: Port): NetAction[ConnectionId] =
    liftF(ConnectSsl(address, port))

  /** Reads data from a connection */
  def read(connectionId: ConnectionId, length: Int): NetAction[ByteVector] =
    liftF(Read(connectionId, length))

  /** Reads bytes until the specified marker byte and returns all bytes including the marker suffix */
  def readUntil(conId: ConnectionId, marker: Byte): NetAction[ByteVector] =
    read(conId, 1) >>= { bytes ⇒
      if (bytes(0) === marker)
        NetAction.pure(bytes)
      else
        readUntil(conId, marker) map (bytes ++ _)
    }

  /** Keeps re-reading until getting the specified amount of bytes */
  def mustRead(connectionId: ConnectionId, length: Int): NetAction[ByteVector] =
    if (length == 0)
      NetAction.pure(ByteVector.empty)
    else
      read(connectionId, length) >>= { got =>
        if (got.length < length)
          mustRead(connectionId, length - got.length.toInt) map (got ++ _)
        else
          NetAction.pure(got)
      }

  /** Writes data to a connection */
  def write(connectionId: ConnectionId, data: ByteVector): NetAction[Unit] =
    liftF(Write(connectionId, data))

  /** Disconnects from a connection */
  def disconnect(connectionId: ConnectionId): NetAction[Unit] =
    liftF(Disconnect(connectionId))
}
