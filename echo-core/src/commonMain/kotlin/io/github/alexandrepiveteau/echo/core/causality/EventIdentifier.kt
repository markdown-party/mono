package io.github.alexandrepiveteau.echo.core.causality

import io.github.alexandrepiveteau.echo.core.packInts
import io.github.alexandrepiveteau.echo.core.unpackInt1
import io.github.alexandrepiveteau.echo.core.unpackInt2
import kotlin.jvm.JvmInline

/**
 * Builds a new [EventIdentifier], for a given [SequenceNumber] and a given [SiteIdentifier].
 *
 * @param seqno the [SequenceNumber] that's used.
 * @param site the [SiteIdentifier] that's used.
 * @return the built [EventIdentifier].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun EventIdentifier(
    seqno: SequenceNumber,
    site: SiteIdentifier,
): EventIdentifier = EventIdentifier(packInts(seqno.index.toInt(), site.unique.toInt()))

/**
 * An [EventIdentifier] uniquely identifies events and their causality relationships in a
 * distributed system. It offers the properties of a Lamport timestamp, with a unique site
 * identifier for each client to disambiguate duplicate timestamps.
 *
 * @param packed the packed value for this identifier.
 */
@JvmInline
value class EventIdentifier
@PublishedApi
internal constructor(
    internal val packed: Long,
) {

  // Because we're using packed values and giving precedence to the sequence number, we can simply
  // compare event identifiers as longs to find a total order.
  operator fun compareTo(other: EventIdentifier) = packed.compareTo(other.packed)

  val seqno: SequenceNumber
    get() = SequenceNumber(unpackInt1(packed).toUInt())

  val site: SiteIdentifier
    get() = SiteIdentifier(unpackInt2(packed).toUInt())

  operator fun component1(): SequenceNumber = SequenceNumber(unpackInt1(packed).toUInt())
  operator fun component2(): SiteIdentifier = SiteIdentifier(unpackInt2(packed).toUInt())

  override fun toString(): String = "EventIdentifier(seqno = ${seqno.index}, site = ${site.unique})"

  companion object {

    /** A special sentinel value that indicates that no [EventIdentifier] is set. */
    val Unspecified: EventIdentifier =
        EventIdentifier(
            SequenceNumber.Unspecified,
            SiteIdentifier.Unspecified,
        )
  }
}

/** `false` when this has [SiteIdentifier.Unspecified] or [SequenceNumber.Unspecified]. */
inline val EventIdentifier.isSpecified: Boolean
  get() {
    // Avoid auto-boxing.
    val seqnoSpecified = seqno.index != SequenceNumber.Unspecified.index
    val siteSpecified = site.unique != SiteIdentifier.Unspecified.unique
    return seqnoSpecified && siteSpecified
  }

/** `false` when this has [SiteIdentifier.Unspecified] or [SequenceNumber.Unspecified]. */
inline val EventIdentifier.isUnspecified: Boolean
  get() {
    // Avoid auto-boxing.
    val seqnoSpecified = seqno.index == SequenceNumber.Unspecified.index
    val siteSpecified = site.unique == SiteIdentifier.Unspecified.unique
    return seqnoSpecified || siteSpecified
  }

/**
 * If this [EventIdentifier] [isSpecified] then this is returned, otherwise [block] is executed and
 * its result is returned.
 */
inline fun EventIdentifier.takeOrElse(block: () -> EventIdentifier): EventIdentifier =
    if (isSpecified) this else block()
