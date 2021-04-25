package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * A utility [KSerializer] that offers access to the underlying [JsonDecoder] when the messages are
 * serialized in Json format.
 *
 * @param T the type of the deserialized content.
 */
internal abstract class JsonSerializer<T> : KSerializer<T> {

  /** @see [deserialize] */
  abstract fun deserialize(decoder: JsonDecoder): T

  /** @see [serialize] */
  abstract fun serialize(encoder: JsonEncoder, value: T)

  override fun deserialize(decoder: Decoder): T = deserialize(decoder.asJsonDecoder())
  override fun serialize(encoder: Encoder, value: T) = serialize(encoder.asJsonEncoder(), value)
}

// BUILDING JSON

internal fun JsonObjectBuilder.put(
    key: String,
    value: UInt,
): JsonElement? = put(key, value.toInt())

internal fun JsonObjectBuilder.put(
    key: String,
    value: SequenceNumber,
    json: Json,
): JsonElement? = put(key, json.encodeToJsonElement(SequenceNumber.serializer(), value))

internal fun JsonObjectBuilder.put(
    key: String,
    value: SiteIdentifier,
    json: Json,
): JsonElement? = put(key, json.encodeToJsonElement(SiteIdentifier.serializer(), value))

// PRIVATE UTILITIES

private fun Decoder.asJsonDecoder(): JsonDecoder =
    this as? JsonDecoder ?: error("Only Json is supported")

private fun Encoder.asJsonEncoder(): JsonEncoder =
    this as? JsonEncoder ?: error("Only Json is supported")
