package httpc.net

import java.util.concurrent.locks.ReadWriteLock

object Locks {

  def syncRead[A](lock: ReadWriteLock)(a: => A): A =
    try {
      lock.readLock().lock()
      a
    } finally {
      lock.readLock().unlock()
    }

  def syncWrite[A](lock: ReadWriteLock)(a: => A): A =
    try {
      lock.writeLock().lock()
      a
    } finally {
      lock.writeLock().unlock()
    }
}
