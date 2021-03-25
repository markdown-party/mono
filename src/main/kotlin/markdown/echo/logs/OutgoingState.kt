@file:OptIn(InternalCoroutinesApi::class)
@file:Suppress("SameParameterValue")

package markdown.echo.logs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.withLock
import markdown.echo.EchoEventLogPreview
import markdown.echo.Message.V1.Incoming as Inc
import markdown.echo.Message.V1.Outgoing as Out
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier

/**
 * A sealed class representing the different states that the finite state machine may be in. Each
 * step may decide to continue or stop processing with the [keepGoing] method, and defines state
 * transitions as part of the [step].
 *
 * @param T the type of the events.
 */
internal sealed class OutgoingState<T> {

  companion object {

    /** Creates a new [OutgoingState] that's the beginning of the FSM. */
    operator fun <T> invoke(): OutgoingState<T> = OutgoingAdvertising(mutableListOf())
  }

  /**
   * Returns true iff a loop on this state should keep iterating.
   *
   * @param incoming the [ReceiveChannel] for events coming from other sites.
   * @param outgoing the [SendChannel] for events going to other sites.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  open fun keepGoing(
      incoming: ReceiveChannel<*>,
      outgoing: SendChannel<*>,
  ): Boolean {
    return !incoming.isClosedForReceive && !outgoing.isClosedForSend
  }

  /** Computes the next step. */
  abstract suspend fun step(): OutgoingStep<T>
}

/**
 * Indicates that a step with the given name should not be reachable.
 *
 * @param name the name of the unreachable step.
 */
private fun notReachable(name: String? = null): Nothing {
  error("State ${name?.plus(" ")}should not be reachable")
}

// FINITE STATE MACHINE

private data class OutgoingAdvertising<T>(
    private val available: MutableList<SiteIdentifier>,
) : OutgoingState<T>() {

  override suspend fun step(): OutgoingStep<T> = {
    select {
      onReceiveOrClosed { v ->
        when (val msg = v.valueOrNull) {
          is Inc.Advertisement -> {
            available += msg.site
            this@OutgoingAdvertising // mutable state update.
          }
          is Inc.Ready -> {
            OutgoingListening(
                pendingRequests = available,
                requested = mutableListOf(),
            )
          }
          is Inc.Done, null -> OutgoingCancelling()
          is Inc.Event -> notReachable()
        }
      }
    }
  }
}

@OptIn(EchoEventLogPreview::class)
private data class OutgoingListening<T>(
    private val pendingRequests: MutableList<SiteIdentifier>,
    private val requested: MutableList<SiteIdentifier>,
) : OutgoingState<T>() {

  override suspend fun step(): OutgoingStep<T> = { log ->
    val request = pendingRequests.lastOrNull()
    val expected = withLock { request?.let(log::expected) } ?: SequenceNumber.Zero
    val max = withLock { log.expected }

    select {
      if (request != null) {
        onSend(
            Out.Request(
                nextForAll = max,
                nextForSite = expected,
                site = request,
                count = Long.MAX_VALUE,
            )) {
          pendingRequests.removeLast()
          requested.add(request)
          this@OutgoingListening // mutable state update.
        }
      }

      onReceiveOrClosed { v ->
        when (val msg = v.valueOrNull) {
          is Inc.Done, null -> OutgoingCancelling()
          is Inc.Advertisement -> {
            pendingRequests.add(msg.site)
            this@OutgoingListening // mutable state update.
          }
          is Inc.Event -> {
            withLock { log[msg.seqno, msg.site] = msg.body }
            this@OutgoingListening
          }
          is Inc.Ready -> notReachable()
        }
      }
    }
  }
}

// 1. We can send a Done message, and move to Completed.
private class OutgoingCancelling<T> : OutgoingState<T>() {

  override suspend fun step(): OutgoingStep<T> = {
    select { onSend(Out.Done) { OutgoingCompleted() } }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is OutgoingCancelling<*>) return false

    return true
  }

  override fun hashCode(): Int {
    return 31
  }
}

// 1. We receive a Done message, and move to Completed.
private class OutgoingCompleting<T> : OutgoingState<T>() {

  override suspend fun step(): OutgoingStep<T> = {
    select {
      onReceiveOrClosed { v ->
        when (v.valueOrNull) {
          is Inc.Done, null -> OutgoingCompleted()
          else -> this@OutgoingCompleting // Draining.
        }
      }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is OutgoingCompleting<*>) return false

    return true
  }

  override fun hashCode(): Int {
    return 31
  }
}

// We're done.
private class OutgoingCompleted<T> : OutgoingState<T>() {

  override fun keepGoing(
      incoming: ReceiveChannel<*>,
      outgoing: SendChannel<*>,
  ): Boolean = false

  override suspend fun step(): OutgoingStep<T> = notReachable("Completed")

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is OutgoingCompleted<*>) return false

    return true
  }

  override fun hashCode(): Int {
    return 31
  }
}
