
import Dependencies._

val org = "io.github.amrhassan"
sonatypeProfileName := org

val commonDeps =
  cats ++
  scalaCheck ++
  enumeratum ++
  specs2 ++
  simulacrum

lazy val commonSettings = Seq(
  organization := org,
  scalaVersion := "2.12.0",
  crossScalaVersions := Seq("2.11.8", "2.12.0"),
  libraryDependencies ++= commonDeps,
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  publishArtifact in Test := false,
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  autoCompilerPlugins := true,
  scalacOptions ++= Seq(
    "-Xfatal-warnings",
    "-Ywarn-unused-import",
    "-Ypartial-unification",
    "-Xlint",
    "-feature",
    "-language:implicitConversions"
  ),
  scalacOptions in Test ++= Seq("-Yrangepos"),
  scalacOptions in (Compile, doc) ++= Seq(
    "-no-link-warnings" // Suppresses problems with Scaladoc @throws links
  ),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseCrossBuild := true,
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
    libraryDependencies ++= testUsingCirce ++ base64Test
  )
  .dependsOn(`httpc-net`)

val `httpc-circe` = project
  .settings(commonSettings:_*)
  .settings(
    name := "httpc-circe",
    libraryDependencies ++= circe ++ circeTesting
  )
  .dependsOn(`httpc-http` % "test->test;compile->compile")

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
