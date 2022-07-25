package party.markdown.data.project

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * An interface defining the API to interact with the project. This includes renaming the project,
 * and getting its current name.
 */
interface ProjectApi {

  /** Returns a [StateFlow] with the latest value for the project name. */
  val currentName: Flow<String>

  /**
   * Sets the name of the project. On concurrent modifications, the [value] may be ignored and
   * replaced with the concurrent call to [name].
   *
   * @param value the [String] to use as the new name
   */
  suspend fun name(value: String)
}
