package io.github.alexandrepiveteau.echo.core.causality

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * [SequenceNumber] are represented as a [Long] in their serialized format, even though only the
 * least significant 32 bits are actually used.
 */
internal object SequenceNumberSerializer : KSerializer<SequenceNumber> {

  override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("SequenceNumber", PrimitiveKind.LONG)

  override fun deserialize(decoder: Decoder): SequenceNumber {
    val decoded = decoder.decodeLong()
    val leastSignificant = decoded and 0xFFFFFFFF
    if (decoded != leastSignificant)
        throw SerializationException(
            "SequenceNumber not in [${UInt.MIN_VALUE}, ${UInt.MAX_VALUE}] range",
        )
    return leastSignificant.toUInt().toSequenceNumber()
  }

  override fun serialize(encoder: Encoder, value: SequenceNumber) {
    val encoded = value.toUInt().toLong()
    encoder.encodeLong(encoded)
  }
}
