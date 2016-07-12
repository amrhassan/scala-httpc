name := "scala-httpc"

version := "0.0"
scalaVersion := "2.11.8"

val cats = Seq("org.typelevel" %% "cats" % "0.6.0" )
val refined = Seq(
  "eu.timepit" %% "refined" % "0.5.0",
  "eu.timepit" %% "refined-scalacheck" % "0.5.0" % "test"
)


libraryDependencies ++= cats ++ refined


autoCompilerPlugins := true

addCompilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full)
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.8.0")

scalacOptions ++= Seq(
  "-Xfatal-warnings"
)


wartremoverErrors ++= Warts.unsafe
