package io.github.alexandrepiveteau.echo.pn

import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.SyncStrategy
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.mutableHistoryOf
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.projections.OneWayMutableProjection
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import kotlin.math.max
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule

private object FastPNCounterProjection : OneWayProjection<PersistentMap<SiteIdentifier, Int>, Int> {

  override fun forward(
      model: PersistentMap<SiteIdentifier, Int>,
      identifier: EventIdentifier,
      event: Int,
  ): PersistentMap<SiteIdentifier, Int> {
    val current = model.getOrElse(identifier.site) { event }
    return model.put(identifier.site, max(event, current))
  }
}

private object IntBinaryFormat : BinaryFormat {

  override val serializersModule: SerializersModule = SerializersModule {}

  override fun <T> decodeFromByteArray(
      deserializer: DeserializationStrategy<T>,
      bytes: ByteArray,
  ): T {
    check(bytes.size == 4) { "Serialization only supports Int." }
    val a = bytes[0].toInt() and 0xFF
    val b = bytes[1].toInt() and 0xFF
    val c = bytes[2].toInt() and 0xFF
    val d = bytes[3].toInt() and 0xFF
    val result = (a shl 24) or (b shl 16) or (c shl 8) or d
    @Suppress("UNCHECKED_CAST") return result as T
  }

  override fun <T> encodeToByteArray(
      serializer: SerializationStrategy<T>,
      value: T,
  ): ByteArray {
    check(value is Int) { "Serialization only supports Int." }
    return ByteArray(4).apply {
      set(0, ((value shr 24) and 0xFF).toByte())
      set(1, ((value shr 16) and 0xFF).toByte())
      set(2, ((value shr 8) and 0xFF).toByte())
      set(3, (value and 0xFF).toByte())
    }
  }
}

/**
 * A [MutableSite] for a PN-counter, optimized to avoid the overhead of serialization.
 *
 * @param identifier the [SiteIdentifier] for this counter.
 * @param strategy the strategy for this [MutableSite].
 */
fun fastCounter(
    identifier: SiteIdentifier,
    strategy: SyncStrategy = SyncStrategy.Continuous,
): MutableSite<Int, Int> {
  val projection =
      OneWayMutableProjection(FastPNCounterProjection, Int.serializer(), IntBinaryFormat)
  val history = mutableHistoryOf(persistentMapOf(), projection)
  return mutableSite(
      identifier = identifier,
      history = history,
      eventSerializer = Int.serializer(),
      format = IntBinaryFormat,
      strategy = strategy,
      transform = { it.values.sum() },
  )
}
