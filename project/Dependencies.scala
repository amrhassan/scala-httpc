
import sbt._

object Dependencies {

  val cats = Seq("cats-core", "cats-free") map ("org.typelevel" %% _ % "0.7.2")

  val circe = Seq("circe-core", "circe-parser", "circe-generic") map ("io.circe" %% _ % "0.5.2")
  val circeTest = circe map (_ % Test)

  val scalaCheck = Seq(
    "org.scalacheck" %% "scalacheck" % "1.13.2",
    "io.github.amrhassan" %% "scalacheck-cats" % "0.1.2"
  ) map (_ % Test)

  val enumeratum = Seq("com.beachape" %% "enumeratum" % "1.4.9")

  val specs2 = Seq("specs2-core", "specs2-scalacheck", "specs2-cats") map ("org.specs2" %% _ % "3.8.5" % Test)

  val base64Test = Seq("com.github.marklister" %% "base64" % "0.2.2" % Test)
}

