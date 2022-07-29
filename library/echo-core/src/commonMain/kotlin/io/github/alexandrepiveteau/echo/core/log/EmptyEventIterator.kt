package io.github.alexandrepiveteau.echo.core.log

/** An implementation of [ListIterator] which has no elements. */
internal object EmptyListIterator : ListIterator<Nothing> {

  override fun hasNext() = false
  override fun hasPrevious() = false
  override fun next(): Nothing = empty()
  override fun nextIndex(): Int = empty()
  override fun previous(): Nothing = empty()
  override fun previousIndex(): Int = empty()

  /** Throws an [IllegalStateException] indicating that the iterator is empty. */
  private fun empty(): Nothing = error("Can't retrieve an element from an empty EventIterator.")
}
