name := "scala-httpc"

version := "0.0"
scalaVersion := "2.11.8"


libraryDependencies ++= Seq(
  "org.typelevel" %% "cats" % "0.6.0"
)

autoCompilerPlugins := true

addCompilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full)
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.8.0")

scalacOptions ++= Seq(
  "-Xfatal-warnings"
)


wartremoverErrors ++= Warts.unsafe
