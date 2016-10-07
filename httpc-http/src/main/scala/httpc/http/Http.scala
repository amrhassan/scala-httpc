package httpc.http

import scala.util.Try
import cats.implicits._
import HttpAction._
import httpc.net.{Bytes, ConnectionId, NetInterpreters}
import httpc.net
import Render.ops._


/** Module API */
trait Http {

  val HttpVersion = "HTTP/1.1".getBytes.toVector

  /** Dispatches a request yielding a response for it */
  def dispatch(url: Url, r: Request, options: Options): HttpAction[Response] =
    for {
      netProtocol ← NetProtocol.fromUrl(url)
      address ← fromNetIo(net.lookupAddress(url.host))
      con ← fromNetIo(netProtocol.connect(address, url.port.getOrElse(netProtocol.defaultPort)))
      _ ← fromNetIo(net.write(con, r.render.toArray))
      status ← readStatus(con)
      headers ← readHeaders(con)
      body ← fromNetIo(net.read(con, bodySize(headers, options.maxResponseBodySize)))
      _ ← fromNetIo(net.disconnect(con))
    } yield Response(status, headers, body)

  /** Builds a request */
  def request[A: ToRequest](method: Method, url: Url, data: A): Request = {
    val dataBytes = ToRequest[A].body(data)

    val requiredHeaders = List(Header.host(url.host), Header.contentLength(dataBytes.length))
    val customHeaders = ToRequest[A].fallbackHeaders

    val message = Message(requestHeaders(requiredHeaders, customHeaders), dataBytes)
    Request(method, url.path, message)
  }

  private def requestHeaders(requiredHeaders: List[Header], customHeaders: List[Header]): List[Header] = {
    // Required headers overridden by custom headers
    val z = requiredHeaders.map(h ⇒ (h.name, h)).toMap
    customHeaders.foldRight(z)((header, headers) ⇒ headers.updated(header.name, header)).values.toList
  }

  private def bodySize(headers: List[Header], maxValue: Int): Int = {
    val fromResponse = for {
      header <- headers.find(_.name == HeaderNames.ContentLength)
      size <- Try(header.value.toInt).toOption
    } yield size
    fromResponse.getOrElse(maxValue).max(maxValue)
  }

  private def readHeaders(con: ConnectionId): HttpAction[List[Header]] = {
    def readHeader(line: Vector[Byte]): HttpAction[Header] = either {
      Header.read(line).toRight(HttpError.MalformedHeader(new String(line.toArray).trim))
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

  private def readLine(con: ConnectionId): HttpAction[Vector[Byte]] = fromNetIo {
    net.readUntil(con, '\n'.toByte)
  }

  def run[A](command: HttpAction[A]): Either[HttpError, A] =
    HttpAction.run(command, netInterpreter)

  private val netInterpreter: net.Interpreter = NetInterpreters.socketsInterpreter
}
