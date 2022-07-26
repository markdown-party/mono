package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.History.OnValueUpdateListener
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

  /** The [OnValueUpdateListener]s which should be notified when the value is updated. */
  private val listeners = mutableSetOf<OnValueUpdateListener<T>>()

  private inline fun MutableEventIterator.withScope(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      block: ChangeScope.() -> Unit
  ) = block(ChangeScope { array, from, until -> add(seqno, site, array, from, until) })

  override fun MutableEventIterator.addToLog(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: ByteArray,
      from: Int,
      until: Int,
  ) {
    val id = EventIdentifier(seqno, site)
    val changeIterator = changeStore.iteratorAtEnd()
    while (hasPrevious() && previousEventIdentifier > id) {
      movePrevious()
      while (changeIterator.hasPrevious() &&
          changeIterator.previousEventIdentifier == nextEventIdentifier) {
        changeIterator.movePrevious()
        current =
            projection.backward(
                model = current,
                identifier = nextEventIdentifier,
                data = nextEvent,
                from = nextFrom,
                until = nextUntil,
                changeData = changeIterator.nextEvent,
                changeFrom = changeIterator.nextFrom,
                changeUntil = changeIterator.nextUntil,
            )
        changeIterator.remove()
      }
    }
    add(seqno, site, event, from, until)
    movePrevious()
    while (hasNext()) {
      moveNext()
      changeIterator.withScope(previousSeqno, previousSite) {
        with(projection) {
          current =
              forward(
                  model = current,
                  identifier = previousEventIdentifier,
                  data = previousEvent,
                  from = previousFrom,
                  until = previousUntil,
              )
        }
      }
    }
  }

  private val changeStore = BlockLog()

  override fun clear() {
    changeStore.clear()
    super.clear()
  }

  final override var current: T = initial
    private set(value) {
      field = value
      notifyValueListeners(value)
    }

  /** Notifies all the [OnValueUpdateListener]s that a new value is available. */
  private fun notifyValueListeners(value: T) {
    listeners.toSet().forEach { it.onValueUpdated(value) }
  }

  override fun registerValueUpdateListener(listener: OnValueUpdateListener<T>) {
    listeners += listener
    listener.onValueUpdated(current)
  }

  override fun unregisterValueUpdateListener(listener: OnValueUpdateListener<T>) {
    listeners -= listener
  }
}
