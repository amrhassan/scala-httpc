# scala-httpc
HTTP Client for Scala built using purely functional programming and [cats](https://github.com/typelevel/cats)

# Usage Example #
```scala
import httpc._
import cats.data.Xor
import scala.concurrent.ExecutionContext.Implicits.global
import cats.implicits._

object Sandbox extends App {

  get("http://www.example.com/") map {
    case Xor.Left(e) ⇒ println("ERROR: " + e.show)
    case Xor.Right(resp) ⇒
      println(resp.status)
      println(resp.headers)
      println(new String(resp.body))
  }

  Thread.sleep(10000)
}

```
