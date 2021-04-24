package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class OutgoingTest {

  @Test
  fun testRequestDecoding() {
    val json =
        """
            { "type": "request"
            , "nextForSite": 123
            , "nextForAll": 321
            , "site": "00000000"
            , "count": 42
            }
        """
    val decoded = Json.decodeFromString(Outgoing.serializer<Nothing>(), json)
    assertEquals(
        Outgoing.Request(
            nextForSite = SequenceNumber(123u),
            nextForAll = SequenceNumber(321u),
            site = SiteIdentifier(Int.MIN_VALUE),
            count = 42,
        ),
        decoded)
  }
}
