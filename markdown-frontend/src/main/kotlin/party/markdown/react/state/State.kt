package party.markdown.react.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import react.useEffectWithCleanup
import react.useState

// TODO : Use ReactJS rather than Compose terminology.

/** Returns a [CoroutineScope] which is bound to the current composition context. */
fun rememberCoroutineScope(): CoroutineScope {
  // TODO : Use a cancellable MainScope instead ?
  return GlobalScope
}

/**
 * Collects a [StateFlow] in a single value.
 *
 * @return the last emitted value.
 */
fun <T> StateFlow<T>.collectAsState(): T {
  return collectAsState(value)
}

/**
 * Collects a [Flow] in a single value.
 *
 * @param initial the initial value, that is provided until the [Flow] starts producing values.
 *
 * @return the last emitted value.
 */
fun <T> Flow<T>.collectAsState(initial: T): T {
  val (state, setState) = useState(initial)
  useEffectWithCleanup(listOf()) {
    val job = onEach { setState(it) }.launchIn(GlobalScope)
    return@useEffectWithCleanup { job.cancel() }
  }
  return state
}
