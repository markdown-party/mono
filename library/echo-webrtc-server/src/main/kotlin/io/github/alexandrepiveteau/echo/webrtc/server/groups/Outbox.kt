package io.github.alexandrepiveteau.echo.webrtc.server.groups

import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel

/**
 * An alternative to [SendChannel] which can be used to send some messages to a specific client and
 * catches [ClosedSendChannelException], but propagates cancellation exceptions properly.
 *
 * @param T the type of the messages sent.
 */
internal fun interface Outbox<in T> {

  /**
   * Send a message through this [Outbox].
   *
   * @param element the type of the sent element.
   */
  suspend fun sendCatching(element: T)

  companion object {

    /**
     * Creates an [Outbox] from the provided [SendChannel].
     *
     * @param T the type of the elements which can be sent.
     * @param channel the underlying [SendChannel].
     * @return the resulting [Outbox].
     */
    internal fun <T> wrap(channel: SendChannel<T>) =
        Outbox<T> { element ->
          try {
            channel.send(element)
          } catch (_: Throwable) {
            // Ignore, but propagate cancellation exceptions.
          }
        }
  }
}
