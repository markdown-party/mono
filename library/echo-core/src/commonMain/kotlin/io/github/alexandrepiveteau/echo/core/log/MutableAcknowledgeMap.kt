package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.buffer.MutableEventIdentifierGapBuffer
import io.github.alexandrepiveteau.echo.core.buffer.binarySearchBySite
import io.github.alexandrepiveteau.echo.core.buffer.toEventIdentifierArray
import io.github.alexandrepiveteau.echo.core.causality.*
import kotlinx.datetime.Clock

/** Returns an [EventIdentifier] that acknowledges the given [SequenceNumber]. */
private fun EventIdentifier.withSequenceNumber(seqno: SequenceNumber): EventIdentifier =
    EventIdentifier(maxOf(this.seqno, seqno), this.site)

/** A [MutableAcknowledgeMap] manages a list set of acknowledgements. */
internal class MutableAcknowledgeMap
private constructor(
    private val clock: Clock,
    private val backing: MutableEventIdentifierGapBuffer,
) {

  /** Creates a new [MutableAcknowledgeMap]. */
  constructor(clock: Clock) : this(clock, MutableEventIdentifierGapBuffer(size = 0))

  /**
   * Acknowledges the given [SequenceNumber] for the provided [SiteIdentifier]. Future calls to the
   * [contains] method with the provided [SequenceNumber] and [SiteIdentifier] pairs will always
   * return `true`.
   */
  fun acknowledge(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ) {
    require(seqno.isSpecified)

    val i = backing.binarySearchBySite(site)
    if (i >= 0) backing[i] = backing[i].withSequenceNumber(seqno)
    else backing.push(EventIdentifier(seqno, site), offset = -(i + 1))
  }

  /**
   * Acknowledges all the identifiers in the provided [EventIdentifierArray], assuming the array is
   * sorted.
   */
  fun acknowledge(ids: EventIdentifierArray) {
    // TODO : Provide a faster path if the array is sorted.
    for (i in 0 until ids.size) {
      acknowledge(ids[i].seqno, ids[i].site)
    }
  }

  /**
   * Sets the given [SequenceNumber] for the provided [SiteIdentifier]. This may decrement the
   * [SequenceNumber] at the provided [SiteIdentifier], meaning that some previous [contains] calls
   * which returned `true` may now return `false`.
   */
  operator fun set(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ) {
    require(seqno.isSpecified)

    val id = EventIdentifier(seqno, site)
    val i = backing.binarySearchBySite(site)
    if (i >= 0) backing[i] = id else backing.push(id, offset = -(i + 1))
  }

  /** Returns true if the given [EventIdentifier] was acknowledged. */
  operator fun contains(value: EventIdentifier): Boolean {
    return contains(value.seqno, value.site)
  }

  /** Returns true iff the [EventIdentifier] with the given [seqno] and [site] was acknowledged. */
  fun contains(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): Boolean {
    require(seqno.isSpecified) { "Sequence number must be specified." }
    val i = backing.binarySearchBySite(site)
    return (i >= 0) && backing[i].seqno >= seqno
  }

  /** Returns the next expected [SequenceNumber] for all the [SiteIdentifier]. */
  fun expected(): SequenceNumber {
    var max = clock.now().toSequenceNumber()
    for (i in 0 until backing.size) {
      max = maxOf(max, backing[i].seqno.inc())
    }
    return maxOf(max, SequenceNumber.Min)
  }

  /** Returns the next expected [SequenceNumber] for the given [SiteIdentifier]. */
  fun expected(site: SiteIdentifier): SequenceNumber {
    val physical = clock.now().toSequenceNumber()
    val logical = get(site).inc() // Because SequenceNumber.Unspecified + 1U == SequenceNumber.Min
    return maxOf(physical, logical)
  }

  /**
   * Returns the last [SequenceNumber] that was acknowledged for the given [SiteIdentifier], or
   * [SequenceNumber.Unspecified] if the [SiteIdentifier] never acknowledged any.
   */
  operator fun get(
      site: SiteIdentifier,
  ): SequenceNumber {
    val i = backing.binarySearchBySite(site)
    return if (i >= 0) backing[i].seqno else SequenceNumber.Unspecified
  }

  /**
   * Returns the sorted [EventIdentifierArray] which contains the event identifiers corresponding to
   * each acknowledged site.
   */
  fun toEventIdentifierArray(): EventIdentifierArray {
    return backing.toEventIdentifierArray()
  }
}
