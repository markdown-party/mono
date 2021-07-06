package party.markdown.tree

import io.github.alexandrepiveteau.echo.core.buffer.mutableEventIdentifierGapBufferOf
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier

internal class MutableEventIdentifierAssociationTable {

  /** The map that gives the association index given the event identifier. */
  private val backward = mutableMapOf<EventIdentifier, Int>()

  /** The buffer that gives the event identifier given the association identifier. */
  private val forward = mutableEventIdentifierGapBufferOf()

  /**
   * Associates the returned [Int] with the given [EventIdentifier]
   *
   * @param identifier the [EventIdentifier] for the association.
   * @return the next vertex.
   */
  fun associate(identifier: EventIdentifier): Int {
    val id = forward.size
    forward.push(identifier)
    backward[identifier] = id
    return id
  }

  /** Returns true if the given vertex has an associated [EventIdentifier]. */
  fun hasIdentifier(vertex: Int): Boolean {
    return vertex in 0 until forward.size
  }

  /** Returns the [EventIdentifier] for a certain vertex. */
  fun identifier(vertex: Int): EventIdentifier {
    return forward[vertex]
  }

  /** Returns true if the given [EventIdentifier] has an associated vertex. */
  fun hasVertex(identifier: EventIdentifier): Boolean {
    return backward.containsKey(identifier)
  }

  /** Returns the vertex for a certain [EventIdentifier]. */
  fun vertex(identifier: EventIdentifier): Int {
    return backward[identifier] ?: error("not present")
  }
}
