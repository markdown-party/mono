@file:Suppress("NOTHING_TO_INLINE")

package markdown.echo.causal

import markdown.echo.util.packInts
import markdown.echo.util.unpackInt1
import markdown.echo.util.unpackInt2

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
): EventIdentifier = EventIdentifier(packInts(seqno.index.toInt(), site.unique))

/**
 * An [EventIdentifier] uniquely identifies events and their causality relationships in a
 * distributed system. It offers the properties of a Lamport timestamp, with a unique site
 * identifier for each client to disambiguate duplicate timestamps.
 *
 * @param packed the packed value for this identifier. Prefer building an instance with the
 * ```
 *               dedicated builder function.
 * ```
 */
inline class EventIdentifier
internal constructor(
    private val packed: Long,
) : Comparable<EventIdentifier> {

  // Because we're using packed values and giving precedence to the sequence number, we can simply
  // compare event identifiers as longs to find a total order.
  override fun compareTo(other: EventIdentifier) = packed.compareTo(other.packed)

  val seqno: SequenceNumber
    get() = SequenceNumber(unpackInt1(packed).toUInt())
  val site: SiteIdentifier
    get() = SiteIdentifier(unpackInt2(packed))

  operator fun component1(): SequenceNumber = SequenceNumber(unpackInt1(packed).toUInt())
  operator fun component2(): SiteIdentifier = SiteIdentifier(unpackInt2(packed))
}
