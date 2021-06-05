package io.github.alexandrepiveteau.echo.core

import io.github.alexandrepiveteau.echo.core.internal.packInts
import io.github.alexandrepiveteau.echo.core.internal.unpackInt1
import io.github.alexandrepiveteau.echo.core.internal.unpackInt2
import kotlin.jvm.JvmInline

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
 * @param packed the packed value for this identifier.
 */
@JvmInline
value class EventIdentifier
internal constructor(
    private val packed: Long,
) {

  init {
    require(site != SiteIdentifier.None) { "Can't use SiteIdentifier.None." }
  }

  // Because we're using packed values and giving precedence to the sequence number, we can simply
  // compare event identifiers as longs to find a total order.
  fun compareTo(other: EventIdentifier) = packed.compareTo(other.packed)

  val seqno: SequenceNumber
    get() = SequenceNumber(unpackInt1(packed).toUInt())

  val site: SiteIdentifier
    get() = SiteIdentifier(unpackInt2(packed))

  operator fun component1(): SequenceNumber = SequenceNumber(unpackInt1(packed).toUInt())
  operator fun component2(): SiteIdentifier = SiteIdentifier(unpackInt2(packed))

  override fun toString(): String = "EventIdentifier(seqno = ${seqno.index}, site = ${site.unique})"
}
