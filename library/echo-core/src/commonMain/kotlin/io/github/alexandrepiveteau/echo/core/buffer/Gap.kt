package io.github.alexandrepiveteau.echo.core.buffer

/** An interface representing the [Gap] from a gap buffer. */
interface Gap {

  /** The start index of the gap (inclusive). */
  val startIndex: Int

  /** The end index of the gap (exclusive). */
  val endIndex: Int

  /**
   * Shifts the start and end indices of the gap by the provided amount, keeping it in the
   * boundaries of the gap buffer.
   *
   * This operation may move the gap.
   */
  fun shift(amount: Int)

  companion object {

    /** The default size at which gap buffers will be created. */
    const val DefaultSize = 32
  }
}
