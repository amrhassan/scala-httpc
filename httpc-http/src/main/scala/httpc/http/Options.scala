package httpc.http

case class Options(maxResponseBodySize: Int)

object Options {
  val default = Options(
    maxResponseBodySize =  1024 * 1024 * 10   // 10 MB, too much? too little? Time will tell.
  )
}
