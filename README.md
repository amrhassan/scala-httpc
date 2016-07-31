# scala-httpc
[![Build Status](https://travis-ci.org/amrhassan/scala-httpc.svg?branch=master)](https://travis-ci.org/amrhassan/scala-httpc)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.amrhassan/httpc_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.amrhassan/httpc_2.11)


A minimal HTTP Client for Scala built using purely functional programming and [cats](https://github.com/typelevel/cats)

# Usage #

```
libraryDependencies += "io.github.amrhassan" %% "httpc" % "0.2.0"
```

```scala
import httpc._
import scala.concurrent.ExecutionContext.Implicits.global
import cats.data.Xor
import cats.implicits._


object Sandbox extends App {

  val command =
    put("http://httpbin.org/put", data = "OK Computer")  // Description of a PUT request yielding a response

  // Run command into an Future[HttpError Xor Response]
  run(command).map {
    case Xor.Left(error) ⇒ println(error.show)
    case Xor.Right(response) ⇒
      println(response.status)
      println(response.text)
  }

  // Because all APIs are non-blocking
  Thread.sleep(10000)
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
