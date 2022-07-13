package io.github.alexandrepiveteau.echo.indexeddb

import com.juul.indexeddb.KeyPath
import io.github.alexandrepiveteau.echo.core.causality.toSequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.toSiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toUInt
import io.github.alexandrepiveteau.echo.core.log.Event
import kotlinx.js.jso

/** The [KeyPath] for the keys of [StoredEvent]. */
internal val EventIdPath = KeyPath("session", "site", "seqno")

/** The path for the events object store. */
internal const val EventsStore = "events"

/** An interface representing an [StoredEvent] which may be persisted in the database. */
internal external interface StoredEvent {

  /** The session identifier for this event. */
  var session: String

  /** The site identifier for the [StoredEvent]. */
  var site: Int

  /** The sequence number for the [StoredEvent]. */
  var seqno: Int

  /** The body of the [StoredEvent]. */
  var data: ByteArray
}

/**
 * Transforms this [Event] to a [StoredEvent].
 *
 * @param session the current session.
 */
internal fun Event.toStoredEvent(session: String): StoredEvent {
  val event = jso<StoredEvent>()
  event.session = session
  event.site = site.toUInt().toInt()
  event.seqno = seqno.toUInt().toInt()
  event.data = data
  return event
}

/** Transforms this [StoredEvent] to an [Event]. */
internal fun StoredEvent.toEvent(): Event {
  return Event(
      site = site.toUInt().toSiteIdentifier(),
      seqno = seqno.toUInt().toSequenceNumber(),
      data = data,
  )
}
