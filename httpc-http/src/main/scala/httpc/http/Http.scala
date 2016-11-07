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
      con <- sendRequest(url, r)
      response <- receiveResponse(con)
      _ <- fromNetIo(net.disconnect(con))
    } yield response

  /** Sends the given request and returns the [[ConnectionId]] of the connection */
  private [httpc] def sendRequest(url: Url, r: Request): HttpAction[ConnectionId] =
    for {
      netProtocol <- NetProtocol.fromUrl(url)
      address <- fromNetIo(net.lookupAddress(url.host))
      con <- fromNetIo(netProtocol.connect(address, url.port.getOrElse(netProtocol.defaultPort)))
      _ <- fromNetIo(net.write(con, r.render))
    } yield con

  private [httpc] def receiveResponse(connectionId: ConnectionId): HttpAction[Response] =
    for {
      status <- receiveStatus(connectionId)
      headers <- receiveHeaders(connectionId)
      transferMode <- either(TransferMode.fromResponseHeaders(headers))
      body <- readBody(connectionId, transferMode)
    } yield Response(status, headers, body)

  def buildRequest[A: Entity](method: Method, url: Url, data: A, userHeaders: Headers): Request = {
    val body = Entity[A].body(data)
    val requiredHeaders = Headers(Header.host(url.host), Header.contentLength(body.length))

    val headers = requiredHeaders overwriteWith (Entity[A].fallbackHeaders |+| userHeaders)

    Request(method, url.resource, Message(headers, body))
  }

  private def readBody(connectionId: ConnectionId, mode: TransferMode): HttpAction[ByteVector] =
    mode match {
      case UnspecifiedTransferMode => HttpAction.error(UnspecifiedTransferModeError)
      case FixedLengthTransferMode(length) => fromNetIo(net.read(connectionId, length))
      case ChunkedTransferMode => ChunkedTransferMode.readAllChunks(connectionId)
    }

  private def receiveHeaders(con: ConnectionId): HttpAction[List[Header]] = {
    def readHeader(line: ByteVector): HttpAction[Header] = either {
      Header.read(line).toRight(HttpError.MalformedHeader(Bytes.toString(line).trim))
    }
    receiveLine(con) >>= { line ⇒
      if (Bytes.isWhitespace(line)) {
        HttpAction.pure(List.empty)
      } else {
        readHeader(line) >>= { header ⇒
          receiveHeaders(con) map (header :: _)
        }
      }
    }
  }

  private [httpc] def receiveStatus(con: ConnectionId): HttpAction[Status] =
    receiveLine(con) >>= { line ⇒
      val parts = Bytes.split(line, ' '.toByte)
      val status = Status.read(parts(1)).toRight(HttpError.MalformedStatus(Bytes.toString(line).trim))
      HttpAction.either(status)
    }

  private def receiveLine(con: ConnectionId): HttpAction[ByteVector] = fromNetIo {
    net.readUntil(con, '\n'.toByte)
  }

  def run[A](command: HttpAction[A]): Either[HttpError, A] =
    HttpAction.run(command, netInterpreter)

  private val netInterpreter: net.Interpreter = NetInterpreters.socketsInterpreter
}
