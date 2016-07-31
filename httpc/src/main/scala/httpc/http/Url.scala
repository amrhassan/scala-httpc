package httpc.http

import cats.data.Xor
import httpc.http.HttpError.MalformedUrl
import HttpAction._

case class Url(protocol: String, host: String, path: String)

object Url {
  def parse(url: String): HttpAction[Url] = xor {
    Xor.catchNonFatal(new java.net.URL(url)).leftMap(_ ⇒ MalformedUrl(url)) map { parsed ⇒
      Url(parsed.getProtocol, parsed.getHost, parsed.getPath)
    }
  }
}
