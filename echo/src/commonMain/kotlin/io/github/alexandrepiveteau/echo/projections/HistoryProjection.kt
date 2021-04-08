@file:Suppress("FunctionName")

package io.github.alexandrepiveteau.echo.projections

import io.github.alexandrepiveteau.echo.logs.EventValue
import io.github.alexandrepiveteau.echo.projections.HistoryProjection.History
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * Creates a new [HistoryProjection] for a [OneWayProjection]. This assumes that the underlying type
 * [M] is immutable, and that delta updates are efficiently stored and computed, such that keeping
 * the history of all the changes is not too costly.
 *
 * @param projection the underlying [OneWayProjection].
 *
 * @param M the type of the model.
 * @param T the type of the events.
 */
internal fun <M, T> HistoryProjection(
    projection: OneWayProjection<M, EventValue<T>>
): HistoryProjection<M, T, M> =
    HistoryProjection(
        object : TwoWayProjection<M, EventValue<T>, M> {
          override fun forward(
              body: EventValue<T>,
              model: M,
          ): Step<M, M> {
            return Step(
                data = projection.forward(body, model),
                change = model,
            )
          }
          override fun backward(change: M, model: M): M {
            return change
          }
        })

/**
 * Creates a new [HistoryProjection] for a [TwoWayProjection]. This assumes that the underlying
 * types [M] and [C] are immutable. The [TwoWayProjection] can then be traversed bidirectionally to
 * replay the events properly, as needed.
 *
 * @param projection the underlying [OneWayProjection].
 *
 * @param M the type of the model.
 * @param T the type of the events.
 * @param C the type of the changes.
 */
internal class HistoryProjection<M, T, C>(
    private val projection: TwoWayProjection<M, EventValue<T>, C>,
) : OneWayProjection<History<M, T, C>, EventValue<T>> {

  /**
   * A step that was executed in the log. Generally, it has been ordered totally, and may be
   * reversed by applying the [change] to the model.
   *
   * @param T the type of the events.
   * @param C the type of the changes.
   */
  internal data class LogStep<out T, out C>(
      val event: EventValue<T>,
      val change: C,
  )

  /**
   * The state of a [HistoryProjection]. This contains the [current] model, as well as a list of
   * [past] steps and changes to the model.
   *
   * @param M the type of the model.
   */
  internal data class History<M, T, C>(
      val current: M,
      val past: PersistentList<LogStep<T, C>> = persistentListOf(),
  ) {

    /**
     * Inserts an [EventValue] into the [History], recomputing the [past] and [current] values as
     * needed.
     *
     * @param event the inserted [EventValue].
     * @param projection the [TwoWayProjection] used.
     */
    fun insert(
        event: EventValue<T>,
        projection: TwoWayProjection<M, EventValue<T>, C>,
    ): History<M, T, C> {

      /**
       * Rewinds the history of operations, splitting the log in segments that come before and after
       * the [event].
       *
       * @param current the start model.
       * @param past the [LogStep] that have already been performed.
       * @param future the [EventValue] that will have to be replayed.
       *
       * @return a [Triple] with the split history. The future log is reversed and should be
       * consumed from the end.
       */
      tailrec fun rewind(
          past: PersistentList<LogStep<T, C>>,
          current: M,
          future: PersistentList<EventValue<T>> = persistentListOf(),
      ): Triple<PersistentList<LogStep<T, C>>, M, PersistentList<EventValue<T>>> =
          when (val last = past.lastOrNull()) {
            null -> Triple(past, current, future)
            else ->
                if (last.event.identifier < event.identifier) {
                  Triple(past, current, future)
                } else {
                  rewind(
                      past.removeAt(past.lastIndex),
                      projection.backward(last.change, current),
                      future.add(last.event),
                  )
                }
          }

      /**
       * Replays a [PersistentList] of [future] events (which come in a reversed order), aggregating
       * additional events to the [past] history and updating the [current] model stepwise.
       *
       * @param current the starting model.
       * @param past the past history.
       * @param future the remaining events to process. The first event to process should come last.
       *
       * @return a [History] of the final model and changes.
       */
      tailrec fun replay(
          past: PersistentList<LogStep<T, C>>,
          current: M,
          future: PersistentList<EventValue<T>>,
      ): History<M, T, C> =
          when (val step = future.lastOrNull()) {
            null -> History(current, past)
            else -> {
              val (model, change) = projection.forward(step, current)
              replay(past.add(LogStep(step, change)), model, future.removeAt(future.lastIndex))
            }
          }

      val (past, present, future) = rewind(past, current)
      val (next, change) = projection.forward(event, present)

      return replay(past.add(LogStep(event, change)), next, future)
    }
  }

  override fun forward(
      body: EventValue<T>,
      model: History<M, T, C>,
  ): History<M, T, C> {
    return model.insert(body, projection)
  }
}
