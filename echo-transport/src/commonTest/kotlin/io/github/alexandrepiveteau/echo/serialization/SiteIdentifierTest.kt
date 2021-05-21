package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.causal.toSiteIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class SiteIdentifierTest {

  private fun testEncodingDecoding(value: SiteIdentifier) {
    val serializer = SiteIdentifier.serializer()
    val encoded = Json.encodeToString(serializer, value)
    val decoded = Json.decodeFromString(serializer, encoded)
    assertEquals(value, decoded)
  }

  @Test
  fun testMinSerialization() {
    val expected = "\"00000000\""
    val identifier = Int.MIN_VALUE.toSiteIdentifier()
    val encoded = Json.encodeToString(SiteIdentifier.serializer(), identifier)
    assertEquals(expected, encoded)
    val decoded = Json.decodeFromString(SiteIdentifier.serializer(), expected)
    assertEquals(identifier, decoded)
  }

  @Test
  fun testMaxSerialization() {
    val expected = "\"ffffffff\""
    val identifier = Int.MAX_VALUE.toSiteIdentifier()
    val encoded = Json.encodeToString(SiteIdentifier.serializer(), identifier)
    assertEquals(expected, encoded)
    val decoded = Json.decodeFromString(SiteIdentifier.serializer(), expected)
    assertEquals(identifier, decoded)
  }

  @Test
  fun testZeroSerialization() {
    // 2^31 = 2 147 483 648 = 0x80000000
    val expected = "\"80000000\""
    val identifier = 0.toSiteIdentifier()
    val encoded = Json.encodeToString(SiteIdentifier.serializer(), identifier)
    assertEquals(expected, encoded)
    val decoded = Json.decodeFromString(SiteIdentifier.serializer(), expected)
    assertEquals(identifier, decoded)
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
    for (i in -2..2) testEncodingDecoding(i.toSiteIdentifier())
  }
}
