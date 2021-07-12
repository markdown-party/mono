package party.markdown.data.text

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray
import kotlinx.coroutines.flow.StateFlow

/** An interface defining the API to interact with a specific document. */
interface TextApi {

  /**
   * Returns the [StateFlow] of [CharArray] and associated [EventIdentifierArray] for the document
   * with the given [id].
   */
  fun current(id: EventIdentifier): StateFlow<Pair<CharArray, EventIdentifierArray>>
}
