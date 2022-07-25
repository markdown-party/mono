package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.buffer.*
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.requireRange

/** A special sentinel value which indicates that an index is not set. */
private const val NoIndex = -1

/**
 * A [BlockLog] manages a sequence of blocks of bytes events or changes. It stores them in a linear
 * fashion, and associates them a size and a unique identifier. Additionally, it supports traversing
 * the data structure using iterators, and insertions using iterators.
 *
 * However, [iterator] and [iteratorAtEnd] can't handle concurrent iterators : only the last created
 * [MutableEventIterator] should be used, otherwise a [ConcurrentModificationException] will be
 * thrown.
 */
internal class BlockLog {

  /** The buffer with the blocks of events, positioned contiguously. */
  private val blocks = mutableByteGapBufferOf()

  /** The buffer with the identifiers of the events. */
  private val blockIds = mutableEventIdentifierGapBufferOf()

  /** The buffer with the sizes of the [blocks]. */
  private val blockSizes = mutableIntGapBufferOf()

  /** A token which uniquely identifies the current iterator. */
  private var currentIteratorToken = 0

  /** Returns the size of this [BlockLog]. */
  val size: Int
    get() = blockIds.size

  /** Returns a new [Iterator] instance, at the start of the [BlockLog]. */
  fun iterator(): MutableEventIterator = Iterator(cursorNextBlock = 0, cursorNextIndex = 0)

  /** Returns a new [Iterator] instance, at the end of the [BlockLog]. */
  fun iteratorAtEnd(): MutableEventIterator =
      Iterator(cursorNextBlock = blocks.size, cursorNextIndex = blockIds.size)

  /** Clears the [BlockLog] and removes all the inner events, invalidating all the iterators. */
  fun clear() {
    blocks.clear()
    blockIds.clear()
    blockSizes.clear()
    currentIteratorToken++
  }

  /**
   * An implementation of [EventIterator], which may be used as long as the [BlockLog] instance has
   * not been mutated.
   *
   * @param token the iterator token which is used.
   * @param cursorNextIndex the index of the element which would be returned by a call to [next].
   * @param cursorNextBlock the index of the data which would be returned by a call to [next].
   */
  internal inner class Iterator(
      private val token: Int = ++currentIteratorToken,
      var cursorNextIndex: Int,
      var cursorNextBlock: Int,
  ) : MutableEventIterator {

    /** Checks the iterator token. */
    private fun checkToken() {
      if (token != currentIteratorToken) {
        throw ConcurrentModificationException("BlockLog does not support concurrent iterators.")
      }
    }

    override fun add(
        seqno: SequenceNumber,
        site: SiteIdentifier,
        event: ByteArray,
        from: Int,
        until: Int
    ) {
      checkToken()
      requireRange(from, until, event)
      blocks.push(array = event, offset = cursorNextBlock, startIndex = from, endIndex = until)
      blockIds.push(value = EventIdentifier(seqno, site), offset = cursorNextIndex)
      blockSizes.push(value = until - from, offset = cursorNextIndex)
      cursorNextBlock += (until - from)
      cursorNextIndex += 1
      lastCursorNextBlock = NoIndex
      lastCursorNextIndex = NoIndex
    }

    override fun add(element: Event) = add(element.seqno, element.site, element.data)

    private var lastCursorNextIndex = NoIndex
    private var lastCursorNextBlock = NoIndex

    override fun set(element: Event) {
      checkToken()
      check(lastCursorNextBlock != NoIndex)
      check(lastCursorNextIndex != NoIndex)
      blocks.remove(lastCursorNextBlock, blockSizes[lastCursorNextIndex])
      blocks.push(element.data, offset = lastCursorNextBlock)
      blockSizes[lastCursorNextIndex] = element.data.size
      blockIds[lastCursorNextIndex] = EventIdentifier(element.seqno, element.site)
      lastCursorNextIndex = NoIndex
      lastCursorNextBlock = NoIndex
    }

    override fun remove() {
      checkToken()
      check(lastCursorNextBlock != NoIndex)
      check(lastCursorNextBlock != NoIndex)
      blocks.remove(lastCursorNextBlock, blockSizes[lastCursorNextIndex])
      blockSizes.remove(lastCursorNextIndex, 1)
      blockIds.remove(lastCursorNextIndex, 1)
      lastCursorNextIndex = NoIndex
      lastCursorNextBlock = NoIndex
    }

    override val previousSeqno: SequenceNumber
      get() {
        check(hasPrevious())
        return blockIds[previousIndex()].seqno
      }
    override val previousSite: SiteIdentifier
      get() {
        check(hasPrevious())
        return blockIds[previousIndex()].site
      }
    override val previousEvent: MutableByteGapBuffer
      get() {
        check(hasPrevious())
        return blocks
      }
    override val previousFrom: Int
      get() {
        check(hasPrevious())
        return cursorNextBlock - blockSizes[previousIndex()]
      }
    override val previousUntil: Int
      get() {
        check(hasPrevious())
        return cursorNextBlock
      }

    override val nextSeqno: SequenceNumber
      get() {
        check(hasNext())
        return blockIds[nextIndex()].seqno
      }
    override val nextSite: SiteIdentifier
      get() {
        check(hasNext())
        return blockIds[nextIndex()].site
      }
    override val nextEvent: MutableByteGapBuffer
      get() {
        check(hasNext())
        return blocks
      }
    override val nextFrom: Int
      get() {
        check(hasNext())
        return cursorNextBlock
      }
    override val nextUntil: Int
      get() {
        check(hasNext())
        return cursorNextBlock + blockSizes[nextIndex()]
      }

    override fun next(): Event {
      check(hasNext())
      moveNext()
      return Event(
          seqno = previousSeqno,
          site = previousSite,
          data = previousEvent.copyOfRange(previousFrom, previousUntil),
      )
    }
    override fun previous(): Event {
      check(hasPrevious())
      movePrevious()
      return Event(
          seqno = nextSeqno,
          site = nextSite,
          data = nextEvent.copyOfRange(nextFrom, nextUntil),
      )
    }
    override fun moveNext() {
      check(hasNext())
      lastCursorNextIndex = cursorNextIndex
      lastCursorNextBlock = cursorNextBlock
      cursorNextBlock += blockSizes[nextIndex()]
      cursorNextIndex += 1
    }
    override fun movePrevious() {
      check(hasPrevious())
      cursorNextBlock -= blockSizes[previousIndex()]
      cursorNextIndex -= 1
      lastCursorNextIndex = cursorNextIndex
      lastCursorNextBlock = cursorNextBlock
    }
    override fun hasNext(): Boolean {
      checkToken()
      return blockIds.size > cursorNextIndex
    }
    override fun hasPrevious(): Boolean {
      checkToken()
      return cursorNextIndex > 0
    }
    override fun nextIndex(): Int {
      check(hasNext())
      return cursorNextIndex
    }
    override fun previousIndex(): Int {
      check(hasPrevious())
      return cursorNextIndex - 1
    }
  }
}
