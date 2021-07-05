package io.github.alexandrepiveteau.echo.core.causality

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object EventIdentifierSerializer : KSerializer<EventIdentifier> {

  override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("EventIdentifier", PrimitiveKind.LONG)

  override fun deserialize(decoder: Decoder): EventIdentifier {
    val decoded = decoder.decodeLong().toULong()
    return EventIdentifier(packed = decoded)
  }

  override fun serialize(encoder: Encoder, value: EventIdentifier) {
    val encoded = value.packed.toLong()
    encoder.encodeLong(encoded)
  }
}
