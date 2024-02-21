package io.github.alexandrepiveteau.echo.core.log.tree

import io.github.alexandrepiveteau.echo.core.buffer.MutableByteGapBuffer
import io.github.alexandrepiveteau.echo.core.buffer.mutableByteGapBufferOf
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.Event
import io.github.alexandrepiveteau.echo.core.log.EventIterator
import kotlin.collections.Map.Entry

/**
 * An implementation of [EventIterator] which makes use of a [ListIterator] of [Entry].
 *
 * @property iterator the underlying [ListIterator].
 */
internal class ListIteratorDecorator(
    private val iterator: ListIterator<Entry<EventIdentifier, ByteArray>>,
) : EventIterator {

  override fun hasNext() = iterator.hasNext()
  override fun hasPrevious() = iterator.hasPrevious()
  override fun next(): Event {
    val (k, v) = iterator.next()
    return Event(k.seqno, k.site, v)
  }
  override fun nextIndex() = iterator.nextIndex()
  override fun previous(): Event {
    val (k, v) = iterator.previous()
    return Event(k.seqno, k.site, v)
  }
  override fun previousIndex() = iterator.previousIndex()
  override fun moveNext() {
    iterator.next()
  }
  override fun movePrevious() {
    iterator.previous()
  }

  /** Runs the given [block] with the [next] value. */
  private inline fun <R> withNext(block: Entry<EventIdentifier, ByteArray>.() -> R): R {
    val result = block(iterator.next())
    iterator.previous()
    return result
  }

  /** Runs the given [block] with the [previous] value. */
  private inline fun <R> withPrevious(block: Entry<EventIdentifier, ByteArray>.() -> R): R {
    val result = block(iterator.previous())
    iterator.next()
    return result
  }
  override val previousSeqno: SequenceNumber
    get() = withPrevious { key.seqno }
  override val previousSite: SiteIdentifier
    get() = withPrevious { key.site }
  override val previousEvent: MutableByteGapBuffer
    get() = withPrevious { mutableByteGapBufferOf().apply { push(value) } }
  override val previousFrom: Int
    get() = 0
  override val previousUntil: Int
    get() = withPrevious { value.size }

  override val nextSeqno: SequenceNumber
    get() = withNext { key.seqno }
  override val nextSite: SiteIdentifier
    get() = withNext { key.site }
  override val nextEvent: MutableByteGapBuffer
    get() = withNext { mutableByteGapBufferOf().apply { push(value) } }
  override val nextFrom: Int
    get() = 0
  override val nextUntil: Int
    get() = withNext { value.size }
}
