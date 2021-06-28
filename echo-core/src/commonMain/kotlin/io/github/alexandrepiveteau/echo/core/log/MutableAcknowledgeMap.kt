package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.buffer.MutableEventIdentifierGapBuffer
import io.github.alexandrepiveteau.echo.core.buffer.toEventIdentifierArray
import io.github.alexandrepiveteau.echo.core.causality.*
import kotlin.jvm.JvmInline

/** A [MutableAcknowledgeMap] manages a list set of acknowledgements. */
// TODO : Use a binary tree internally, or a sorted data structure.
@JvmInline
internal value class MutableAcknowledgeMap
private constructor(
    private val backing: MutableEventIdentifierGapBuffer,
) {

  /** Creates a new [MutableAcknowledgeMap]. */
  constructor() : this(MutableEventIdentifierGapBuffer(size = 0))

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
    require(site.isSpecified)

    // 1. Try to update an existing site identifier.
    for (i in 0 until backing.size) {
      val (exSeqno, exSite) = backing[i]
      if (exSite == site) {
        backing[i] = EventIdentifier(maxOf(exSeqno, seqno), site)
        return
      }
    }

    // 2. Or insert the new sequence identifier.
    backing.push(EventIdentifier(seqno, site))
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
    require(site.isSpecified)

    // 1. Try to update an existing site identifier.
    for (i in 0 until backing.size) {
      val (_, exSite) = backing[i]
      if (exSite == site) {
        backing[i] = EventIdentifier(seqno, site)
        return
      }
    }

    // 2. Or insert the new sequence number.
    backing.push(EventIdentifier(seqno, site))
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
    require(site.isSpecified) { "Site must be specified." }
    require(seqno.isSpecified) { "Sequence number must be specified." }
    for (i in 0 until backing.size) {
      if (backing[i].site == site) return backing[i].seqno >= seqno
      // TODO (if sorted) : if (backing[i].site > site) return false
    }
    return false
  }

  //fun contains()

  /** Returns the next expected [SequenceNumber] for all the [SiteIdentifier]. */
  fun expected(): SequenceNumber {
    var max = SequenceNumber.Min
    for (i in 0 until backing.size) {
      max = maxOf(max, backing[i].seqno.inc())
    }
    return max
  }

  /** Returns the next expected [SequenceNumber] for the given [SiteIdentifier]. */
  fun expected(site: SiteIdentifier): SequenceNumber {
    return get(site).inc() // Because SequenceNumber.Unspecified + 1 == SequenceNumber.Min
  }

  /**
   * Returns the last [SequenceNumber] that was acknowledged for the given [SiteIdentifier], or
   * [SequenceNumber.Unspecified] if the [SiteIdentifier] never acknowledged any.
   */
  operator fun get(
      site: SiteIdentifier,
  ): SequenceNumber {
    require(site.isSpecified) { "Site must be specified." }
    for (i in 0 until backing.size) {
      if (backing[i].site == site) return backing[i].seqno
      // TODO (if sorted) : if (backing[i].site > site) return SequenceNumber.Unspecified
    }
    return SequenceNumber.Unspecified
  }

  /**
   * Returns the sorted [EventIdentifierArray] which contains the event identifiers corresponding to
   * each acknowledged site.
   */
  fun toEventIdentifierArray(): EventIdentifierArray {
    return backing.toEventIdentifierArray().apply { sort() }
  }
}
