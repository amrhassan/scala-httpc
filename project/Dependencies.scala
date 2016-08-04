
import sbt._

object Dependencies {

  val cats = Seq(
    "org.typelevel" %% "cats-core",
    "org.typelevel" %% "cats-free"
  ) map (_ % "0.6.1")

  val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-parser"
  ).map(_ % "0.5.0-M2")
}

