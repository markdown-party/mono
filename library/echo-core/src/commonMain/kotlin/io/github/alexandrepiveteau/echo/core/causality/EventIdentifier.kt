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
public fun EventIdentifier(
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
public value class EventIdentifier
internal constructor(
    internal val packed: ULong,
) : Comparable<EventIdentifier> {

  // Because we're using packed values and giving precedence to the sequence number, we can simply
  // compare event identifiers as longs to find a total order.
  override operator fun compareTo(other: EventIdentifier): Int = packed.compareTo(other.packed)

  public val seqno: SequenceNumber
    get() = SequenceNumber(unpackUInt1(packed))

  public val site: SiteIdentifier
    get() = SiteIdentifier(unpackUInt2(packed))

  public operator fun component1(): SequenceNumber = SequenceNumber(unpackUInt1(packed))
  public operator fun component2(): SiteIdentifier = SiteIdentifier(unpackUInt2(packed))

  override fun toString(): String = "EventIdentifier(seqno = ${seqno}, site = ${site.unique})"

  public companion object {

    /** A special sentinel value that indicates that no [EventIdentifier] is set. */
    public val Unspecified: EventIdentifier =
        EventIdentifier(
            SequenceNumber.Unspecified,
            SiteIdentifier.Min,
        )
  }
}

/** `false` when this has [SiteIdentifier.Unspecified] or [SequenceNumber.Unspecified]. */
public inline val EventIdentifier.isSpecified: Boolean
  get() {
    // Avoid auto-boxing.
    return seqno.isSpecified
  }

/** `false` when this has [SiteIdentifier.Unspecified] or [SequenceNumber.Unspecified]. */
public inline val EventIdentifier.isUnspecified: Boolean
  get() {
    // Avoid auto-boxing.
    return seqno.isUnspecified
  }

/** Creates an [EventIdentifier] from a [ULong]. */
public fun ULong.toEventIdentifier(): EventIdentifier = EventIdentifier(this)

/** Creates an [ULong] from an [EventIdentifier]. */
public fun EventIdentifier.toULong(): ULong = packed
