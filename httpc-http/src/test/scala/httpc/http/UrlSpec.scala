package httpc.http

import org.specs2._
import DataArbitraries._
import cats.implicits._

class UrlSpec extends Specification with ScalaCheck { def is = s2"""
  Parsing URLs is correct $parsing
  """

  def parsing = prop { (url: String) =>
    Url.parse(url).map(_.show) must beSome(url)
  }.setGen(genUrlString)
}
