package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private const val FuzzIterationsCount = 1000

class Fuzzing {

  private fun fuzzInt() = Random.nextInt()
  private fun fuzzLong() = Random.nextLong()
  private fun fuzzSiteIdentifier() = SiteIdentifier.random()
  private fun fuzzSequenceNumber() = SequenceNumber(Random.nextInt().toUInt())
  private fun fuzzIncoming() =
      when (Random.nextInt(until = 3)) {
        0 -> Incoming.Advertisement(fuzzSiteIdentifier())
        1 -> Incoming.Ready
        2 -> Incoming.Event(fuzzSequenceNumber(), fuzzSiteIdentifier(), fuzzInt())
        else -> error("bad fuzzing")
      }
  private fun fuzzOutgoing() =
      when (Random.nextInt(until = 1)) {
        0 ->
            Outgoing.Request(
                fuzzSequenceNumber(), fuzzSequenceNumber(), fuzzSiteIdentifier(), fuzzLong())
        else -> error("bad fuzzing")
      }

  @Test
  fun testIncomingFuzz() =
      repeat(FuzzIterationsCount) {
        val incoming = fuzzIncoming()
        val encoded = Json.encodeToString(Incoming.serializer(Int.serializer()), incoming)
        val decoded = Json.decodeFromString(Incoming.serializer(Int.serializer()), encoded)
        assertEquals(incoming, decoded)
      }

  @Test
  fun testOutgoingFuzz() =
      repeat(FuzzIterationsCount) {
        val outgoing = fuzzOutgoing()
        val encoded = Json.encodeToString(Outgoing.serializer(), outgoing)
        val decoded = Json.decodeFromString(Outgoing.serializer<Int>(), encoded)
        assertEquals(outgoing, decoded)
      }
}
