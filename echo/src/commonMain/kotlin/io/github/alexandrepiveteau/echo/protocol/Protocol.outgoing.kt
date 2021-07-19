package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.core.buffer.*
import io.github.alexandrepiveteau.echo.core.causality.*
import io.github.alexandrepiveteau.echo.core.log.Event
import io.github.alexandrepiveteau.echo.core.log.EventIterator
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as I
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as O
import kotlinx.coroutines.selects.select

/**
 * Advertises all the available events, and returns an [MutableEventIdentifierGapBuffer] with the
 * advertised contents. Receiving any message from the other site at this moment should lead to an
 * illegal state, which should also be reached if the outgoing channel gets closed for whatever
 * reason.
 */
internal suspend fun ExchangeScope<O, I>.outgoingAdvertiseAll(): MutableEventIdentifierGapBuffer {
  val missing = withEventLogLock { acknowledged() }
  val queue = ArrayDeque<I>()

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
 * @param stopAfterAdvertised a [Boolean] with value `true` if only the [advertised] events should
 * be sent.
 */
internal suspend fun ExchangeScope<O, I>.outgoingSending(
    advertised: MutableEventIdentifierGapBuffer,
    stopAfterAdvertised: Boolean,
) {
  // The buffers representing the minimum sequence numbers for each site that we have to send, and
  // the available credits for each site (aka how many events we're still allowed to send).
  val requestsBuffer = mutableEventIdentifierGapBufferOf()
  val creditsBuffer = mutableIntGapBufferOf()
  val queue = ArrayDeque<I>()

  while (true) {

    // Prepare the advertisements that should be sent, as well as the events.
    val available = withEventLogLock { acknowledged() }

    // Only check whether we're done sending all the required events after the queue is empty, as
    // we then make sure the messages have been properly received.
    if (queue.isEmpty() && stopAfterAdvertised)
        terminateIfAllAdvertisedSent(requestsBuffer, advertised)

    if (queue.isEmpty() && !stopAfterAdvertised) enqueueAdvertisements(advertised, available, queue)
    if (queue.isEmpty()) {
      enqueueEvents(
          requests = requestsBuffer,
          credits = creditsBuffer,
          queue = queue,
          advertised = advertised,
          stopAfterAdvertised = stopAfterAdvertised,
      )
    }

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
 * Terminates if all the advertised events can be found in the requested events. Requested events
 * are the next event that will be sent to the site, and therefore indicate what the other side is
 * ready to receive.
 *
 * @param requests the event identifiers requested by the other site.
 * @param advertised the events advertised by this site.
 */
private fun terminateIfAllAdvertisedSent(
    requests: MutableEventIdentifierGapBuffer,
    advertised: MutableEventIdentifierGapBuffer,
) {
  // This could run in O(n).
  for (i in 0 until advertised.size) {
    val (seqno, site) = advertised[i]
    val index = requests.binarySearchBySite(site)
    if (index < 0) return
    if (requests[index].seqno <= seqno) return
  }
  terminate()
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
  if (advertised.size == available.size) return // no update optimization.
  var adI = 0
  var avI = 0
  // Merge the items.
  while (adI < advertised.size && avI < available.size) {
    val ad = advertised[adI]
    val av = available[avI]
    when {
      ad.site == av.site -> {
        adI++ // next item
        avI++ // next item
      }
      ad.site > av.site -> {
        advertised.push(av, offset = adI)
        queue += I.Advertisement(av.site, av.seqno + 1U)
      }
      else -> error("site identifier in advertised $advertised but not in available $available")
    }
  }
  // Push the remaining items.
  while (avI < available.size) {
    val av = available[avI]
    advertised.push(av)
    queue += I.Advertisement(av.site, av.seqno + 1U)
    avI++
  }
}

/*
 * A comparator of I.Event messages, which sends events with a smaller identifier first. This allows
 * for more efficient sync when there are many concurrent messages.
 */
private object EventComparator : Comparator<Event> {
  override fun compare(
      a: Event,
      b: Event,
  ) = EventIdentifier(a.seqno, a.site).compareTo(EventIdentifier(b.seqno, b.site))
}

/**
 * Moves the given [EventIterator] until the event identifier formed by the provided
 * [SequenceNumber] and [SiteIdentifier] is present on the current index, or should be present if it
 * was to be included in the log.
 *
 * If the iterator is empty, it will not move.
 */
private fun EventIterator.moveAt(
    seqno: SequenceNumber,
    site: SiteIdentifier,
) {

  // Start from the right hand-side.
  while (hasNext()) moveNext()
  if (hasPrevious()) movePrevious()

  // We have an empty iterator.
  if (!has()) return

  // Iterate to the left, until we either can't move anymore, or are on an event identifier that is
  // smaller than the given value.
  while (true) {

    // Check the current item.
    when {
      EventIdentifier(this.seqno, this.site) == EventIdentifier(seqno, site) -> return
      EventIdentifier(this.seqno, this.site) < EventIdentifier(seqno, site) -> {
        if (hasNext()) moveNext()
        return
      }
      else -> {
        // If we reached the start of the iterator, stop here. Otherwise, we can move to the
        // previous item to check for equality.
        if (!hasPrevious()) return
        movePrevious()
      }
    }
  }
}

/**
 * Takes as many events as available in the [credits] (for the given [index]) and adds them to the
 * [queue]. If an event is taken, the [requests] array is updated.
 */
private fun EventIterator.take(
    index: Int,
    requests: MutableEventIdentifierGapBuffer,
    credits: MutableIntGapBuffer,
    queue: MutableList<Event>,
    advertised: MutableEventIdentifierGapBuffer,
    stopAfterAdvertised: Boolean,
) {

  if (!has()) return // Empty iterator.
  if (EventIdentifier(seqno, site) < requests[index]) return // No interesting event.

  while (true) {

    // We have a valid event. Check if we have the credits for it, and if so, add it to the queue.
    if (credits[index].toUInt() == 0U) return

    // Do not enqueue events if we have already reached the advertised threshold.
    if (stopAfterAdvertised) {
      val adIndex = advertised.binarySearchBySite(site)
      if (adIndex < 0) return
      if (seqno > advertised[adIndex].seqno) return
    }

    // Update the credits and requests.
    credits[index] = (credits[index].toUInt() - 1U).toInt()
    requests[index] = EventIdentifier(seqno + 1U, site)

    // Add the event to the queue.
    queue.add(
        Event(
            seqno = seqno,
            site = site,
            data = event.copyOfRange(from, until),
        ),
    )

    // Move to the next event, if possible.
    if (!hasNext()) return
    moveNext()
  }
}

/** Enqueues all the available events into the [ArrayDeque] of messages. */
private suspend fun ExchangeScope<*, *>.enqueueEvents(
    requests: MutableEventIdentifierGapBuffer,
    credits: MutableIntGapBuffer,
    queue: ArrayDeque<I>,
    advertised: MutableEventIdentifierGapBuffer,
    stopAfterAdvertised: Boolean,
) {
  val events = mutableListOf<Event>()

  withEventLogLock {
    for (i in 0 until requests.size) {
      val (seqno, site) = requests[i]
      val iterator = iterator(site).apply { moveAt(seqno, site) }
      iterator.take(
          index = i,
          requests = requests,
          credits = credits,
          queue = events,
          advertised = advertised,
          stopAfterAdvertised = stopAfterAdvertised,
      )
    }
  }

  // Sort the events by identifier before sending them to the other side.
  events.sortWith(EventComparator)
  if (events.isNotEmpty()) queue.addLast(I.Events(events))
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
  val index = requests.binarySearchBySite(site)
  if (index >= 0) {
    requests[index] = EventIdentifier(maxOf(requests[index].seqno, seqno), site)
  } else {
    requests.push(EventIdentifier(seqno, site), offset = -(index + 1))
    credits.push(0, -(index + 1))
  }
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
  val index = requests.binarySearchBySite(site)
  if (index >= 0) {
    var updated = credits[index].toUInt() + count
    if (updated < count) updated = UInt.MAX_VALUE // overflow-safe
    credits[index] = updated.toInt()
  } else {
    requests.push(EventIdentifier(SequenceNumber.Min, site), offset = -(index + 1))
    credits.push(count.toInt(), offset = -(index + 1))
  }
}
