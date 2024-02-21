package io.github.alexandrepiveteau.echo.samples.drawing.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.DrawingChange.*
import kotlinx.collections.immutable.*

/** Creates a new [PersistentDrawingBoard] instance. */
fun persistentDrawingBoardOf(): PersistentDrawingBoard =
    ActualPersistentDrawingBoard(
        available = persistentMapOf(),
        tombstones = persistentSetOf(),
    )

/** A [FigureId] uniquely identifies a figure. */
typealias FigureId = EventIdentifier

/**
 * A class representing a [Figure] in the board. A figure is uniquely identified by its [id].
 *
 * @param id the unique id for this [Figure].
 * @param color the current color of the [Figure].
 * @param offset the offset for the [Figure].
 */
@Immutable
data class Figure(
    val id: FigureId,
    val color: Color,
    val offset: DpOffset,
)

/**
 * An [ImmutableDrawingBoard] contains a [Set] of [Figure], which will be displayed to the user. The
 * returned [Figure] will all have a different [Figure.id].
 */
@Stable
interface ImmutableDrawingBoard {

  /** The [ImmutableSet] of the [Figure] from this drawing board. */
  val figures: ImmutableSet<Figure>

  /**
   * Returns the [Figure] with the given [FigureId], or null if it can't be found in the
   * [ImmutableDrawingBoard].
   */
  operator fun get(id: FigureId): Figure?
}

/**
 * A persistent variation of [ImmutableDrawingBoard], which lets users insert figures, update them
 * or remove them from the data structure. Each modification operation returns a new
 * [PersistentDrawingBoard] and does not mutate the current data structure.
 */
@Stable
interface PersistentDrawingBoard : ImmutableDrawingBoard {

  /**
   * Adds a new [Figure] with the provided [FigureId]. If the figure already exists or was deleted,
   * this will result in a no-op.
   */
  fun add(figureId: FigureId): Pair<PersistentDrawingBoard, DrawingChange>

  /**
   * Finds corresponding [Figure], and updates its color. Deleted and missing figures are ignored.
   *
   * @param figure the [FigureId] to update.
   * @param color the new color to use for the figure.
   *
   * @return an updated [PersistentDrawingBoard].
   */
  fun color(
      figure: FigureId,
      color: Color,
  ): Pair<PersistentDrawingBoard, DrawingChange>

  /**
   * Moves the corresponding [Figure] to the provided (absolute) offset. Deleted and missing figures
   * are ignored.
   *
   * @param figure the [FigureId] to update.
   * @param toX the destination x position.
   * @param toY the destination y position.
   *
   * @return an updated [PersistentDrawingBoard].
   */
  fun move(
      figure: FigureId,
      toX: Dp,
      toY: Dp,
  ): Pair<PersistentDrawingBoard, DrawingChange>

  /**
   * Removes a [Figure] from the board. This operation is idempotent. Missing figures will be marked
   * as deleted and adding them will result in no-ops.
   *
   * @param figure the [FigureId] to remove.
   *
   * @return an updated [PersistentDrawingBoard].
   */
  fun delete(
      figure: FigureId,
  ): Pair<PersistentDrawingBoard, DrawingChange>

  /**
   * Reverses a [DrawingChange], such that the [PersistentDrawingBoard] is brought back to its
   * previous state.
   */
  fun reverse(change: DrawingChange): PersistentDrawingBoard
}

/**
 * An implementation of a [PersistentDrawingBoard], backed by persistent collections.
 *
 * @param available the currently available figures.
 * @param tombstones the removed figures.
 */
@Immutable
private data class ActualPersistentDrawingBoard(
    private val available: PersistentMap<FigureId, Figure>,
    private val tombstones: PersistentSet<FigureId>,
) : PersistentDrawingBoard {

  override val figures =
      available.values.asSequence().filter { it.id !in tombstones }.toImmutableSet()

  override fun get(id: FigureId) = available[id]?.takeIf { it.id !in tombstones }

  override fun add(figureId: FigureId): Pair<PersistentDrawingBoard, DrawingChange> {
    val shouldAdd = !available.containsKey(figureId) && !tombstones.contains(figureId)
    return if (shouldAdd) {
      val figure = Figure(id = figureId, offset = DpOffset.Zero, color = Color.White)
      copy(available = available.put(figureId, figure)) to RemoveFigure(figureId)
    } else {
      this to NoOp // Ignored mutation.
    }
  }

  override fun color(figure: FigureId, color: Color): Pair<PersistentDrawingBoard, DrawingChange> {
    val existing = available[figure] ?: return this to NoOp // Ignored mutation.
    val updated = existing.copy(color = color)
    return copy(available = available.put(figure, updated)) to
        PutFigure(
            isTombstone = tombstones.contains(figure),
            figure = figure,
            atX = existing.offset.x,
            atY = existing.offset.y,
            color = existing.color,
        )
  }

  override fun move(
      figure: FigureId,
      toX: Dp,
      toY: Dp
  ): Pair<PersistentDrawingBoard, DrawingChange> {
    val existing = available[figure] ?: return this to NoOp // Ignored mutation.
    val updated = existing.copy(offset = DpOffset(toX, toY))
    return copy(available = available.put(figure, updated)) to
        PutFigure(
            isTombstone = tombstones.contains(figure),
            figure = figure,
            atX = existing.offset.x,
            atY = existing.offset.y,
            color = existing.color,
        )
  }

  override fun delete(figure: FigureId): Pair<PersistentDrawingBoard, DrawingChange> {
    val existing = available[figure] ?: return this to NoOp
    val isTombstone = tombstones.contains(figure)
    return copy(tombstones = tombstones.add(figure)) to
        PutFigure(
            isTombstone = isTombstone,
            figure = figure,
            atX = existing.offset.x,
            atY = existing.offset.y,
            color = existing.color,
        )
  }

  override fun reverse(change: DrawingChange): PersistentDrawingBoard =
      when (change) {
        NoOp -> this
        is PutFigure ->
            changePut(
                change.figure,
                change.isTombstone,
                change.atX,
                change.atY,
                change.color,
            )
        is RemoveFigure ->
            changeRemove(
                change.figure,
            )
      }

  /**
   * Puts a certain [Figure] in the board with certain properties. This will update the figure if
   * needed, moving it into and out of the tombstones.
   *
   * All the properties of the figure will be individually set at this state.
   *
   * @param figure the identifier of the figure.
   * @param isTombstone true if the figure should be marked as tombstone or not.
   * @param atX the destination x position.
   * @param atY the destination y position.
   * @param color the new color to use for the figure.
   */
  private fun changePut(
      figure: FigureId,
      isTombstone: Boolean,
      atX: Dp,
      atY: Dp,
      color: Color,
  ): PersistentDrawingBoard =
      copy(
          available = available.put(figure, Figure(figure, color, DpOffset(atX, atY))),
          tombstones = if (isTombstone) tombstones + figure else tombstones - figure,
      )

  /**
   * Removes a [Figure] from the board, but does not keep a tombstone. This operation isn't
   * idempotent.
   *
   * @param figure the [FigureId] to remove.
   *
   * @return an updated [PersistentDrawingBoard].
   */
  private fun changeRemove(
      figure: FigureId,
  ): PersistentDrawingBoard =
      copy(
          available = available - figure,
          tombstones = tombstones - figure,
      )
}
