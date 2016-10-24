
import Dependencies._

val commonDeps =
  cats ++
  scalaCheck ++
  enumeratum ++
  specs2 ++
  simulacrum

lazy val commonSettings = Seq(
  organization := "io.github.amrhassan",
  version := "0.3.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0-RC2"),
  libraryDependencies ++= commonDeps,
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.8.0"),
  addCompilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  publishArtifact in Test := false,
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  autoCompilerPlugins := true,
  scalacOptions ++= Seq(
    "-Xfatal-warnings",
    "-Ywarn-unused-import",
    "-Xlint",
    "-feature",
    "-language:implicitConversions"
  ),
  scalacOptions in Test ++= Seq("-Yrangepos"),
  scalacOptions in (Compile, doc) ++= Seq(
    "-no-link-warnings" // Suppresses problems with Scaladoc @throws links
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

val `httpc-net` = project
  .settings(commonSettings:_*)
  .settings(
    name := "httpc-net",
    libraryDependencies ++= scodecBits
  )

val `httpc-http` = project
  .settings(commonSettings:_*)
  .settings(
    name := "httpc-http",
    libraryDependencies ++= circeTest ++ base64Test
  )
  .dependsOn(`httpc-net`)

val `httpc-circe` = project
  .settings(commonSettings:_*)
  .settings(
    name := "httpc-circe",
    libraryDependencies ++= circe
  )
  .dependsOn(`httpc-http`)

val httpc = project
  .settings(commonSettings:_*)
  .settings(
    name := "httpc"
  )
  .dependsOn(`httpc-http`)
  .dependsOn(`httpc-circe`)

lazy val `scala-httpc` = (project in file("."))
  .settings(packagedArtifacts := Map.empty)
  .aggregate(httpc, `httpc-net`, `httpc-circe`, `httpc-http`)
