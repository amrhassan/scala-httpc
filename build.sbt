
import Dependencies._

val commonDeps =
  cats ++
  refined

lazy val commonSettings = Seq(
  organization := "io.github.amrhassan",
  version := "0.3.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  libraryDependencies ++= commonDeps,
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.8.0"),
  addCompilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full),
  publishArtifact in Test := false,
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  autoCompilerPlugins := true,
  scalacOptions ++= Seq(
    "-Xfatal-warnings",
    "-Ywarn-unused-import",
    "-Xlint",
    "-feature"
  ),
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
)

lazy val httpc = project
  .settings(commonSettings:_*)
  .settings(
    name := "httpc"
  )

lazy val root = (project in file("."))
  .settings(packagedArtifacts := Map.empty)
  .aggregate(httpc)
