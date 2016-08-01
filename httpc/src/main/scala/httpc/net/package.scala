package httpc

import cats.free.Free
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric._
import eu.timepit.refined._

/** TCP Networking */
package object net extends Net {
  type NetIo[A] = Free[NetOp, A]
  type Length = Int Refined NonNegative

  def length(v: Int): Option[Length] =
    refineV[NonNegative](v).right.toOption
}
