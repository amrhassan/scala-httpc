package httpc

import cats.free.Free
import cats.~>

/** TCP Networking */
package object net extends Net {
  type NetAction[A] = Free[NetOp, A]
  type Interpreter = NetOp ~> Either[NetError, ?]
}
