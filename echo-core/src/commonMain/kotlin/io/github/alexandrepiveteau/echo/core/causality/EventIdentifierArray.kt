package io.github.alexandrepiveteau.echo.core.causality

import kotlin.jvm.JvmInline

/** Creates an empty [EventIdentifierArray]. */
fun eventIdentifierArrayOf(): EventIdentifierArray = EventIdentifierArray(longArrayOf())

// TODO : Document this class.
@JvmInline
value class EventIdentifierArray
internal constructor(
    internal val backing: LongArray,
) {

  constructor(size: Int) : this(LongArray(size))

  val size: Int
    get() = backing.size

  operator fun get(index: Int): EventIdentifier = EventIdentifier(backing[index])
  operator fun set(index: Int, value: EventIdentifier): Unit = backing.set(index, value.packed)
  operator fun iterator(): EventIdentifierIterator = ActualEventIdentifier(backing.iterator())

  @JvmInline
  private value class ActualEventIdentifier(
      private val backing: LongIterator,
  ) : EventIdentifierIterator {
    override fun hasNext(): Boolean = backing.hasNext()
    override fun nextEventIdentifier(): EventIdentifier = EventIdentifier(backing.nextLong())
  }
}

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
