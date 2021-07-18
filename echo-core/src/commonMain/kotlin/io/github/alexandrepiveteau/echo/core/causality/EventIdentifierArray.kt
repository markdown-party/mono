@file:OptIn(ExperimentalUnsignedTypes::class)

package io.github.alexandrepiveteau.echo.core.causality

/** Creates an empty [EventIdentifierArray]. */
fun eventIdentifierArrayOf(): EventIdentifierArray = EventIdentifierArray(ulongArrayOf())

/**
 * An array of event identifiers. When targeting the JVM, instances of this class are represented as
 * `long[]`.
 */
// TODO : Wait for a fix of KT-35749, KT-39696
/* @kotlin.jvm.JvmInline value */
class EventIdentifierArray
internal constructor(
    internal val backing: ULongArray,
) {

  /**
   * Creates a new array of the specified [size], with all elements initialized to
   * [EventIdentifier.Unspecified].
   */
  constructor(size: Int) : this(ULongArray(size))

  /** Returns the number of elements in the array. */
  val size: Int
    get() = backing.size

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is EventIdentifierArray) return false
    return this.backing == other.backing
  }

  override fun hashCode(): Int {
    return backing.hashCode()
  }

  operator fun contains(identifier: EventIdentifier): Boolean {
    for (i in 0 until size) {
      if (get(i) == identifier) return true
    }
    return false
  }

  /**
   * Returns the array element at the given [index]. This method can be called using the index
   * operator.
   *
   * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException] except in
   * Kotlin/JS where the behavior is unspecified.
   */
  operator fun get(index: Int): EventIdentifier = EventIdentifier(backing[index])

  /**
   * Sets the element at the given [index] to the given [value]. This method can be called using the
   * index operator.
   *
   * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException] except in
   * Kotlin/JS where the behavior is unspecified.
   */
  operator fun set(index: Int, value: EventIdentifier): Unit = backing.set(index, value.packed)

  /** Creates an iterator over the elements of the array. */
  operator fun iterator(): EventIdentifierIterator =
      ActualEventIdentifier(backing.asLongArray().iterator())

  override fun toString(): String {
    return backing.joinToString(
        prefix = "[",
        postfix = "]",
    ) { EventIdentifier(it).toString() }
  }

  private class ActualEventIdentifier(
      private val backing: LongIterator,
  ) : EventIdentifierIterator() {
    override fun hasNext(): Boolean = backing.hasNext()
    override fun nextEventIdentifier(): EventIdentifier =
        EventIdentifier(backing.nextLong().toULong())
  }
}

/**
 * Copies this array or its subrange into the [destination] array and returns that array.
 *
 * It's allowed to pass the same array in the [destination] and even specify the subrange so that it
 * overlaps with the destination range.
 *
 * @param destination the array to copy to.
 * @param destinationOffset the position in the [destination] array to copy to, 0 by default.
 * @param startIndex the beginning (inclusive) of the subrange to copy, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to copy, size of this array by default.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex]
 * is out of range of this array indices or when `startIndex > endIndex`.
 * @throws IndexOutOfBoundsException when the subrange doesn't fit into the [destination] array
 * starting at the specified [destinationOffset], or when that index is out of the [destination]
 * array indices range.
 *
 * @return the [destination] array.
 */
fun EventIdentifierArray.copyInto(
    destination: EventIdentifierArray,
    destinationOffset: Int = 0,
    startIndex: Int = 0,
    endIndex: Int = size,
): EventIdentifierArray =
    EventIdentifierArray(
        backing.copyInto(
            destination = destination.backing,
            destinationOffset = destinationOffset,
            startIndex = startIndex,
            endIndex = endIndex,
        ),
    )

/** Returns a typed object array containing all of the elements of this primitive array. */
fun EventIdentifierArray.toTypedArray(): Array<EventIdentifier> {
  return Array(size, this::get)
}

/** Returns a primitive array from a generic [Array] of [EventIdentifier]. */
fun Array<EventIdentifier>.toEventIdentifierArray(): EventIdentifierArray {
  val array = EventIdentifierArray(size)
  for (i in 0 until size) array[i] = get(i)
  return array
}

/** Sorts the array in-place. */
fun EventIdentifierArray.sort(): Unit = backing.sort() // ULong quicksort.
