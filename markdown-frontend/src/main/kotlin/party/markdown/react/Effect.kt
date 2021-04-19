@file:Suppress("FunctionName")

package party.markdown.react

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import react.RDependenciesList
import react.useEffectWithCleanup
import react.useState

/** A hook that returns a [CoroutineScope]. */
fun useCoroutineScope(): CoroutineScope {
  val (scope, _) = useState { MainScope() }
  useEffectWithCleanup(listOf()) {
    return@useEffectWithCleanup { scope.cancel() }
  }
  return scope
}

/**
 * Returns the latest emitted value of the given [StateFlow].
 *
 * @param flow the [StateFlow] that is collected.
 */
fun <T> useFlow(flow: StateFlow<T>): T {
  return useFlow(initial = flow.value, flow)
}

/**
 * Returns the latest emitted value of the given [Flow], starting with the [initial] value.
 *
 * @param initial the initial value of the effect.
 * @param flow the [Flow] that is collected.
 */
fun <T> useFlow(initial: T, flow: Flow<T>): T {
  val (state, setState) = useState(initial)
  useEffectWithCleanup(listOf()) {
    val scope = MainScope()
    val job = flow.onEach { setState(it) }.launchIn(scope)
    return@useEffectWithCleanup {
      scope.cancel()
      job.cancel()
    }
  }
  return state
}

/**
 * Runs the provided suspending [body] in a coroutine.
 *
 * @param dependencies the dependencies for this effect.
 * @param body thee suspending body to be run.
 */
fun useLaunchedEffect(
    dependencies: RDependenciesList? = null,
    body: suspend () -> Unit,
) {
  useEffectWithCleanup(dependencies) {
    val scope = MainScope()
    scope.launch { body() }
    return@useEffectWithCleanup { scope.cancel() }
  }
}
