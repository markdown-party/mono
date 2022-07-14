package io.github.alexandrepiveteau.echo.indexeddb

import com.juul.indexeddb.Key
import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.core.buffer.copyOfRange
import io.github.alexandrepiveteau.echo.core.log.Event
import io.github.alexandrepiveteau.echo.core.log.EventIterator
import io.github.alexandrepiveteau.echo.core.log.mutableEventLogOf
import io.github.alexandrepiveteau.echo.exchange
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.github.alexandrepiveteau.echo.sync
import io.github.alexandrepiveteau.echo.sync.SyncStrategy

/** Transforms the events from the provided [EventIterator] into a [List] of [Event]. */
private fun EventIterator.toList(): List<Event> = buildList {
  while (hasPrevious()) movePrevious()
  if (has()) {
    add(
        Event(
            seqno = seqno,
            site = site,
            data = event.copyOfRange(from, until),
        ),
    )
  }
  while (hasNext()) add(next())
}

/**
 * Saves the content from the provided [Exchange] to the given [session] in the browser.
 *
 * @param session the session identifier.
 * @param exchange the [Exchange] of which the events are saved.
 */
suspend fun save(session: String, exchange: Exchange<Incoming, Outgoing>) {
  val database = openDatabase()
  val log = mutableEventLogOf().also { sync(exchange, exchange(it, SyncStrategy.Once)) }
  val events = log.iterator().toList()
  database.writeTransaction(EventsStore) {
    objectStore(EventsStore).apply { events.forEach { put(it.toStoredEvent(session)) } }
  }
}

/**
 * Loads the events from the browser into the provided [Exchange].
 *
 * @param session the session identifier.
 * @param exchange the [Exchange] of which the events are saved.
 */
suspend fun load(session: String, exchange: Exchange<Incoming, Outgoing>) {
  val database = openDatabase()
  val events =
      database.transaction(EventsStore) {
        val log = mutableEventLogOf()
        val events =
            objectStore(EventsStore)
                .index(SessionIndex)
                .getAll(Key(session))
                .map { it.unsafeCast<StoredEvent>().toEvent() }
                .sortedBy { it.seqno }
        events.forEach { event ->
          log.insert(
              seqno = event.seqno,
              site = event.site,
              event = event.data,
          )
        }
        log
      }
  val local = exchange(events, SyncStrategy.Once)
  sync(local, exchange)
}
