# scala-httpc
[![Build Status](https://travis-ci.org/amrhassan/scala-httpc.svg?branch=master)](https://travis-ci.org/amrhassan/scala-httpc)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.amrhassan/httpc_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.amrhassan/httpc_2.11)


A minimal HTTP Client for Scala built using purely functional programming and [cats](https://github.com/typelevel/cats)

# Usage #

```
libraryDependencies += "io.github.amrhassan" %% "httpc" % "0.2.2"
```

```scala
import httpc._  // All you need to import actually
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
  }
}
```

# TODO #
* [X] Safe TCP sockets API
* [X] Minimal HTTP data structures and protocol
* [X] Convenience public library API
* [ ] HTTPS support
* [ ] Better documentation showcasing the public API
* [ ] Optional JSON Support
* [ ] Lots and lots of property tests
* [ ] Benchmarks and performance optimizations
