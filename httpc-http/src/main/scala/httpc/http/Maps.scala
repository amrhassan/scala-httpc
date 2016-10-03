package httpc.http

object Maps {

  /** Constructs a map from the given sequence using a function to determine the map keys and original sequence
    * entries as values
    */
  def fromTraversable[A, B](bs: Traversable[B], key: B => A): Map[A, B] =
    bs.map(b => (key(b), b)).toMap
}
