@file:Suppress("unused")

package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.protocol.Message
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.json.*

/**
 * Returns a [KSerializer] that supports [Message.Incoming].
 *
 * @param elementSerializer the [KSerializer] to use for the embedded events.
 */
fun <T> Message.Incoming.Companion.serializer(
    elementSerializer: KSerializer<T>,
): KSerializer<Message.Incoming<T>> = IncomingSerializer(elementSerializer)

/** Returns a [KSerializer] that supports [Message.Outgoing]. */
fun <T> Message.Outgoing.Companion.serializer(): KSerializer<Message.Outgoing<T>> =
    OutgoingSerializer()

// HAND-CRAFTED IMPLEMENTATIONS

private fun badSerial(): Nothing = throw SerializationException("Badly formatted message.")

private class IncomingSerializer<T>(
    private val elementSerializer: KSerializer<T>,
) : JsonSerializer<Message.Incoming<T>>() {

  private companion object {

    private const val KeyType = "type"
    private const val KeySite = "site"
    private const val KeyNextSeqno = "nextSeqno"
    private const val KeySeqno = "seqno"
    private const val KeyEvent = "event"

    private const val TypeAdv = "advertisement"
    private const val TypeReady = "ready"
    private const val TypeEvent = "event"
  }

  override val descriptor: SerialDescriptor
    get() = buildClassSerialDescriptor("Incoming") { element<String>(KeyType) }

  override fun deserialize(decoder: JsonDecoder): Message.Incoming<T> {
    val json = decoder.json
    val element = decoder.decodeJsonElement()
    return when (element.jsonObject[KeyType]?.jsonPrimitive?.contentOrNull) {
      TypeAdv -> {
        val site = element.jsonObject[KeySite] ?: badSerial()
        val seqno = element.jsonObject[KeyNextSeqno] ?: badSerial()
        Message.Incoming.Advertisement(
            json.decodeFromJsonElement(SiteIdentifier.serializer(), site),
            json.decodeFromJsonElement(SequenceNumber.serializer(), seqno),
        )
      }
      TypeReady -> Message.Incoming.Ready
      TypeEvent -> {
        val site = element.jsonObject[KeySite] ?: badSerial()
        val seqno = element.jsonObject[KeySeqno] ?: badSerial()
        val body = element.jsonObject[KeyEvent] ?: badSerial()
        Message.Incoming.Event(
            site = json.decodeFromJsonElement(SiteIdentifier.serializer(), site),
            seqno = json.decodeFromJsonElement(SequenceNumber.serializer(), seqno),
            body = json.decodeFromJsonElement(elementSerializer, body))
      }
      else -> badSerial()
    }
  }

  override fun serialize(encoder: JsonEncoder, value: Message.Incoming<T>) {
    val json = encoder.json
    when (value) {
      is Message.Incoming.Advertisement ->
          encoder.encodeJsonElement(
              buildJsonObject {
                put(KeyType, TypeAdv)
                put(KeySite, value.site, json)
                put(KeyNextSeqno, value.nextSeqno, json)
              },
          )
      Message.Incoming.Ready ->
          encoder.encodeJsonElement(buildJsonObject { put(KeyType, JsonPrimitive(TypeReady)) })
      is Message.Incoming.Event ->
          encoder.encodeJsonElement(
              buildJsonObject {
                put(KeyType, TypeEvent)
                put(KeySite, value.site, json)
                put(KeySeqno, value.seqno, json)
                put(KeyEvent, json.encodeToJsonElement(elementSerializer, value.body))
              })
    }
  }
}

private class OutgoingSerializer<T> : JsonSerializer<Message.Outgoing<T>>() {

  private companion object {

    private const val KeyType = "type"
    private const val KeySite = "site"
    private const val KeyNextSeqno = "nextSeqno"
    private const val KeyCount = "count"

    private const val TypeAcknowledge = "acknowledge"
    private const val TypeRequest = "request"
  }

  override val descriptor: SerialDescriptor
    get() = buildClassSerialDescriptor("Outgoing") { element<String>(KeyType) }

  override fun deserialize(decoder: JsonDecoder): Message.Outgoing<T> {
    val json = decoder.json
    val element = decoder.decodeJsonElement()
    return when (element.jsonObject[KeyType]?.jsonPrimitive?.contentOrNull) {
      TypeAcknowledge -> {
        val site = element.jsonObject[KeySite] ?: badSerial()
        val nextForSite = element.jsonObject[KeyNextSeqno] ?: badSerial()
        Message.Outgoing.Acknowledge(
            site = json.decodeFromJsonElement(SiteIdentifier.serializer(), site),
            nextSeqno = json.decodeFromJsonElement(SequenceNumber.serializer(), nextForSite),
        )
      }
      TypeRequest -> {
        val site = element.jsonObject[KeySite] ?: badSerial()
        val count = element.jsonObject[KeyCount] ?: badSerial()
        Message.Outgoing.Request(
            site = json.decodeFromJsonElement(SiteIdentifier.serializer(), site),
            count = count.jsonPrimitive.intOrNull?.toUInt() ?: badSerial(),
        )
      }
      else -> badSerial()
    }
  }

  override fun serialize(encoder: JsonEncoder, value: Message.Outgoing<T>) {
    val json = encoder.json
    when (value) {
      is Message.Outgoing.Acknowledge ->
          encoder.encodeJsonElement(
              buildJsonObject {
                put(KeyType, TypeAcknowledge)
                put(KeyNextSeqno, value.nextSeqno, json)
                put(KeySite, value.site, json)
              })
      is Message.Outgoing.Request ->
          encoder.encodeJsonElement(
              buildJsonObject {
                put(KeyType, TypeRequest)
                put(KeyCount, value.count)
                put(KeySite, value.site, json)
              })
    }
  }
}
