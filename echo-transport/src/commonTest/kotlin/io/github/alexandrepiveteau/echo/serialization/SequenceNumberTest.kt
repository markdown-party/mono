package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.toSequenceNumber
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class SequenceNumberTest {

  private fun testEncodingDecoding(value: SequenceNumber) {
    val serializer = SequenceNumber.serializer()
    val encoded = Json.encodeToString(serializer, value)
    val decoded = Json.decodeFromString(serializer, encoded)
    assertEquals(value, decoded)
  }

  @Test
  fun testZeroSerialization() {
    testEncodingDecoding(SequenceNumber.Zero)
  }

  @Test
  fun testMaxSerialization() {
    testEncodingDecoding(SequenceNumber.Max)
  }

  @Test
  fun testFailsNegativeDeserialization() {
    val encoded1 = "-1"
    assertFailsWith<SerializationException> {
      Json.decodeFromString(SequenceNumber.serializer(), encoded1)
    }

    val encoded2 = "${Long.MIN_VALUE}"
    assertFailsWith<SerializationException> {
      Json.decodeFromString(SequenceNumber.serializer(), encoded2)
    }
  }

  @Test
  fun testFailsTooBigDeserialization() {
    val encoded = "${UInt.MAX_VALUE.toLong() + 1}"
    assertFailsWith<SerializationException> {
      Json.decodeFromString(SequenceNumber.serializer(), encoded)
    }
  }

  @Ignore
  @Test
  fun testExhaustively() {
    for (i in UInt.MIN_VALUE..UInt.MAX_VALUE) {
      testEncodingDecoding(i.toSequenceNumber())
    }
  }
}
