package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.toSequenceNumber
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.protobuf.ProtoBuf

class SequenceNumberTest {

  private fun testEncodingDecoding(value: SequenceNumber) {
    val serializer = SequenceNumber.serializer()
    val encoded = ProtoBuf.encodeToByteArray(serializer, value)
    val decoded = ProtoBuf.decodeFromByteArray(serializer, encoded)
    assertEquals(value, decoded)
  }

  @Test
  fun testZeroSerialization() {
    testEncodingDecoding(SequenceNumber.Min)
  }

  @Test
  fun testMaxSerialization() {
    testEncodingDecoding(SequenceNumber.Max)
  }

  @Ignore
  @Test
  fun testExhaustively() {
    for (i in UInt.MIN_VALUE..UInt.MAX_VALUE) {
      testEncodingDecoding(i.toSequenceNumber())
    }
  }
}
