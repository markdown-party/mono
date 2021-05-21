package io.github.alexandrepiveteau.echo.logs

import kotlin.jvm.JvmInline

/**
 * A discriminated union of changes, usually associated with events. It encapsulates a present or
 * absent change, which contains a delta over a previous value in an event log.
 *
 * Actual changes represent a value of type [T], which contains the delta to rewind the operation.
 * Skipped (or empty) changes represent an absence of delta to apply.
 */
@JvmInline
value class Change<out T> internal constructor(private val holder: Any?) {

  /** Returns true if the [Change] can be skipped. */
  val isSkipped: Boolean
    get() = holder === skipped

  /** Returns true if the [Change] could be rewind. */
  val isDelta: Boolean
    get() = holder !== skipped

  /** Returns the delta of this [Change], or null if it is skipped. */
  @Suppress("UNCHECKED_CAST") fun deltaOrNull(): T? = if (isDelta) holder as T else null

  private class Skipped {
    override fun toString() = "Skipped"
  }

  companion object {

    // A sentinel value for skipped, allowing for nullable T (so that T : Any? safely) in Change<T>.
    private val skipped = Skipped()

    /** Returns a [Change] which which can be reversed with the [value] provided as delta. */
    fun <T> delta(value: T): Change<T> = Change(value)

    /** Returns a [Change] which can not be reversed, and which will be ignored. */
    fun <T> skipped(): Change<T> = Change(skipped)
  }
}
