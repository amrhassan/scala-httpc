
import sbt._

object Dependencies {

  val refined = {
    val version = "0.5.0"
    Seq(
      "eu.timepit" %% "refined" % version,
      "eu.timepit" %% "refined-scalacheck" % version % Test
    )
  }

  val cats = Seq("org.typelevel" %% "cats" % "0.6.0")
}

