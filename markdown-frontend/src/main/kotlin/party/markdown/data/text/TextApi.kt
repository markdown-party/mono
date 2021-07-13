package party.markdown.data.text

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray
import io.github.alexandrepiveteau.echo.events.EventScope
import kotlinx.coroutines.flow.StateFlow
import party.markdown.rga.RGAEvent
import party.markdown.tree.TreeNodeIdentifier

/** An interface defining the API to interact with a specific document. */
interface TextApi {

  /**
   * Returns the [StateFlow] of [CharArray] and associated [EventIdentifierArray] for the document
   * with the given [id].
   */
  fun current(id: TreeNodeIdentifier): StateFlow<Pair<CharArray, EventIdentifierArray>>

  // TODO : Document this.
  suspend fun edit(id: TreeNodeIdentifier, scope: suspend EventScope<RGAEvent>.() -> Unit)
}
