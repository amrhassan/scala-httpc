package httpc

import cats.free.Free
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric._


package object net {
  type NetIo[A] = Free[NetIoOp, A]
  type Length = Int Refined NonNegative
}
