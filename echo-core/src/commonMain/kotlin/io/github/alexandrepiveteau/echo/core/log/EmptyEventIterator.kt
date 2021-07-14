package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier

internal object EmptyEventIterator : EventIterator {

  override val seqno: SequenceNumber = error("Empty")
  override val site: SiteIdentifier = error("Empty")
  override val event: ByteArray = error("Empty")
  override val from: Int = error("Empty")
  override val until: Int = error("Empty")

  override fun hasNext() = false
  override fun hasPrevious() = false

  override fun nextIndex(): Int = error("Empty")
  override fun previousIndex(): Int = error("Empty")

  override fun moveNext() = error("Empty")
  override fun movePrevious() = error("Empty")
}
