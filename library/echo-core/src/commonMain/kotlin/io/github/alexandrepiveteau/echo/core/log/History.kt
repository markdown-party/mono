package io.github.alexandrepiveteau.echo.core.log

/**
 * A [History] is a high-performance log of serialized events, which aggregate the events into a
 * [current] value which is incrementally computed from the linear sequence of events.
 *
 * @param T the type of the aggregate.
 */
interface History<out T> : EventLog {

  /** The [current] value of the aggregated [History]. */
  val current: T

  /**
   * A listener which may be used to observe some changes on the current value.
   *
   * @param T the type of the aggregate.
   */
  fun interface OnValueUpdateListener<in T> {

    /**
     * A callback which will be called whenever an update is performed on the [current] value.
     *
     * @param value the new value which was set.
     */
    fun onValueUpdated(value: T)
  }

  /** Registers the provided [OnValueUpdateListener] to this [History]. */
  fun registerValueUpdateListener(listener: OnValueUpdateListener<T>)

  /** Unregisters the provided [OnValueUpdateListener] to this [History]. */
  fun unregisterValueUpdateListener(listener: OnValueUpdateListener<T>)
}
