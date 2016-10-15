package httpc.http

import httpc.net
import cats.implicits._
import httpc.http.HttpError.{CorruptedChunkedResponse, CorruptedContentLength}
import HttpAction._
import httpc.net.ConnectionId
import scodec.bits.ByteVector

private [httpc] sealed trait TransferMode
private [httpc] case class FixedLengthTransferMode(length: Int) extends TransferMode
private [httpc] case object UnspecifiedTransferMode extends TransferMode

private [httpc] object ChunkedTransferMode extends TransferMode {

  def readAllChunks(connectionId: ConnectionId): HttpAction[ByteVector] =
    readChunkSize(connectionId) >>= { size =>
      if (size > 0)
        for {
          data <- fromNetIo(net.mustRead(connectionId, size))
          _ <- fromNetIo(net.read(connectionId, 2))  // the trailer
          next <- readAllChunks(connectionId)
        } yield data ++ next
      else
        pure(ByteVector.empty)
    }

  def readChunkSize(connectionId: ConnectionId): HttpAction[Int] = for {
    header <- fromNetIo(net.readUntil(connectionId, '\r'.toByte))
    _ <- fromNetIo(net.read(connectionId, 1))  // rest of trailer
    sizeHex = net.Bytes.toString(header.init)
    size <- either(Either.catchOnly[NumberFormatException](Integer.valueOf(sizeHex, 16)).left.map(_ => CorruptedChunkedResponse))
  } yield size

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
