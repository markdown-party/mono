package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming.Advertisement
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming.Companion as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming.Ready
import io.github.alexandrepiveteau.echo.serialization.serializer
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Ignore
class JsonDecodingTest {

  /** A custom serializable [Body] for testing. */
  @Serializable private object Body

  @Test
  fun testAdvertisementDecoding() {
    val json = """
        {"t":"adv", "s": 123}
      """
    val decoded = Json.decodeFromString(Inc.serializer(Body.serializer()), json)
    assertEquals(Advertisement(site = SiteIdentifier(123)), decoded)
  }

  @Test
  fun testReadyDecoding() {
    val json = """
            {"t":"rdy"}
        """
    val decoded = Json.decodeFromString(Inc.serializer(Body.serializer()), json)
    assertEquals(Ready, decoded)
  }
}
