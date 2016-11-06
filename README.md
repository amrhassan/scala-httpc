# scala-httpc
[![Build Status](https://travis-ci.org/amrhassan/scala-httpc.svg?branch=master)](https://travis-ci.org/amrhassan/scala-httpc)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.amrhassan/scala-httpc_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.amrhassan/scala-httpc_2.11)

A reasonably minimal HTTP Client for Scala built using purely functional programming and [cats](https://github.com/typelevel/cats)

Optional support for JSON payloads is available via [Circe](https://github.com/travisbrown/circe).

# Quick Usage #

```sbt
libraryDependencies ++= Seq("io.github.amrhassan" %% "httpc" % "0.3.0-RC1")
```

```scala
import httpc.all._

object Sandbox extends App {

  // Description of a PUT request yielding a response
  val command = put("http://httpbin.org/put", 
      data = "OK Computer",
      headers = List(cacheControlNoCache))

  // Run the command into an Either[HttpError, Response]
  run(command) match {
    case Left(error) ⇒ println(error)
    case Right(response) ⇒
      println(response.status)
      println(response.text)
      println(response.json)
  }
}
```
