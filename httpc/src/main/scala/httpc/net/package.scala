package httpc

import cats.free.Free

/** TCP Networking */
package object net extends Net {
  type NetIo[A] = Free[NetOp, A]
}
