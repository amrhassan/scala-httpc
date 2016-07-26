package httpc

import cats.free.Free
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric._


package object tcp {
  type TcpNet[A] = Free[TcpNetOp, A]
  type Length = Int Refined NonNegative
}
