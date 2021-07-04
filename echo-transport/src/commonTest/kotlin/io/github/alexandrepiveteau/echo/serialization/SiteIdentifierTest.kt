package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toSiteIdentifier
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SiteIdentifierTest {

  private fun testEncodingDecoding(value: SiteIdentifier) {
    val serializer = SiteIdentifier.serializer()
    val encoded = Json.encodeToString(serializer, value)
    val decoded = Json.decodeFromString(serializer, encoded)
    assertEquals(value, decoded)
  }

  @Test
  fun testCapitalizationSupported() {
    val lowercase = Json.decodeFromString(SiteIdentifier.serializer(), "\"abcd0000\"")
    val uppercase = Json.decodeFromString(SiteIdentifier.serializer(), "\"ABCD0000\"")
    val mixedcase = Json.decodeFromString(SiteIdentifier.serializer(), "\"aBcD0000\"")
    assertEquals(lowercase, uppercase)
    assertEquals(uppercase, mixedcase)
    assertEquals(mixedcase, lowercase)
  }

  @Test
  fun testAFewValues() {
    for (i in -2..2) testEncodingDecoding(i.toUInt().toSiteIdentifier())
  }
}
