package io.github.alexandrepiveteau.echo.lww

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.MutableHistory
import io.github.alexandrepiveteau.echo.core.log.mutableHistoryOf
import io.github.alexandrepiveteau.echo.lww.LWWRegister.Tagged
import io.github.alexandrepiveteau.echo.projections.OneWayMutableProjection
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer

/**
 * Returns a [MutableHistory] which handles [Tagged] values of type [T].
 *
 * @param initial the initial value in the history.
 */
@Suppress("FunctionName")
inline fun <reified T> LWWMutableHistory(initial: T): MutableHistory<Tagged<T>> {
  val initialIdentifier = EventIdentifier(SequenceNumber.Unspecified, SiteIdentifier.Min)
  return mutableHistoryOf(
          initial = Tagged(initial, initialIdentifier),
          projection = OneWayMutableProjection(LWWRegister(), serializer(), ProtoBuf),
      )
      .apply { registerLogUpdateListener(LWWOnLogUpdateListener(this)) }
}
