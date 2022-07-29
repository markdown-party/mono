package io.github.alexandrepiveteau.echo.core.log.buffer

import io.github.alexandrepiveteau.echo.core.causality.*
import io.github.alexandrepiveteau.echo.core.log.*
import io.github.alexandrepiveteau.echo.core.requireRange
import kotlinx.datetime.Clock

/**
 * An implementation of [MutableEventLog], which stores events by site as well as in a linear
 * fashion, allowing for faster queries.
 *
 * @param clock the [Clock] used to integrate new events.
 */
internal abstract class AbstractGapBufferMutableEventLog(
    clock: Clock = Clock.System,
) : AbstractMutableEventLog(clock) {

  // Storing the events and the changes.
  private val eventStore = BlockLog()
  private val eventStoreBySite = mutableMapOf<SiteIdentifier, BlockLog>()

  override val size: Int
    get() = eventStore.size

  /**
   * Retrieves the [BlockLog] for the given [SiteIdentifier].
   *
   * @param site the [SiteIdentifier] for which the [BlockLog] is retrieved.
   * @return the [BlockLog] for the given site.
   */
  private fun site(site: SiteIdentifier) = eventStoreBySite.getOrPut(site) { BlockLog() }

  /**
   * Moves the [MutableEventIterator] to the right index for insertion of the given event, assuming
   * all the events are linearly ordered.
   *
   * @see MutableEventIterator.add
   */
  private fun MutableEventIterator.addOrdered(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: ByteArray,
      from: Int,
      until: Int,
  ) {
    val id = EventIdentifier(seqno, site)
    while (hasPrevious() && previousEventIdentifier > id) movePrevious()
    while (hasNext() && nextEventIdentifier < id) moveNext()
    add(seqno, site, event, from, until)
  }

  /**
   * Adds an event to the log, starting from the end of the [EventLog].
   *
   * @receiver the [MutableEventIterator], starting from the end.
   */
  protected open fun MutableEventIterator.addToLog(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: ByteArray,
      from: Int,
      until: Int,
  ) = addOrdered(seqno, site, event, from, until)

  override fun insert(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: ByteArray,
      from: Int,
      until: Int,
  ) {

    // Input sanitization.
    require(seqno.isSpecified)
    requireRange(from, until, event)

    // Don't add existing events.
    if (EventIdentifier(seqno, site) in this) return

    // Acknowledge the new event.
    acknowledge(seqno, site)

    // Adding the event in the iterators.
    eventStore.iteratorAtEnd().addToLog(seqno, site, event, from, until)
    site(site).iteratorAtEnd().addOrdered(seqno, site, event, from, until)
    forEachLogUpdateListener { onInsert(seqno, site, event, from, until) }
  }

  override fun iterator(): EventIterator = eventStore.iterator()

  override fun iteratorAtEnd(): EventIterator = eventStore.iteratorAtEnd()

  override fun iterator(site: SiteIdentifier): EventIterator = site(site).iterator()

  override fun iteratorAtEnd(site: SiteIdentifier): EventIterator = site(site).iteratorAtEnd()

  override fun remove(seqno: SequenceNumber, site: SiteIdentifier): Boolean {

    // Input sanitization.
    require(seqno.isSpecified)

    val id = EventIdentifier(seqno, site)
    var removed = false

    val iterators = setOf(eventStore.iteratorAtEnd(), site(site).iteratorAtEnd())
    for (iterator in iterators) {
      loop@ while (iterator.hasPrevious()) {
        iterator.movePrevious()
        if (iterator.nextEventIdentifier == id) {
          iterator.remove()
          removed = true
          break@loop
        }
      }
    }

    if (removed) forEachLogUpdateListener { onRemoved(seqno, site) }

    return removed
  }

  override fun clear() {
    eventStore.clear()
    eventStoreBySite.clear()
    forEachLogUpdateListener { onCleared() }
  }
}
