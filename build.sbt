name := "scala-httpc"

version := "0.1.6"
scalaVersion := "2.11.8"
organization := "io.github.amrhassan"


val cats = Seq("org.typelevel" %% "cats" % "0.6.0" )
val refined = Seq(
  "eu.timepit" %% "refined" % "0.5.0",
  "eu.timepit" %% "refined-scalacheck" % "0.5.0" % "test"
)


publishArtifact in Test := false
publishMavenStyle := true
pomIncludeRepository := { _ => false }


libraryDependencies ++= cats ++ refined

autoCompilerPlugins := true

addCompilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full)
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.8.0")

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-Ywarn-unused-import",
  "-Xlint",
  "-feature"
)


pomExtra := (
  <url>https://amrhassan.github.io/scala-httpc/</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>http://www.opensource.org/licenses/mit-license.php</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:amrhassan/scala-httpc.git</url>
      <connection>scm:git:git@github.com:amrhassan/scala-httpc.git</connection>
    </scm>
    <developers>
      <developer>
        <id>amrhassan</id>
        <name>Amr Hassan</name>
        <url>http://amrhassan.info</url>
      </developer>
    </developers>)
