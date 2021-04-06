@file:UseSerializers(EventIdentifierSerializer::class)

package io.github.alexandrepiveteau.echo.samples.drawing.data.model

import io.github.alexandrepiveteau.echo.causal.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * A sealed class representing the different events which are supported in the drawing app, and
 * which can be performed by end users.
 *
 * In this case, events revolve around figures. Figures can be created, moved and see their color
 * changed.
 */
@Serializable
sealed class DrawingEvent {

  /**
   * Adds a new figure, which will be identified by a [FigureId]. The [FigureId] is generated when
   * the figure event is created.
   *
   * When created, a figure has a white color and is centered with offset (0, 0).
   */
  @Serializable object AddFigure : DrawingEvent()

  /**
   * Moves a figure to a certain relative offset from the center.
   *
   * Follows a LWW semantic.
   */
  @Serializable
  data class Move(
      val figure: FigureId,
      val toX: Int,
      val toY: Int,
  ) : DrawingEvent()

  /**
   * Sets the figure to a specific RGB value.
   *
   * Follows a LWW semantic.
   */
  @Serializable
  data class SetColor(
      val figure: FigureId,
      val color: Int,
  ) : DrawingEvent()

  /** Removes the figure. Additional calls to [Move] and [SetColor] will not have any effect. */
  @Serializable
  data class Delete(
      val figure: FigureId,
  ) : DrawingEvent()
}

// CUSTOM SERIALIZERS

@Serializer(forClass = EventIdentifier::class)
private object EventIdentifierSerializer : KSerializer<EventIdentifier> {

  override val descriptor =
      buildClassSerialDescriptor("EventIdentifier") {
        element<Int>("site")
        element<Int>("seqno")
      }

  override fun deserialize(decoder: Decoder): EventIdentifier {
    return decoder.decodeStructure(descriptor) {
      var site: Int? = null
      var seqno: Int? = null

      loop@ while (true) {
        when (val index = decodeElementIndex(descriptor)) {
          DECODE_DONE -> break@loop
          0 -> site = decodeIntElement(descriptor, 0)
          1 -> seqno = decodeIntElement(descriptor, 1)
          else -> error("Unexpected index $index.")
        }
      }

      EventIdentifier(
          SequenceNumber(requireNotNull(seqno).toUInt()),
          SiteIdentifier(requireNotNull(site)),
      )
    }
  }

  override fun serialize(encoder: Encoder, value: EventIdentifier) {
    encoder.encodeStructure(descriptor) {
      encodeIntElement(descriptor, 0, value.site.toInt())
      encodeIntElement(descriptor, 1, value.seqno.toUInt().toInt())
    }
  }
}
