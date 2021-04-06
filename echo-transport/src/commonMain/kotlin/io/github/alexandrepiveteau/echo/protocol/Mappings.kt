package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.causal.toInt
import io.github.alexandrepiveteau.echo.causal.toUInt

/**
 * Maps a [Message.V1.Incoming] to its [Transport] counterpart.
 *
 * @param T the type of the event body.
 */
fun <T> Message.V1.Incoming<T>.toTransport(
    encode: (T) -> String,
): Transport.V1.Incoming =
    when (this) {
      is Message.V1.Incoming.Advertisement ->
          Transport.V1.Incoming.Advertisement(
              site = this.site.toInt(),
          )
      is Message.V1.Incoming.Ready -> Transport.V1.Incoming.Ready
      is Message.V1.Incoming.Event ->
          Transport.V1.Incoming.Event(
              seqno = this.seqno.toUInt().toInt(),
              site = this.site.toInt(),
              body = encode(this.body),
          )
      is Message.V1.Incoming.Done -> Transport.V1.Incoming.Done
    }

/**
 * Maps a [Message.V1.Outgoing] to its [Transport] counterpart.
 *
 * @param T the type the event body.
 */
fun <T> Message.V1.Outgoing<T>.toTransport(): Transport.V1.Outgoing =
    when (this) {
      is Message.V1.Outgoing.Request ->
          Transport.V1.Outgoing.Request(
              nextForAll = this.nextForAll.toUInt().toInt(),
              nextForSite = this.nextForSite.toUInt().toInt(),
              count = this.count,
              site = this.site.toInt(),
          )
      is Message.V1.Outgoing.Done -> Transport.V1.Outgoing.Done
    }

/**
 * Maps a [Transport.V1.Incoming] to its [Message] counterpart.
 *
 * @param T the type of the event body.
 */
fun <T> Transport.V1.Incoming.toMessage(
    decode: (String) -> T,
): Message.V1.Incoming<T> =
    when (this) {
      is Transport.V1.Incoming.Advertisement ->
          Message.V1.Incoming.Advertisement(
              site = SiteIdentifier(unique = this.site),
          )
      is Transport.V1.Incoming.Ready -> Message.V1.Incoming.Ready
      is Transport.V1.Incoming.Event ->
          Message.V1.Incoming.Event(
              site = SiteIdentifier(unique = this.site),
              seqno = SequenceNumber(index = this.seqno.toUInt()),
              body = decode(this.body),
          )
      is Transport.V1.Incoming.Done -> Message.V1.Incoming.Done
    }

/**
 * Maps a [Transport.V1.Outgoing] to its [Message] counterpart.
 *
 * @param T the type of the event body.
 */
fun <T> Transport.V1.Outgoing.toMessage(): Message.V1.Outgoing<T> =
    when (this) {
      is Transport.V1.Outgoing.Request ->
          Message.V1.Outgoing.Request(
              nextForAll = SequenceNumber(this.nextForAll.toUInt()),
              nextForSite = SequenceNumber(this.nextForSite.toUInt()),
              site = SiteIdentifier(this.site),
              count = this.count,
          )
      is Transport.V1.Outgoing.Done -> Message.V1.Outgoing.Done
    }
