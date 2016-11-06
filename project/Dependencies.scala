
import sbt._

object Dependencies {

  val cats = Seq("cats-core", "cats-free") map ("org.typelevel" %% _ % "0.8.0")

  val circe = Seq("circe-core", "circe-parser", "circe-generic") map ("io.circe" %% _ % "0.6.0-RC1")
  val circeTest = circe map (_ % Test)

  val scalaCheck = Seq(
    "org.scalacheck" %% "scalacheck" % "1.13.4",
    "io.github.amrhassan" %% "scalacheck-cats" % "0.2.1"
  ) map (_ % Test)

  val enumeratum = Seq("com.beachape" %% "enumeratum" % "1.4.17")

  val specs2 = Seq("specs2-core", "specs2-scalacheck", "specs2-cats") map ("org.specs2" %% _ % "3.8.6" % Test)

  val base64Test = Seq("com.github.marklister" %% "base64" % "0.2.2" % Test)

  val simulacrum = Seq("com.github.mpilquist" %% "simulacrum" % "0.10.0")

  val scodecBits = Seq("org.scodec" %% "scodec-bits" % "1.1.2")
}

