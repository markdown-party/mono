package party.markdown.p2p

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import webrtc.RTCDataChannel

/**
 * Transforms a [RTCDataChannel] to a [PeerToPeerConnection], which will remain active in the
 * provided [CoroutineScope].
 *
 * @receiver the [RTCDataChannel] which is used to create the connection.
 * @param scope the [CoroutineScope] in which the connection is managed.
 * @return the resulting [PeerToPeerConnection].
 */
fun RTCDataChannel.peerToPeerConnectionIn(scope: CoroutineScope): PeerToPeerConnection {
  val toRemote = Channel<String>(Channel.UNLIMITED)
  val fromRemote = Channel<String>(Channel.UNLIMITED)

  // TODO : Handle onclose event from RTCDataChannel.
  onopen = {
    // Start sending messages in the channel only after it's been opened !
    scope.launch {
      for (msg in toRemote) send(msg)
      close() // TODO : Test this ?
    }
    null
  }
  onmessage = {
    fromRemote.trySend(it.data.unsafeCast<String>())
    null
  }

  return object : PeerToPeerConnection {
    override val incoming: ReceiveChannel<String> = fromRemote
    override val outgoing: SendChannel<String> = toRemote
  }
}

/**
 * Pipes the elements of the receiver [ReceiveChannel] to the given [SendChannel].
 *
 * @param T the type of the piped elements.
 * @receiver the [ReceiveChannel] which is receives some elements to be piped.
 * @param channel the [SendChannel] to which the elements are sent.
 */
suspend fun <T> ReceiveChannel<T>.pipeTo(
    channel: SendChannel<T>,
): Unit = consumeAsFlow().onEach(channel::send).onCompletion { channel.close() }.collect()
