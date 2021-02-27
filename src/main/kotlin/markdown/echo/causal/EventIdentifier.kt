@file:Suppress("NOTHING_TO_INLINE")

package markdown.echo.causal

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
): EventIdentifier = EventIdentifier(packInts(seqno.index, site.unique))

/**
 * An [EventIdentifier] uniquely identifies events and their causality relationships in a
 * distributed system. It offers the properties of a Lamport timestamp, with a unique site
 * identifier for each client to disambiguate duplicate timestamps.
 *
 * @param packed the packed value for this identifier. Prefer building an instance with the
 *               dedicated builder function.
 */
inline class EventIdentifier internal constructor(
    private val packed: Long,
) : Comparable<EventIdentifier> {

    // Because we're using packed values and giving precedence to the sequence number, we can simply
    // compare event identifiers as longs to find a total order.
    override fun compareTo(other: EventIdentifier) = packed.compareTo(other.packed)

    val seqno: SequenceNumber get() = SequenceNumber(unpackInt1(packed))
    val site: SiteIdentifier get() = SiteIdentifier(unpackInt2(packed))

    operator fun component1(): SequenceNumber = SequenceNumber(unpackInt1(packed))
    operator fun component2(): SiteIdentifier = SiteIdentifier(unpackInt2(packed))
}

// PACKING UTILITIES

/**
 * Packs two Int values into one Long value for use in inline classes.
 */
private inline fun packInts(val1: Int, val2: Int): Long {
    return val1.toLong().shl(32) or (val2.toLong() and 0xFFFFFFFF)
}

/**
 * Unpacks the first Int value in [packInts] from its returned ULong.
 */
private inline fun unpackInt1(value: Long): Int {
    return value.shr(32).toInt()
}

/**
 * Unpacks the second Int value in [packInts] from its returned ULong.
 */
private inline fun unpackInt2(value: Long): Int {
    return value.and(0xFFFFFFFF).toInt()
}
