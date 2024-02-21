package io.github.alexandrepiveteau.echo.protocol

/**
 * An exception that will be thrown when the protocol terminates gracefully. It will be caught and
 * won't be propagated to the callers of the exchanges.
 */
internal class TerminationException : RuntimeException()

/** Terminates the protocol gracefully. */
internal fun terminate(): Nothing = throw TerminationException()

/** Runs the [body], catching termination exceptions. */
internal inline fun runCatchingTermination(body: () -> Unit) {
  try {
    body()
  } catch (_: TerminationException) {}
}
