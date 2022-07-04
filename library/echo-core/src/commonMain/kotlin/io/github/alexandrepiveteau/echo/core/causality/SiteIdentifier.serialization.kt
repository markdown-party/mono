package io.github.alexandrepiveteau.echo.core.causality

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object SiteIdentifierSerializer : KSerializer<SiteIdentifier> {

  override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("SiteIdentifier", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): SiteIdentifier {
    val decoded = decoder.decodeInt().toUInt()
    return SiteIdentifier(unique = decoded)
  }

  override fun serialize(encoder: Encoder, value: SiteIdentifier) {
    val encoded = value.unique.toInt()
    encoder.encodeInt(encoded)
  }
}
