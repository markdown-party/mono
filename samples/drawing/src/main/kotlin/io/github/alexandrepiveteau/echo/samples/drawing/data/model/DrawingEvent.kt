@file:OptIn(
    ExperimentalSerializationApi::class,
)
@file:UseSerializers(
    ColorSerializer::class,
    DpSerializer::class,
)

package io.github.alexandrepiveteau.echo.samples.drawing.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// EVENTS

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
      val toX: Dp,
      val toY: Dp,
  ) : DrawingEvent()

  /**
   * Sets the figure to a specific RGB value.
   *
   * Follows a LWW semantic.
   */
  @Serializable
  data class SetColor(
      val figure: FigureId,
      val color: Color,
  ) : DrawingEvent()

  /** Removes the figure. Additional calls to [Move] and [SetColor] will not have any effect. */
  @Serializable
  data class Delete(
      val figure: FigureId,
  ) : DrawingEvent()
}

// CUSTOM SERIALIZERS

@Serializer(forClass = Color::class)
private object ColorSerializer : KSerializer<Color> {
  override val descriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.INT)
  override fun deserialize(decoder: Decoder) = Color(decoder.decodeInt())
  override fun serialize(encoder: Encoder, value: Color) = encoder.encodeInt(value.toArgb())
}

@Serializer(forClass = Dp::class)
private object DpSerializer : KSerializer<Dp> {
  override val descriptor = PrimitiveSerialDescriptor("Dp", PrimitiveKind.FLOAT)
  override fun deserialize(decoder: Decoder) = Dp(decoder.decodeFloat())
  override fun serialize(encoder: Encoder, value: Dp) = encoder.encodeFloat(value.value)
}

// CHANGES

/**
 * A sealed class representing the different changes which are supported for the [DrawingEvent].
 * Whenever an event is applied, the change will be stored, so it can be reversed by the site if
 * needed.
 *
 * Similarly to [DrawingEvent], changes revolve around figures. They describe what operations to
 * perform on them.
 */
@Serializable
sealed class DrawingChange {

  /** A [DrawingChange] where nothing is performed. */
  @Serializable object NoOp : DrawingChange()

  /** Puts a new figure, located [atX] and [atY], with the given [color]. */
  @Serializable
  data class PutFigure(
      val isTombstone: Boolean,
      val figure: FigureId,
      val atX: Dp,
      val atY: Dp,
      val color: Color,
  ) : DrawingChange()

  /**
   * Removes a figure from the board. The figure will no longer be present in the set of available
   * figures.
   */
  @Serializable
  data class RemoveFigure(
      val figure: FigureId,
  ) : DrawingChange()
}
