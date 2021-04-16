package io.github.alexandrepiveteau.echo.internal.history

import io.github.alexandrepiveteau.echo.EchoEventLogPreview
import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.logs.Change
import io.github.alexandrepiveteau.echo.logs.EventLog.IndexedEvent
import io.github.alexandrepiveteau.echo.logs.PersistentEventLog
import io.github.alexandrepiveteau.echo.logs.persistentEventLogOf
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import io.github.alexandrepiveteau.echo.projections.Step
import io.github.alexandrepiveteau.echo.projections.TwoWayProjection
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

internal fun <T, M> ActualPersistentHistory(
    initial: M,
    log: PersistentEventLog<T, M> = persistentEventLogOf(),
    projection: OneWayProjection<M, IndexedEvent<T>>,
): ActualPersistentHistory<T, M, M> =
    ActualPersistentHistory(
        initial = initial,
        log = log,
        projection =
            object : TwoWayProjection<M, IndexedEvent<T>, M> {

              override fun forward(
                  body: IndexedEvent<T>,
                  model: M,
              ): Step<M, M> =
                  Step(
                      data = projection.forward(body, model),
                      change = model,
                  )

              override fun backward(change: M, model: M): M = change
            },
    )

internal data class ActualPersistentHistory<T, M, C>
constructor(
    override val current: HistoryModel<T, M, C>,
    private val projection: TwoWayProjection<M, IndexedEvent<T>, C>,
) : PersistentLogHistory<T, M, C> {

  /**
   * Creates a new [PersistentLogHistory], with the provided [initial] model and the given
   * [projection].
   *
   * @param initial the model that acts as the starting point for the history.
   * @param log the underlying [PersistentEventLog].
   * @param projection the [TwoWayProjection] that can be used to move forward or backwards.
   */
  constructor(
      initial: M,
      log: PersistentEventLog<T, C> = persistentEventLogOf(),
      projection: TwoWayProjection<M, IndexedEvent<T>, C>,
  ) : this(
      HistoryModel(log, initial),
      projection,
  )

  @OptIn(EchoEventLogPreview::class)
  override fun forward(
      event: HistoryEvent<T>,
  ): Pair<PersistentLogHistory<T, M, C>, EventIdentifier> {

    /**
     * Rewinds the history of operations, splitting the log in segments that come before and after
     * the [event].
     *
     * @param current the start model.
     * @param past the remaining event log, with already performed steps.
     * @param future the events [T] that will have to be replayed.
     *
     * @return a [Triple] with the split history. The future log is reversed and should be consumed
     * from the end.
     */
    tailrec fun rewind(
        past: PersistentEventLog<T, C>,
        current: M,
        future: PersistentList<IndexedEvent<T>> = persistentListOf(),
    ): Triple<PersistentEventLog<T, C>, M, PersistentList<IndexedEvent<T>>> =
        when (val last = past.lastOrNull()) {
          null -> Triple(past, current, future)
          else ->
              if (last.identifier < EventIdentifier(event.seqno, event.site)) {
                Triple(past, current, future)
              } else {
                val updated =
                    when (val delta = last.change.deltaOrNull()) {
                      null -> current
                      else -> projection.backward(delta, current)
                    }
                rewind(
                    past.remove(last.identifier.site, last.identifier.seqno),
                    updated,
                    future.add(last),
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
     * @return an [ActualPersistentHistory] of the final model and changes.
     */
    tailrec fun replay(
        past: PersistentEventLog<T, C>,
        current: M,
        future: PersistentList<IndexedEvent<T>>,
    ): ActualPersistentHistory<T, M, C> =
        when (val step = future.lastOrNull()) {
          null ->
              ActualPersistentHistory(
                  current = HistoryModel(past, current),
                  projection = projection,
              )
          else -> {
            val (model, change) = projection.forward(step, current)
            replay(
                past =
                    past.set(
                        site = step.identifier.site,
                        seqno = step.identifier.seqno,
                        body = step.body,
                        change = Change.delta(change),
                    ),
                current = model,
                future = future.removeAt(future.lastIndex))
          }
        }

    // If an operation has already been inserted in the operation log, make sure that we don't rerun
    // the projection and don't try to insert it in the log.
    if (current.log.contains(event.site, event.seqno)) return this to event.identifier

    val (past, present, future) = rewind(current.log, current.model)
    val (next, change) = projection.forward(event, present)

    return replay(
        past =
            past.set(
                site = event.site,
                seqno = event.seqno,
                body = event.body,
                change = Change.delta(change),
            ),
        current = next,
        future = future,
    ) to event.identifier
  }
}
