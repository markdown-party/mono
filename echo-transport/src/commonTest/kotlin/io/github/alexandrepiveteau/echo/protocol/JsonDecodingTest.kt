package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.protocol.Transport.V1.Incoming as Inc
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
    val decoded = Json.decodeFromString(Transport.V1.serializer(), json)
    assertEquals(Inc.Advertisement(site = 123), decoded)
  }

  @Test
  fun testReadyDecoding() {
    val json = """
            {"t":"rdy"}
        """
    val decoded = Json.decodeFromString(Transport.V1.serializer(), json)
    assertEquals(Inc.Ready, decoded)
  }
}
