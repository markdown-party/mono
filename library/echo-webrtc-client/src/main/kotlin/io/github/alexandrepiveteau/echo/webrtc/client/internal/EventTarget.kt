package io.github.alexandrepiveteau.echo.webrtc.client.internal

import kotlin.coroutines.resume
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.EventTarget

/**
 * Returns a [Flow] of [Event] from this [EventTarget], registering to all events of the given
 * [type].
 *
 * @receiver the [EventTarget] on which the events are observed.
 * @param type the type of the events which are registered.
 * @param onEventListenerAdded a callback which will be invoked after the event listener has been
 * registered. This callback will be invoked once for each collection.
 * @return a [Flow] of all the [Event] of the provided type.
 */
internal inline fun EventTarget.eventFlow(
    type: String,
    crossinline onEventListenerAdded: () -> Unit = {},
): Flow<Event> =
    callbackFlow {
          val listener: (Event) -> Unit = { trySend(it) }
          addEventListener(type, listener)
          onEventListenerAdded()
          awaitClose { removeEventListener(type, listener) }
        }
        .buffer(Channel.UNLIMITED)

/**
 * Waits until an [Event] dispatched by this [EventTarget].
 *
 * @receiver the [EventTarget] on which the events are observed.
 * @param type the type of the event which is listened.
 * @return the [Event] that was dispatched.
 */
internal suspend inline fun EventTarget.awaitEvent(type: String) =
    suspendCancellableCoroutine<Event> { cont ->
      val listener =
          object : EventListener {
            override fun handleEvent(event: Event) {
              removeEventListener(type, this)
              cont.resume(event)
            }
          }
      addEventListener(type, listener)
      cont.invokeOnCancellation { removeEventListener(type, listener) }
    }
