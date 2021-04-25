package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class OutgoingTest {

  @Test
  fun testAcknowledgeDecoding() {
    val json =
        """
            { "type": "acknowledge"
            , "nextSeqno": 123
            , "site": "00000000"
            }
        """
    val decoded = Json.decodeFromString(Outgoing.serializer<Nothing>(), json)
    assertEquals(
        Outgoing.Acknowledge(
            nextSeqno = SequenceNumber(123u),
            site = SiteIdentifier(Int.MIN_VALUE),
        ),
        decoded,
    )
  }

  @Test
  fun testRequestDecoding() {
    val json =
        """
            { "type": "request"
            , "site": "00000000"
            , "count": 42
            }
        """
    val decoded = Json.decodeFromString(Outgoing.serializer<Nothing>(), json)
    assertEquals(
        Outgoing.Request(
            site = SiteIdentifier(Int.MIN_VALUE),
            count = 42U,
        ),
        decoded,
    )
  }
}
