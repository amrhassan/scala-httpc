package httpc.http

import httpc.net.{ConnectionId, NetAction}
import httpc.net
import cats.implicits._
import httpc.http.HttpError.CorruptedContentLength

private [httpc] sealed trait TransferMode
private [httpc] case class FixedLengthTransferMode(length: Int) extends TransferMode
private [httpc] case object UnspecifiedTransferMode extends TransferMode

private [httpc] object ChunkedTransferMode extends TransferMode {

  def readAllChunks(connectionId: ConnectionId): NetAction[Vector[Byte]] =
    readChunkSize(connectionId) >>= { size =>
      if (size > 0)
        for {
          data <- net.mustRead(connectionId, size)
          _ <- net.read(connectionId, 2)  // the trailer
          next <- readAllChunks(connectionId)
        } yield data.toVector ++ next
      else
        NetAction.pure(Vector.empty)
    }

  def readChunkSize(connectionId: ConnectionId): NetAction[Int] = for {
    header <- net.readUntil(connectionId, trailerHead)
    _ <- net.read(connectionId, 1)  // rest of trailer
    sizeHex = net.Bytes.toString(header.init)
    size = Integer.valueOf(sizeHex, 16)
  } yield size

  private val trailerHead = '\r'.toByte
  private val trailer: Vector[Byte] = "\r\n".getBytes.toVector

  def fromResponseHeaders(headers: List[Header]): Option[Either[HttpError, TransferMode]] =
    if (headers.contains(Header.transferEncodingCunked))
      Either.right(ChunkedTransferMode).some
    else None
}

private [httpc] object TransferMode {

  def fromResponseHeaders(headers: List[Header]): Either[HttpError, TransferMode] = {
    val modes = ChunkedTransferMode.fromResponseHeaders(headers) orElse FixedLengthTransferMode.fromResponseHeaders(headers)
    modes.getOrElse(Either.right(UnspecifiedTransferMode))
  }
}

private [httpc] object FixedLengthTransferMode {

  def fromResponseHeaders(headers: List[Header]): Option[Either[HttpError, TransferMode]] =
    for {
      header <- headers.find(_.name == HeaderNames.ContentLength)
      length = Either.catchNonFatal(header.value.toInt).left.map(_ => CorruptedContentLength)
    } yield length.map(FixedLengthTransferMode.apply)
}
