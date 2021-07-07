package party.markdown.tree

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier

internal class MutableEventIdentifierAssociationTable {

  /** The map that gives the association index given the event identifier. */
  private val backward = mutableMapOf<EventIdentifier, Int>()

  /** The map that gives the event identifier given the association identifier. */
  private val forward = mutableMapOf<Int, EventIdentifier>()

  /**
   * Associates the returned [Int] with the given [EventIdentifier]
   *
   * @param vertex the [Int] corresponding to the vertex identifier.
   * @param identifier the [EventIdentifier] for the association.
   */
  fun associate(vertex: Int, identifier: EventIdentifier) {
    forward[vertex] = identifier
    backward[identifier] = vertex
  }

  /**
   * Dissociates the given [vertex] and [identifier]. If they do not match the currently set values,
   * an exception will be thrown.
   *
   * @param vertex the [Int] corresponding to the vertex identifier.
   * @param identifier the [EventIdentifier] for the association.
   */
  fun dissociate(vertex: Int, identifier: EventIdentifier) {
    require(identifier == forward.remove(vertex))
    require(vertex == backward.remove(identifier))
  }

  /** Returns true if the given vertex has an associated [EventIdentifier]. */
  fun hasIdentifier(vertex: Int): Boolean {
    return forward.containsKey(vertex)
  }

  /** Returns the [EventIdentifier] for a certain vertex. */
  fun identifier(vertex: Int): EventIdentifier {
    return forward[vertex] ?: error("not present")
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
