package io.github.alexandrepiveteau.echo.samples.log

import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.mutableEventLogOf
import kotlin.random.Random

fun main() {
  // A SiteIdentifier uniquely identifies a site in the library. They can be constructed with an
  // instance of Random.
  val alice = Random.nextSiteIdentifier()
  val bob = Random.nextSiteIdentifier()

  // Events are just arrays of bytes. You should use the [echo] library if you're interested in
  // working with generic types, which will automatically be serialized.
  val event1: ByteArray = byteArrayOf(1, 2)
  val event2: ByteArray = byteArrayOf(3, 4, 5)
  val event3: ByteArray = byteArrayOf(6, 7, 8, 9)

  val log =
      mutableEventLogOf().apply {
        // Appending an event means that the event will get a higher sequence number than all the
        // previously issued events for the given site. This is usually what you'll want to do when
        // issuing some new events.
        //
        // In this example, we have `seq1 == SequenceNumber.Min`, and `seq2 == SequenceNumber.Min +
        // 1u`.
        val (seq1, _) = append(site = alice, event = event1)
        val (seq2, _) = append(site = alice, event = event2)

        println("seq1: $seq1, seq2: $seq2")

        // Inserting an event lets you specify both the site identifier and the sequence number for
        // an
        // event. However, you can't insert events for a given site if the log has already seen a
        // sequence number greater or equal to it.
        insert(site = bob, seqno = SequenceNumber.Min, event = event3)
        insert(site = alice, seqno = SequenceNumber.Min, event = event1) // no effect
      }

  println(log.contains(SequenceNumber.Min + 0U, alice)) // true
  println(log.contains(SequenceNumber.Min + 2U, alice)) // false
}
