package io.github.alexandrepiveteau.echo.core.causality

import io.github.alexandrepiveteau.echo.core.packUInts
import io.github.alexandrepiveteau.echo.core.unpackUInt1
import io.github.alexandrepiveteau.echo.core.unpackUInt2
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * Builds a new [EventIdentifier], for a given [SequenceNumber] and a given [SiteIdentifier].
 *
 * @param seqno the [SequenceNumber] that's used.
 * @param site the [SiteIdentifier] that's used.
 * @return the built [EventIdentifier].
 */
fun EventIdentifier(
    seqno: SequenceNumber,
    site: SiteIdentifier,
): EventIdentifier = EventIdentifier(packUInts(seqno.index, site.unique))

/**
 * An [EventIdentifier] uniquely identifies events and their causality relationships in a
 * distributed system. It offers the properties of a Lamport timestamp, with a unique site
 * identifier for each client to disambiguate duplicate timestamps.
 *
 * @param packed the packed value for this identifier.
 */
@Serializable(with = EventIdentifierSerializer::class)
@JvmInline
value class EventIdentifier
internal constructor(
    internal val packed: ULong,
) {

  // Because we're using packed values and giving precedence to the sequence number, we can simply
  // compare event identifiers as longs to find a total order.
  operator fun compareTo(other: EventIdentifier) = packed.compareTo(other.packed)

  val seqno: SequenceNumber
    get() = SequenceNumber(unpackUInt1(packed))

  val site: SiteIdentifier
    get() = SiteIdentifier(unpackUInt2(packed))

  operator fun component1(): SequenceNumber = SequenceNumber(unpackUInt1(packed))
  operator fun component2(): SiteIdentifier = SiteIdentifier(unpackUInt2(packed))

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
