package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toSiteIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.protobuf.ProtoBuf

class SiteIdentifierTest {

  private fun testEncodingDecoding(value: SiteIdentifier) {
    val serializer = SiteIdentifier.serializer()
    val encoded = ProtoBuf.encodeToByteArray(serializer, value)
    val decoded = ProtoBuf.decodeFromByteArray(serializer, encoded)
    assertEquals(value, decoded)
  }

  @Test
  fun testAFewValues() {
    for (i in -2..2) testEncodingDecoding(i.toUInt().toSiteIdentifier())
  }
}
