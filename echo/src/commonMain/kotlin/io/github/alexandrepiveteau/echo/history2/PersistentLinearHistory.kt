package io.github.alexandrepiveteau.echo.history2

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier

interface PersistentLinearHistory<out M, out T, out C> : PersistentHistory<M> {

  fun push(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: @UnsafeVariance T,
  ): PersistentLinearHistory<M, T, C>
}

// TODO : Use Replay for all the changes that have to be inserted, based on a diff with the history.
@ExperimentalHistoryApi
fun interface Replay<out M, in T, in H : PersistentHistory<M>> {

  /**
   * Returns `false` if an [event] with the given [seqno] and [site] can be applied on the current
   * history [H], or `true` if the replay should keep reverting.
   */
  fun revert(
      history: H,
      // TODO : Generic event identifiers.
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: T,
  ): Boolean

  companion object {

    /**
     * A [Replay] with does not reorder events, useful when the events support commutativity on
     * integration from remote.
     */
    @ExperimentalHistoryApi
    val None = Replay<Any?, Any?, PersistentHistory<Any?>> { _, _, _, _ -> false }

    /** A [Replay] which replays all the history, on every insertion. */
    @ExperimentalHistoryApi
    val All = Replay<Any?, Any?, PersistentHistory<Any?>> { _, _, _, _ -> true }
  }
}

@ExperimentalHistoryApi
interface PersistentReplayLinearHistory<out M, out T, out C> : PersistentLinearHistory<M, T, C>
