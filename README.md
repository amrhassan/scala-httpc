# scala-httpc
[![Build Status](https://travis-ci.org/amrhassan/scala-httpc.svg?branch=master)](https://travis-ci.org/amrhassan/scala-httpc)

A minimal HTTP Client for Scala built using purely functional programming and [cats](https://github.com/typelevel/cats)

# Usage #

```sbt
resolvers += Resolver.sonatypeRepo("snapshots")
libraryDependencies ++= Seq(
  "io.github.amrhassan" %% "httpc" % "0.3.0-SNAPSHOT",
  "io.github.amrhassan" %% "httpc-circe" % "0.3.0-SNAPSHOT"  // Optional Circe support
  )
```

```scala
import httpc._                  // All you need to import actually
import httpc.support.circe._    // For JSON support, if you included the httpc-circe module
import scala.concurrent.ExecutionContext.Implicits.global // Gotta have one of those in scope

// The rest of the imports are for the demo purposes
import cats.implicits._
import cats.data.Xor
import scala.concurrent.duration.Duration
import scala.concurrent.Await


object Sandbox extends App {

  // Description of a PUT request yielding a response
  val command = put("http://httpbin.org/put", data = "OK Computer")  

  // Run the command into a Future[HttpError Xor Response]
  val result = run(command)

  Await.result(result, Duration.Inf) match {
    case Xor.Left(error) ⇒ println(error.show)
    case Xor.Right(response) ⇒
      println(response.status)
      println(response.text)
      println(response.json)  // Only if you've imported httpc.support.circe._
  }
}
```

# TODO #
* [X] Safe TCP sockets API
* [X] Minimal HTTP data structures and protocol
* [X] Convenience public library API
* [X] HTTPS support
* [ ] Better documentation showcasing the public API
* [X] Optional JSON Support
* [ ] Lots and lots of property tests
* [ ] Benchmarks and performance optimizations
