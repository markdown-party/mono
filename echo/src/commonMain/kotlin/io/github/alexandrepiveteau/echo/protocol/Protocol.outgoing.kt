package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.core.buffer.*
import io.github.alexandrepiveteau.echo.core.causality.*
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as I
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as O
import kotlinx.coroutines.selects.select

/**
 * Starts the outgoing side of the protocol, sending advertisements, receiving requests and sending
 * events as they are made available in the local log.
 */
internal suspend fun ExchangeScope<O, I>.startOutgoing() = runCatchingTermination {
  val advertised = outgoingAdvertiseAll()
  outgoingSending(advertised)
}

/**
 * Advertises all the available events, and returns an [MutableEventIdentifierGapBuffer] with the
 * advertised contents. Receiving any message from the other site at this moment should lead to an
 * illegal state, which should also be reached if the outgoing channel gets closed for whatever
 * reason.
 */
private suspend fun ExchangeScope<O, I>.outgoingAdvertiseAll(): MutableEventIdentifierGapBuffer {
  val missing = withEventLogLock { acknowledged() }
  val queue = mutableListOf<I>()

  // Prepare the queue of messages to send.
  for ((seqno, site) in missing) queue += I.Advertisement(site, seqno + 1U)
  queue += I.Ready

  while (true) {

    // Stop sending messages when the pending queue is empty.
    val msg = queue.firstOrNull() ?: return missing.toMutableGapBuffer()

    // Either receive a message (or closed token) from the other side, or send one of the
    // advertisements or ready messages. Priority is given to the reception of messages, to make
    // sure that exchanges may terminate right away.
    select<Unit> {
      onReceiveCatching {
        when (val m = it.getOrNull()) {
          null -> terminate()
          else -> error("Didn't expect to receive $m.")
        }
      }
      onSend(msg) { queue.removeFirst() }
    }
  }
}

/**
 * Sends all the local events, depending on the requests from the other site.
 *
 * @param advertised the event identifiers that were already previously advertised.
 */
private suspend fun ExchangeScope<O, I>.outgoingSending(
    advertised: MutableEventIdentifierGapBuffer,
) {
  // The buffers representing the minimum sequence numbers for each site that we have to send, and
  // the available credits for each site (aka how many events we're still allowed to send).
  val requestsBuffer = mutableEventIdentifierGapBufferOf()
  val creditsBuffer = mutableIntGapBufferOf()
  val queue = ArrayDeque<I>()

  while (true) {

    // Prepare the advertisements that should be sent, as well as the events.
    val available = withEventLogLock { acknowledged() }

    if (queue.isEmpty()) enqueueAdvertisements(advertised, available, queue)
    if (queue.isEmpty()) enqueueEvents(requestsBuffer, creditsBuffer, queue)

    select<Unit> {

      // Process incoming messages.
      onReceiveCatching { v ->
        when (val msg = v.getOrNull()) {
          is O.Acknowledge -> acknowledge(requestsBuffer, creditsBuffer, msg.site, msg.nextSeqno)
          is O.Request -> request(requestsBuffer, creditsBuffer, msg.site, msg.count)
          null -> terminate()
        }
      }

      // Send the first message from the queue.
      queue.firstOrNull()?.let { msg -> onSend(msg) { queue.removeFirst() } }

      // Run the loop again with the new events.
      onEventLogUpdate {}
    }
  }
}

/**
 * Enqueues the advertisements present in the [available] array which haven't been stored in the
 * [advertised] buffer yet. The advertisements will be stored in the message [queue].
 */
private fun enqueueAdvertisements(
    advertised: MutableEventIdentifierGapBuffer,
    available: EventIdentifierArray,
    queue: ArrayDeque<I>,
) {
  // TODO : Make this O(n) in worst case.
  // TODO : Make this O(1) in the general case.
  if (advertised.size == available.size) return // no update optimization.
  for ((seqno, site) in available) {
    // Check if the site is present in the advertised sites.
    var present = false
    for (adv in advertised.toEventIdentifierArray()) {
      if (adv.site == site) present = true
    }
    // Add the site to the advertised sites.
    if (!present) {
      advertised.push(EventIdentifier(seqno, site))
      queue += I.Advertisement(site, seqno + 1U)
    }
  }
}

/*
 * A comparator of I.Event messages, which sends events with a smaller identifier first. This allows
 * for more efficient sync when there are many concurrent messages.
 */
