package io.github.alexandrepiveteau.echo.core.log.tree

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.isSpecified
import io.github.alexandrepiveteau.echo.core.log.*
import io.github.alexandrepiveteau.echo.core.requireRange
import kotlinx.datetime.Clock

/**
 * An implementation of [MutableEventLog] which stores events by site and in a balanced tree.
 *
 * @param clock the [Clock] used to integrate new events.
 */
internal abstract class AbstractAVLTreeMutableEventLog(
    clock: Clock = Clock.System,
) : AbstractMutableEventLog(clock) {

  private var tree = PersistentAVLTree<EventIdentifier, ByteArray>()
  private val treeBySite =
      mutableMapOf<SiteIdentifier, PersistentAVLTree<EventIdentifier, ByteArray>>()

  override val size: Int
    get() = tree.size

  /**
   * Retrieves the [PersistentAVLTree] for the given [SiteIdentifier].
   *
   * @param site the [SiteIdentifier] for which the [PersistentAVLTree] is retrieved.
   * @return the [PersistentAVLTree] for the given site.
   */
  private fun site(site: SiteIdentifier) = treeBySite.getOrPut(site) { PersistentAVLTree() }

  override fun insert(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: ByteArray,
      from: Int,
      until: Int
  ) {
    // Input sanitization.
    require(seqno.isSpecified)
    requireRange(from, until, event)

    // Don't add existing events.
    if (EventIdentifier(seqno, site) in this) return

    // Acknowledge the events.
    acknowledge(seqno, site)

    // Add the events in the data structures.
    val bytes = event.copyOfRange(from, until)
    tree = tree.set(EventIdentifier(seqno, site), bytes)
    treeBySite[site] = site(site).set(EventIdentifier(seqno, site), bytes)
    forEachLogUpdateListener { onInsert(seqno, site, event, from, until) }
  }

  override fun iterator(): EventIterator = ListIteratorDecorator(tree.iterator())

  override fun iteratorAtEnd(): EventIterator = ListIteratorDecorator(tree.iteratorAtEnd())

  override fun iterator(site: SiteIdentifier): EventIterator =
      ListIteratorDecorator(site(site).iterator())

  override fun iteratorAtEnd(site: SiteIdentifier): EventIterator =
      ListIteratorDecorator(site(site).iteratorAtEnd())

  override fun remove(seqno: SequenceNumber, site: SiteIdentifier): Boolean {
    val present = tree.contains(EventIdentifier(seqno, site))
    if (present) tree = tree.minus(EventIdentifier(seqno, site))
    if (present) forEachLogUpdateListener { onRemoved(seqno, site) }
    return present
  }

  override fun clear() {
    tree = PersistentAVLTree()
    treeBySite.clear()
    forEachLogUpdateListener { onCleared() }
  }
}
