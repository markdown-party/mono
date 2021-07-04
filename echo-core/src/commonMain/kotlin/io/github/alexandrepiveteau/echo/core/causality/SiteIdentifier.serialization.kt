package io.github.alexandrepiveteau.echo.core.causality

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * [SiteIdentifier] are represented as a [String] in their serialized format, with exactly 8
 * characters. Each character is in the hexadecimal range [0, F].
 */
internal object SiteIdentifierSerializer : KSerializer<SiteIdentifier> {

  // How much values are shifted in their encoding / representation.
  private const val HALF_INT = 1 shl 31

  // Format.
  private const val RADIX = 16
  private const val LENGTH = 8

  override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("SiteIdentifier", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): SiteIdentifier {
    val packed = decoder.decodeString()
    val unpacked =
        packed.toUIntOrNull(RADIX) ?: throw SerializationException("Invalid SiteIdentifier")
    val unshifted = unpacked + HALF_INT.toUInt()
    return unshifted.toSiteIdentifier()
  }

  override fun serialize(encoder: Encoder, value: SiteIdentifier) {
    val shifted = value.toUInt() + HALF_INT.toUInt()
    val packed = shifted.toString(RADIX).padStart(LENGTH, '0')
    encoder.encodeString(packed)
  }
}
