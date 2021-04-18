@file:Suppress("FunctionName")

package party.markdown.react.state

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import react.useEffectWithCleanup

// TODO : Make this ReactJS-friendlier.

/** An object which defines a scope for an effect. */
object DisposableEffectScope {

  /** Defines an effect to be run when the component is disposed. */
  inline fun onDispose(
      crossinline onDisposeEffect: () -> Unit,
  ): DisposableEffectResult =
      object : DisposableEffectResult {
        override fun dispose() {
          onDisposeEffect()
        }
      }
}

/** A result type for effects to be run when disposed. */
interface DisposableEffectResult {
  fun dispose()
}

/** Creates an effect which will be cleaned up when the component is disposed. */
fun DisposableEffect(
    body: DisposableEffectScope.() -> DisposableEffectResult,
) {
  useEffectWithCleanup(listOf()) {
    val result = body(DisposableEffectScope)
    return@useEffectWithCleanup { result.dispose() }
  }
}

/** Creates an effect which can run a suspending function as its body. */
fun LaunchedEffect(
    body: suspend () -> Unit,
) {
  DisposableEffect {
    val scope = MainScope()
    scope.launch { body() }
    onDispose { scope.cancel() }
  }
}
