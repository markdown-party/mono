package party.markdown.p2p

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.launch
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import webrtc.RTCDataChannel

/**
 * Creates a [SendChannel] for the provided [RTCDataChannel], and starts emitting items using the
 * provided [CoroutineScope].
 *
 * @receiver the [RTCDataChannel] for which a [SendChannel] is created.
 * @param scope the [CoroutineScope] in which the items are sent.
 * @return the [SendChannel] which can be used to emit the elements.
 */
fun RTCDataChannel.asSendChannelIn(scope: CoroutineScope): SendChannel<String> {
  val channel = Channel<String>(Channel.UNLIMITED)
  val onopen =
      object : EventListener {
        override fun handleEvent(event: Event) {
          // Only emit items once the channel is open.
          scope.launch {
            for (item in channel) send(item)
            close()
          }
          removeEventListener("open", this)
        }
      }
  val onclose =
      object : EventListener {
        override fun handleEvent(event: Event) {
          channel.cancel()
          removeEventListener("close", this)
        }
      }
  addEventListener("open", onopen)
  addEventListener("close", onclose)
  return channel
}

/**
 * Creates a [ReceiveChannel] for the provided [RTCDataChannel], and starts receiving items.
 *
 * @receiver the [RTCDataChannel] for which a [ReceiveChannel] is created.
 * @return the [ReceiveChannel] which can be used to receive the elements.
 */
fun RTCDataChannel.asReceiveChannel(): ReceiveChannel<String> {
  val channel = Channel<String>(Channel.UNLIMITED)
  val onmessage: (Event) -> Unit = { channel.trySend(it.asDynamic().data.unsafeCast<String>()) }
  val onclose =
      object : EventListener {
        override fun handleEvent(event: Event) {
          channel.close()
          removeEventListener("message", onmessage)
          removeEventListener("close", this)
        }
      }
  addEventListener("message", onmessage)
  addEventListener("close", onclose)
  return channel
}
