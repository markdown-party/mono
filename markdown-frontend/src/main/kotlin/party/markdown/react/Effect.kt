@file:Suppress("FunctionName")

package party.markdown.react

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
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
 * @param context the [CoroutineContext] on which collection occurs.
 */
fun <T> useFlow(
    flow: StateFlow<T>,
    context: CoroutineContext = EmptyCoroutineContext,
): T {
  return useFlow(initial = flow.value, flow = flow, context = context)
}

/**
 * Returns the latest emitted value of the given [Flow], starting with the [initial] value.
 *
 * @param initial the initial value of the effect.
 * @param flow the [Flow] that is collected.
 * @param context the [CoroutineContext] on which collection occurs.
 */
fun <T> useFlow(
    initial: T,
    flow: Flow<T>,
    context: CoroutineContext = EmptyCoroutineContext,
): T {
  val (state, setState) = useState(initial)
  useLaunchedEffect(listOf(flow, context)) {
    if (context == EmptyCoroutineContext) {
      flow.collect { setState(it) }
    } else {
      withContext(context) { flow.collect { setState(it) } }
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
