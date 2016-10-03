package httpc.http

import cats.implicits._
import HttpAction._
import httpc.net.{Bytes, ConnectionId, NetInterpreters}
import httpc.net
import Render.ops._


/** Module API */
trait Http {

  val HttpVersion = "HTTP/1.1".getBytes.toVector

  /** Dispatches a request yielding a response for it */
  def dispatch(url: Url, r: Request): HttpAction[Response] =
    sendRequest(url, r) >>= receiveResponse

  /** Sends the given request and returns the [[ConnectionId]] of the connection */
  private [httpc] def sendRequest(url: Url, r: Request): HttpAction[ConnectionId] =
    for {
      netProtocol <- NetProtocol.fromUrl(url)
      address <- fromNetIo(net.lookupAddress(url.host))
      con ← fromNetIo(netProtocol.connect(address, url.port.getOrElse(netProtocol.defaultPort)))
      _ ← fromNetIo(net.write(con, r.render.toArray))
    } yield con

  private [httpc] def receiveResponse(connectionId: ConnectionId): HttpAction[Response] =
    for {
      status ← receiveStatus(connectionId)
      headers ← receiveHeaders(connectionId)
      bodySize ← either(Headers.determineBodySize(headers).toRight(HttpError.MissingContentLength))
      body ← fromNetIo(net.read(connectionId, bodySize))
      _ ← fromNetIo(net.disconnect(connectionId))
    } yield Response(status, headers, body)

  private [httpc] def buildRequest[A: ToRequest](method: Method, url: Url, data: A): Request = {

    val body = ToRequest[A].body(data)

    val requiredHeaders = List(
      Header.host(url.host),
      Header.contentLength(body.length))

    val headers = Headers.overwrite(requiredHeaders, ToRequest[A].fallbackHeaders)

    Request(method, url.path, Message(headers, body))
  }

  private [httpc] def receiveHeaders(con: ConnectionId): HttpAction[List[Header]] = {
    def readHeader(line: Vector[Byte]): HttpAction[Header] = either {
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

  private def receiveLine(con: ConnectionId): HttpAction[Vector[Byte]] = fromNetIo {
    net.readUntil(con, '\n'.toByte)
  }

  def run[A](command: HttpAction[A]): Either[HttpError, A] =
    HttpAction.run(command, netInterpreter)

  private val netInterpreter: net.Interpreter = NetInterpreters.socketsInterpreter
}
