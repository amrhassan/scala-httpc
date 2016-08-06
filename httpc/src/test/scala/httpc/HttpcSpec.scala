package httpc

import org.specs2.{ScalaCheck, Specification}
import httpc.http.Arbitraries._
import httpc.http.{Message, Request, Status}
import scala.concurrent.ExecutionContext.Implicits.global
import cats.implicits._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.{FutureMatchers, XorMatchers}
import scala.concurrent.duration._
import cats.data.XorT


class HttpcSpec(implicit ee: ExecutionEnv) extends Specification with ScalaCheck with FutureMatchers with XorMatchers { def is = s2"""
  GET requests yield appropriate response ${check(http.Method.Get)}
  PUT requests yield appropriate response ${check(http.Method.Put)}
  POST requests yield appropriate response ${check(http.Method.Post)}
  """


  def check(method: http.Method) = prop { (m: Message) ⇒

    val action = for {
      _url ← url(method)
      request = Request(method, _url.path, m)
      d ← http.dispatch(_url, request)
    } yield d

    val resp = XorT(run(action))

    val statusCheck = resp.map(_.status).value must beXorRight(Status(200)).await(1, 1 minute)

    statusCheck
  }.set(minTestsOk = 4)

}
