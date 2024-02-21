package io.github.alexandrepiveteau.echo.webrtc.server.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * An [Actor] lets clients schedule atomic operations to be executed sequentially, with asynchronous
 * effects. This guarantees that multiple blocks scheduled in a particular order will see their
 * associated effects executed in the same order.
 */
internal interface Actor {

  /**
   * Schedules an asynchronous [block] to be executed on the [Actor], and returns a result
   * synchronously.
   *
   * @param T the type of the value produced by the block.
   * @param block the block that is executed.
   */
  suspend fun <T> schedule(block: ActorScope.() -> ActorResult<T>): T
}

/** The scope in which the block is executed. */
internal interface ActorScope {

  /**
   * Generates an [ActorResult], which indicates that the [block] has been properly scheduled.
   *
   * @param T the type of the return value of the result.
   * @param value the value that is produced in the result.
   * @param block the scheduled block.
   * @return the [ActorResult], which must be returned at the end of the [Actor.schedule] block to
   * actually schedule the [block].
   */
  fun <T> withResult(value: T, block: EffectBlock): ActorResult<T>

  /**
   * Generates an [ActorResult], which indicates that the [block] has been properly scheduled.
   *
   * @param block the scheduled block.
   * @return the [ActorResult], which must be returned at the end of the [Actor.schedule] block to
   * actually schedule the [block].
   *
   * @see withResult the underlying method, which can return a result.
   */
  fun withNoResult(block: EffectBlock): ActorResult<Unit> = withResult(Unit, block)
}

/**
 * A class representing the result of a computation.
 *
 * @param T the type of the return value.
 * @param value the return value of the computation.
 * @param block the effects of the computation, to be scheduled.
 */
internal data class ActorResult<T>(
    val value: T,
    val block: EffectBlock,
)

/**
 * The scope in which the computation block will be scheduled.
 *
 * @see CoroutineScope
 */
internal interface ActorEffectScope : CoroutineScope {

  /** A [Mutex] that guarantees exclusive access. */
  val mutex: Mutex
}

internal typealias EffectBlock = suspend ActorEffectScope.() -> Unit

/**
 * Returns a new [Actor], which uses the receiver [CoroutineScope] to schedule the execution of the
 * different blocks.
 */
internal fun CoroutineScope.actor(): Actor = CoroutineScopeActor(this)

private class CoroutineScopeActorEffectScope(
    override val mutex: Mutex,
    scope: CoroutineScope,
) : ActorEffectScope, CoroutineScope by scope

private class CoroutineScopeActor(private val scope: CoroutineScope) : Actor {

  private val mutex = Mutex()
  private val queue = Channel<EffectBlock>(UNLIMITED)

  init {
    scope.launch {
      for (block in queue) {
        coroutineScope { CoroutineScopeActorEffectScope(mutex, this).block() }
      }
    }
  }

  override suspend fun <T> schedule(block: ActorScope.() -> ActorResult<T>): T {
    return mutex.withLock {
      val result = CoroutineScopeActorScope().block()
      queue.send(result.block)
      result.value
    }
  }

  inner class CoroutineScopeActorScope : ActorScope, CoroutineScope by scope {

    override fun <T> withResult(
        value: T,
        block: EffectBlock,
    ): ActorResult<T> = ActorResult(value, block)
  }
}
