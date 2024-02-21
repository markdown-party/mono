package io.github.alexandrepiveteau.echo.core.log

/**
 * An interface describing a [ChangeScope]. A [ChangeScope] defines a scope that lets users push
 * some new changes, which will then be stored in a log and reverted as needed.
 *
 * Usually, one should not [push] more than once for each event that generated a change;
 * nevertheless, depending on the granularity of the change operations, calling [push] for multiple
 * changes may be appropriate.
 */
public fun interface ChangeScope {

  /** Pushes a section of a [ByteArray] as a change, with boundaries between [from] and [until]. */
  public fun push(array: ByteArray, from: Int, until: Int)

  /** Pushes a single [Byte] as a change. */
  public fun push(byte: Byte) {
    push(byteArrayOf(byte))
  }

  /** Pushes the whole [ByteArray] as a change. */
  public fun push(array: ByteArray) {
    push(array, from = 0, until = array.size)
  }
}
