package httpc.http

import cats.implicits._
import HttpAction._
import httpc.net.{Bytes, ConnectionId, NetInterpreters}
import httpc.net
import Render.ops._
import httpc.http.HttpError.UnspecifiedTransferModeError
import scodec.bits.ByteVector

/** Module API */
trait Http {

  val HttpVersion = Bytes.fromUtf8("HTTP/1.1")

  /** Dispatches a request yielding a response for it */
  def dispatch(url: Url, r: Request): HttpAction[Response] =
    for {
      netProtocol ← NetProtocol.fromUrl(url)
      address ← fromNetIo(net.lookupAddress(url.host))
      con ← fromNetIo(netProtocol.connect(address, url.port.getOrElse(netProtocol.defaultPort)))
      _ ← fromNetIo(net.write(con, r.render))
      status ← readStatus(con)
      headers ← readHeaders(con)
      transferMode <- either(TransferMode.fromResponseHeaders(headers))
      body <- readBody(con, transferMode)
      _ ← fromNetIo(net.disconnect(con))
    } yield Response(status, headers, body)

  /** Builds a request */
  def request[A: Entity](method: Method, url: Url, data: A, headers: List[Header]): Request = {
    val dataBytes = Entity[A].body(data)

    val requiredHeaders = List(Header.host(url.host), Header.contentLength(dataBytes.length))
    val customHeaders = Entity[A].fallbackHeaders ++ headers

    val message = Message(requestHeaders(requiredHeaders, customHeaders), dataBytes)
    Request(method, url.path, message)
  }

  private def requestHeaders(requiredHeaders: List[Header], customHeaders: List[Header]): List[Header] = {
    // Required headers overridden by custom headers
    val z = requiredHeaders.map(h ⇒ (h.name, h)).toMap
    customHeaders.foldRight(z)((header, headers) ⇒ headers.updated(header.name, header)).values.toList
  }

  private def readBody(connectionId: ConnectionId, mode: TransferMode): HttpAction[ByteVector] =
    mode match {
      case UnspecifiedTransferMode => HttpAction.error(UnspecifiedTransferModeError)
      case FixedLengthTransferMode(length) => fromNetIo(net.read(connectionId, length))
      case ChunkedTransferMode => ChunkedTransferMode.readAllChunks(connectionId)
    }

  private def readHeaders(con: ConnectionId): HttpAction[List[Header]] = {
    def readHeader(line: ByteVector): HttpAction[Header] = either {
      Header.read(line).toRight(HttpError.MalformedHeader(Bytes.toString(line).trim))
    }
    readLine(con) >>= { line ⇒
      if (Bytes.isWhitespace(line)) {
        HttpAction.pure(List.empty)
      } else {
        readHeader(line) >>= { header ⇒
          readHeaders(con) map (header :: _)
        }
      }
    }
  }

  private def readStatus(con: ConnectionId): HttpAction[Status] =
    readLine(con) >>= { line ⇒
      val parts = Bytes.split(line, ' '.toByte)
      val status = Status.read(parts(1)).toRight(HttpError.MalformedStatus(Bytes.toString(line).trim))
      HttpAction.either(status)
    }

  private def readLine(con: ConnectionId): HttpAction[ByteVector] = fromNetIo {
    net.readUntil(con, '\n'.toByte)
  }

  def run[A](command: HttpAction[A]): Either[HttpError, A] =
    HttpAction.run(command, netInterpreter)

  private val netInterpreter: net.Interpreter = NetInterpreters.socketsInterpreter
}
