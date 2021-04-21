package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.causal.toInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Returns a [KSerializer] that supports [SiteIdentifier] conversions. */
fun SiteIdentifier.Companion.serializer(): KSerializer<SiteIdentifier> = SiteIdentifierSerializer

/**
 * [SiteIdentifier] are represented as a [String] in their serialized format, with exactly 8
 * characters. Each character is in the hexadecimal range [0, F].
 */
private object SiteIdentifierSerializer : KSerializer<SiteIdentifier> {

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
    val unshifted = unpacked.toInt() + HALF_INT
    return SiteIdentifier(unshifted)
  }

  override fun serialize(encoder: Encoder, value: SiteIdentifier) {
    val shifted = value.toInt() + HALF_INT
    val unsigned = shifted.toUInt()
    val packed = unsigned.toString(RADIX).padStart(LENGTH, '0')
    encoder.encodeString(packed)
  }
}
