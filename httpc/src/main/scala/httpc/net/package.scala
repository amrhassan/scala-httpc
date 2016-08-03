package httpc

import scala.concurrent.Future
import cats.data.XorT
import cats.free.Free
import cats.~>

/** TCP Networking */
package object net extends Net {
  type NetIo[A] = Free[NetOp, A]
  type Interpreter = NetOp ~> XorT[Future, NetError, ?]
}
