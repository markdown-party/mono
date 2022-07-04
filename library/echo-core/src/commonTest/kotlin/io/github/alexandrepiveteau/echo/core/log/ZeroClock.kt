package io.github.alexandrepiveteau.echo.core.log

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** An instance of [Clock], used in testing, that always returns the zero [Instant]. */
internal object ZeroClock : Clock {
  override fun now() = Instant.fromEpochSeconds(0)
}
