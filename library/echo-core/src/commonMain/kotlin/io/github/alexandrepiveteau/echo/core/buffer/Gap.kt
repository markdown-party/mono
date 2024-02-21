package io.github.alexandrepiveteau.echo.core.buffer

/** An interface representing the [Gap] from a gap buffer. */
public interface Gap {

  /** The start index of the gap (inclusive). */
  public val startIndex: Int

  /** The end index of the gap (exclusive). */
  public val endIndex: Int

  /**
   * Shifts the start and end indices of the gap by the provided amount, keeping it in the
   * boundaries of the gap buffer.
   *
   * This operation may move the gap.
   */
  public fun shift(amount: Int)

  public companion object {

    /** The default size at which gap buffers will be created. */
    public const val DefaultSize: Int = 32
  }
}
