package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A class representing an [Event]. This will typically be used when a function should return a
 * complete event, as well as its associated identifier.
 *
 * You should prefer using some allocation-free functions for high-performance code.
 *
 * @param seqno the [SequenceNumber] for this [Event].
 * @param site the [SiteIdentifier] for this [Event].
 * @param data the [ByteArray] that contains the [Event] body.
 */
@Serializable
@SerialName("evt")
public data class Event(
    val seqno: SequenceNumber,
    val site: SiteIdentifier,
    val data: ByteArray,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || other !is Event) return false

    if (seqno != other.seqno) return false
    if (site != other.site) return false
    if (!data.contentEquals(other.data)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = seqno.hashCode()
    result = 31 * result + site.hashCode()
    result = 31 * result + data.contentHashCode()
    return result
  }
}
