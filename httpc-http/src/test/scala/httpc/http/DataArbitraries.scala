package httpc.http

import org.scalacheck._
import scalacheck.cats._
import Gen._
import Arbitrary._
import cats.implicits._

object DataArbitraries {

  val genUrlString: Gen[String] = {

    val genScheme: Gen[String] =
      oneOf("http", "https") map (_ |+| "://")

    val genAuth: Gen[String] =
      (alphaNumStr |@| alphaNumStr) map { (username, password) =>
        if (username.nonEmpty || password.nonEmpty)
          username |+| ":" |+| password |+| "@"
        else ""
      }

    val genHost: Gen[String] =
      alphaNumStr

    val genPort: Gen[String] =
      (chooseNum[Int](0, 65535, 80) |@| arbitrary[Boolean]) map { (port, bool) =>
        if (port == 80 && bool) "" else ":" |+| port.toString
      }

    val genUrlPathSegment: Gen[String] =
      alphaNumStr

    val genPathSegments: Gen[String] =
      listOf(genUrlPathSegment).map(_.fold("/")(_ |+| "/" |+| _))

    val genUrlQueryParam: Gen[String] =
      (alphaNumStr |@| alphaNumStr) map (_ |+| "=" |+| _)

    val genQuery: Gen[String] =
      listOf(genUrlQueryParam) map { (ps) =>
        if (ps.nonEmpty)
          "?" |+| ps.mkString("&")
        else
          ""
      }

    val genFragment: Gen[String] =
      alphaNumStr map { fragment =>
        if (fragment.nonEmpty)
          "#" |+| fragment
        else
          ""
      }

    val genPath: Gen[String] =
      (genPathSegments |@| genQuery |@| genFragment) map (_ |+| _ |+| _)

    (genScheme |@| genAuth |@| genHost |@| genPort |@| genPath) map (_ |+| _ |+| _ |+| _ |+| _)
  }
}
