package io.github.alexandrepiveteau.echo.webrtc.client

import io.github.alexandrepiveteau.echo.ReceiveExchange
import io.github.alexandrepiveteau.echo.SendExchange
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.flow.*

/**
 * An interface representing a connection with a remote peer. A [PeerToPeerConnection] can send and
 * receive messages, and is connected to another peer.
 */
interface PeerToPeerConnection {

  /** Attempts to receive a message from the peer. */
  suspend fun receiveCatching(): ChannelResult<String>

  /** Attempts to send a message to the peer. */
  suspend fun sendCatching(message: String): ChannelResult<Unit>
}

/**
 * Receives all the messages from this [PeerToPeerConnection], and emits them in a [Flow].
 *
 * @receiver the [PeerToPeerConnection] from which the messages are received.
 * @return the [Flow] of messages.
 */
internal fun PeerToPeerConnection.receiveAsFlow(): Flow<String> = flow {
  var finished = false
  while (!finished) {
    receiveCatching()
        .onSuccess { emit(it) }
        .onFailure { it?.let { throw it } }
        .onClosed { finished = true }
  }
}

/**
 * Collects all the [String] in this [Flow], and sends them to the [connection].
 *
 * @receiver the [Flow] of messages that is collected.
 * @param connection the [PeerToPeerConnection] to which the messages are sent.
 */
internal suspend fun Flow<String>.collectTo(
    connection: PeerToPeerConnection,
) = collect { value -> connection.sendCatching(value).getOrThrow() }

/** Syncs this [ReceiveExchange] with the provided [PeerToPeerConnection]. */
internal suspend fun ReceiveExchange<String, String>.sync(
    connection: PeerToPeerConnection,
) = receive(connection.receiveAsFlow()).collectTo(connection)

/** Syncs this [SendExchange] with the provided [PeerToPeerConnection]. */
internal suspend fun SendExchange<String, String>.sync(
    connection: PeerToPeerConnection,
) = send(connection.receiveAsFlow()).collectTo(connection)
