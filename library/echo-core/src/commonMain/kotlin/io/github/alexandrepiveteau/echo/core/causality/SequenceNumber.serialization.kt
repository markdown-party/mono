package io.github.alexandrepiveteau.echo.core.causality

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object SequenceNumberSerializer : KSerializer<SequenceNumber> {

  override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("SequenceNumber", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): SequenceNumber {
    val decoded = decoder.decodeInt().toUInt()
    return SequenceNumber(index = decoded)
  }

  override fun serialize(encoder: Encoder, value: SequenceNumber) {
    val encoded = value.index.toInt()
    encoder.encodeInt(encoded)
  }
}
