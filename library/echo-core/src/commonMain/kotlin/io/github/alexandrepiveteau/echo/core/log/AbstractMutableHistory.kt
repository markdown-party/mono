package io.github.alexandrepiveteau.echo.core.log

import kotlinx.datetime.Clock

/**
 * An implementation of a [MutableHistory], with an initial aggregate, and a projection that is used
 * to incrementally update the model.
 *
 * @param clock the [Clock] used to integrate new events.
 */
abstract class AbstractMutableHistory<T>(
    initial: T,
    private val projection: MutableProjection<T>,
    clock: Clock = Clock.System,
) : AbstractMutableEventLog(clock), MutableHistory<T> {

  // The ChangeScope that will be provided to the projection whenever some changes mush be appended
  // to the history of changes.
  private val scope = ChangeScope { array, from, until ->
    changeStore.pushAtGap(
        id = eventStore.lastId,
        array = array,
        from = from,
        until = until,
    )
  }

  private val changeStore = BlockLog()

  override fun moveLeft() {
    // Remove all the associated changes.
    reverseChange@ while (changeStore.hasPrevious) {

      // val changeId = changesIds[changesIds.gap.startIndex - 1]
      if (changeStore.lastId != eventStore.lastId) break@reverseChange

      // Update the current projection.
      current =
          projection.backward(
              model = current,
              identifier = eventStore.lastId,
              data = eventStore.backing,
              from = eventStore.lastFrom,
              until = eventStore.lastUntil,
              changeData = changeStore.backing,
              changeFrom = changeStore.lastFrom,
              changeUntil = changeStore.lastUntil,
          )

      // Only remove the change once it has been used to update the projection.
      changeStore.removeLeft()
    }

    // Move the event log.
    super.moveLeft()
  }

  override fun moveRight() {
    // Move the event log.
    super.moveRight()

    // Update the current value.
    current =
        with(projection) {
          scope.forward(
              model = current,
              identifier = eventStore.lastId,
              data = eventStore.backing,
              from = eventStore.lastFrom,
              until = eventStore.lastUntil,
          )
        }
  }

  override fun clear() {
    super.clear()
    changeStore.clear()
  }

  final override var current: T = initial
    private set
}