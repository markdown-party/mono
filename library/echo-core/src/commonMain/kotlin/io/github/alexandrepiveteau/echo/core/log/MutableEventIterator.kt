package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier

/**
 * A mutable variation of [EventIterator], which may be used to mutate an underlying log of events.
 */
interface MutableEventIterator : EventIterator, MutableListIterator<Event> {

  /**
   * Adds an event at the current implicit cursor position, and moves the cursor after the inserted
   * element.
   *
   * @see MutableListIterator.add
   *
   * @param seqno the [SequenceNumber] of the inserted event.
   * @param site the [SiteIdentifier] of the inserted event.
   * @param event the [ByteArray] of the event.
   * @param from the start index of the data.
   * @param until the end index of the data.
   */
  fun add(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: ByteArray,
      from: Int = 0,
      until: Int = event.size,
  )
}
