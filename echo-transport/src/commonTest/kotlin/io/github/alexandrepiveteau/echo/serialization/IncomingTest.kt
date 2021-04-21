package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class IncomingTest {

  @Test
  fun testAdvertisementDecoding() {
    val json = """
        {"type":"advertisement", "site": "00000000"}
      """
    val decoded = Json.decodeFromString(Incoming.serializer(Int.serializer()), json)
    assertEquals(Incoming.Advertisement(site = SiteIdentifier(Int.MIN_VALUE)), decoded)
  }

  @Test
  fun testReadyDecoding() {
    val json = """
            {"type":"ready"}
        """
    val decoded = Json.decodeFromString(Incoming.serializer(Int.serializer()), json)
    assertEquals(Incoming.Ready, decoded)
  }

  @Test
  fun testEventDecoding() {
    val json =
        """
            { "type": "event",
              "site": "00000000",
              "seqno": 123,
              "event": 1234
            }
        """
    val decoded = Json.decodeFromString(Incoming.serializer(Int.serializer()), json)
    assertEquals(
        Incoming.Event(
            site = SiteIdentifier(Int.MIN_VALUE),
            seqno = SequenceNumber(123U),
            body = 1234,
        ),
        decoded)
  }

  @Test
  fun testDoneDecoding() {
    val json = """
            {"type": "done"}
        """
    val decoded = Json.decodeFromString(Incoming.serializer(Int.serializer()), json)
    assertEquals(Incoming.Done, decoded)
  }
}
