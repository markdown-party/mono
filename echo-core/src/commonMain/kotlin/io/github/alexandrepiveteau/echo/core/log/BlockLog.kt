package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.buffer.*
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.requireIn
import io.github.alexandrepiveteau.echo.core.requireRange

/**
 * A [BlockLog] manages a sequence of blocks of bytes events or changes. It stores them in a linear
 * fashion, and associates them a size and a unique identifier. Additionally, it supports traversing
 * the data structure using iterators, and finding out whether a given element exists or not fast.
 */
internal class BlockLog {

  private val blocks = mutableByteGapBufferOf()
  private val blocksIds = mutableEventIdentifierGapBufferOf()
  private val blocksSizes = mutableIntGapBufferOf()

  /** Returns the count of items in the [BlockLog]. */
  val size: Int
    get() = blocksSizes.size

  /**
   * The [ByteArray] backing this [BlockLog], which may be exposed for direct access without copying
   * the underlying bytes.
   */
  val backing: ByteArray
    get() = blocks.backing

  // BEGIN : POSITIONS

  val hasPrevious: Boolean
    get() = blocksIds.gap.startIndex > 0

  val hasNext: Boolean
    get() = blocksIds.gap.startIndex < blocksIds.size

  // END : POSITION

  // BEGIN : LAST GAP POSITION

  val lastFrom: Int
    get() = blocks.gap.startIndex - lastSize

  val lastUntil: Int
    get() = blocks.gap.startIndex

  val lastId: EventIdentifier
    get() = blocksIds[blocksIds.gap.startIndex - 1]

  val lastSize: Int
    get() = blocksSizes[blocksSizes.gap.startIndex - 1]

  // END : LAST GAP POSITION

  /** Removes the previous item to the left. */
  fun removeLeft() {
    check(hasPrevious) { "Can't remove left when at first index." }
    blocks.remove(blocks.gap.startIndex - lastSize, lastSize)
    blocksIds.remove(blocksIds.gap.startIndex - 1)
    blocksSizes.remove(blocksSizes.gap.startIndex - 1)
  }

  /** Moves the cursor to the left by one step. */
  fun moveLeft() {
    check(hasPrevious) { "Can't move left when at first index." }
    blocks.gap.shift(-lastSize)
    blocksIds.gap.shift(-1)
    blocksSizes.gap.shift(-1)
  }

  /** Moves the cursor to the right by one step. */
  fun moveRight() {
    check(hasNext) { "Can't move right when at last index." }
    blocksIds.gap.shift(1)
    blocksSizes.gap.shift(1) // Updating lastSize for blocks.gap shift.
    blocks.gap.shift(lastSize)
  }

  private fun moveToIndex(insertion: Int) {
    requireIn(insertion, 0, blocksIds.size + 1)
    while (blocksIds.gap.startIndex > insertion) moveLeft()
    while (blocksIds.gap.startIndex < insertion) moveRight()
  }

  /**
   * Pushes the given [ByteArray] at the gap position which better fits their event identifier.
   *
   * @param id the [EventIdentifier] to push.
   * @param array the [ByteArray] to push.
   * @param from the start of the array range.
   * @param until the end of the array range.
   */
  fun pushAtId(
      id: EventIdentifier,
      array: ByteArray,
      from: Int = 0,
      until: Int = array.size,
  ) {
    val index = blocksIds.binarySearch(id)
    if (index >= 0) return // Already present.
    val insertion = -(index + 1)
    moveToIndex(insertion)
    pushAtGap(id, array, from, until)
  }

  /**
   * Pushes the given [ByteArray] at the current gap position, and moves the gap after the inserted
   * data.
   *
   * @param id the [EventIdentifier] to push.
   * @param array the [ByteArray] to push.
   * @param from the start of the array range.
   * @param until the end of the array range.
   */
  fun pushAtGap(
      id: EventIdentifier,
      array: ByteArray,
      from: Int = 0,
      until: Int = array.size,
  ) {
    requireRange(from, until, array) { "data out of range" }
    blocks.pushAtGap(array, from, until)
    blocksIds.pushAtGap(id)
    blocksSizes.pushAtGap(until - from)
  }

  /**
   * Pushes the given [ByteArray] at the current gap position, but moves to the original insertion
   * point after.
   *
   * @param id the [EventIdentifier] to push.
   * @param array the [ByteArray] to push.
   * @param from the start of the array range.
   * @param until the end of the array range.
   */
  fun pushAtGapWithoutMove(
      id: EventIdentifier,
      array: ByteArray,
      from: Int = 0,
      until: Int = array.size,
  ) {
    pushAtGap(id, array, from, until)
    blocks.gap.shift(-(until - from))
    blocksIds.gap.shift(-1)
    blocksSizes.gap.shift(-1)
  }

  /** Clears the [BlockLog], such that it becomes completely empty. */
  fun clear() {
    blocks.clear()
    blocksIds.clear()
    blocksSizes.clear()
  }

  /**
   * An inner class which can be used to iterate over the items from a [BlockLog], providing
   * list-like access to the items of the [BlockLog].
   */
  inner class Iterator : EventIterator {

    /**
     * The current position in the event identifiers log. This lets us know "which" event we're
     * pointing at.
     */
    private var cursorIdsIndex: Int = blocksIds.size

    /**
     * The current position in the event sizes identifiers. This lets us know "what data" the
     * currently pointed event contains, since not all events have an identical size.
     */
    private var cursorEvents: Int = blocks.size

    override val seqno: SequenceNumber
      get() = blocksIds[cursorIdsIndex].seqno

    override val site: SiteIdentifier
      get() = blocksIds[cursorIdsIndex].site

    override val event: ByteArray
      get() = blocks.backing

    override val from: Int
      get() = cursorEvents

    override val until: Int
      get() = from + blocksSizes[cursorIdsIndex]

    override fun hasNext(): Boolean {
      return cursorIdsIndex < blocksIds.size - 1
    }

    override fun hasPrevious(): Boolean {
      return cursorIdsIndex > 0
    }

    override fun nextIndex(): Int {
      check(hasNext()) { "No next element." }
      return cursorIdsIndex + 1
    }

    override fun previousIndex(): Int {
      check(hasPrevious()) { "No previous element." }
      return cursorIdsIndex - 1
    }

    override fun moveNext() {
      check(hasNext()) { "No next element" }
      cursorEvents += blocksSizes[cursorIdsIndex]
      cursorIdsIndex++
    }

    override fun movePrevious() {
      check(hasPrevious()) { "No previous element." }
      cursorIdsIndex--
      cursorEvents -= blocksSizes[cursorIdsIndex]
    }
  }
}