private object EventComparator : Comparator<I.Event> {
  override fun compare(
      a: Message.Incoming.Event,
      b: Message.Incoming.Event,
  ) = EventIdentifier(a.seqno, a.site).compareTo(EventIdentifier(b.seqno, b.site))
}

private suspend fun ExchangeScope<*, *>.enqueueEvents(
    requests: MutableEventIdentifierGapBuffer,
    credits: MutableIntGapBuffer,
    queue: MutableList<I>,
) {
  val events = mutableListOf<I.Event>()

  withEventLogLock {
    // TODO : Optimize this traversal.
    // TODO : Make this O(1) in the default case, rather than O(n).
    val iterator = iterator()
    while (iterator.hasPrevious()) iterator.movePrevious()

    // For each event, check if we have the credits.
    var keepGoing = size > 0
    while (keepGoing) {

      // Find the site index.
      var index = 0
      findIndex@ while (index < requests.size) {
        if (requests[index].site == iterator.site) break@findIndex
        index++
      }

      // Check if there are some credits, and if the event has not been acknowledged yet.
      val hasCredits = index != requests.size && credits[index].toUInt() > 0U
      val isUnknown = index != requests.size && requests[index].seqno <= iterator.seqno

      // Add the event to the queue.
      if (hasCredits && isUnknown) {
        consumeCredit(requests, credits, iterator.site)
        val event =
            I.Event(
                iterator.seqno,
                iterator.site,
                iterator.event.copyOfRange(iterator.from, iterator.until),
            )
        events.add(event)
        requests[index] = EventIdentifier(iterator.seqno + 1U, iterator.site)
      }

      if (iterator.hasNext()) {
        keepGoing = true
        iterator.moveNext()
      } else {
        keepGoing = false
      }
    }
  }

  // Sort the events by identifier before sending them to the other side.
  events.sortWith(EventComparator)
  queue.addAll(events)
}

/**
 * Processes an acknowledgement in the requests buffer. If the identifier of the site does not exist
 * yet, a new entry will be created.
 *
 * @param requests the buffer with the acknowledgements.
 * @param credits the buffer with the available credits.
 * @param site the [SiteIdentifier] that we acknowledge.
 * @param seqno the [SequenceNumber] that is known.
 */
private fun acknowledge(
    requests: MutableEventIdentifierGapBuffer,
    credits: MutableIntGapBuffer,
    site: SiteIdentifier,
    seqno: SequenceNumber,
) {
  var index = 0
  while (index < requests.size) {
    val currentSite = requests[index].site
    if (site == currentSite) {
      requests[index] = EventIdentifier(maxOf(requests[index].seqno, seqno), currentSite)
      return
    }
    index++
  }
  requests.push(EventIdentifier(seqno, site))
  credits.push(0)
}

/**
 * Processes a request message in the requests and credits buffers. If the identifier of the site
 * does not exist yet, a new entry will be created.
 *
 * @param requests the buffer with the acknowledgements.
 * @param credits the buffer with the available credits.
 * @param site the [SiteIdentifier] that we acknowledge.
 * @param count the number of events requested.
 */
private fun request(
    requests: MutableEventIdentifierGapBuffer,
    credits: MutableIntGapBuffer,
    site: SiteIdentifier,
    count: UInt,
) {
  var index = 0
  while (index < requests.size) {
    val currentSize = requests[index].site
    if (site == currentSize) {
      var updated = credits[index].toUInt() + count
      if (updated < count) updated = UInt.MAX_VALUE // overflow-safe
      credits[index] = updated.toInt()
      return
    }
    index++
  }
  requests.push(EventIdentifier(SequenceNumber.Min, site))
  credits.push(count.toInt())
}

/**
 * Consumes one credit for the given [site] from the [requests] and [credits] buffers. This assumes
 * that both buffers aren't sorted, and are constantly kept in sync when it comes to their sizes.
 *
 * @param requests the gap buffer with the acknowledgements.
 * @param credits the gap buffer with the request counts.
 * @param site the [SiteIdentifier] for which we consume one credit.
 */
private fun consumeCredit(
    requests: MutableEventIdentifierGapBuffer,
    credits: MutableIntGapBuffer,
    site: SiteIdentifier,
) {
  var index = 0
  while (index < requests.size) {
    val currentSite = requests[index].site
    if (site == currentSite) {
      credits[index] = credits[index] - 1
      return
    }
    index++
  }
}
