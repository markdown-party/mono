package io.github.alexandrepiveteau.echo.webrtc.server.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// TODO : Rename this to Actor.

/**
 * A utility interface which schedules atomic operations to be executed sequentially. This
 * guarantees that multiple blocks scheduled in a particular order will see their associated tasks
 * executed in the same order.
 */
internal interface InOrder {

  /**
   * Schedules a [block] to be executed.
   *
   * @param T the type of the value produced by the block.
   * @param block the block that is executed.
   */
  suspend fun <T> schedule(block: InOrderScope.() -> InOrderResult<T>): T
}

/** The scope in which the block is executed. */
internal interface InOrderScope {

  /**
   * Generates an [InOrderResult], which indicates that the [block] has been properly scheduled.
   *
   * @param T the type of the return value of the result.
   * @param value the value that is produced in the result.
   * @param block the scheduled block.
   * @return the [InOrderResult], which must be returned at the end of the [InOrder.schedule] block
   * to actually schedule the [block].
   */
  fun <T> withResult(value: T, block: ScheduledBlock): InOrderResult<T>

  /**
   * Generates an [InOrderResult], which indicates that the [block] has been properly scheduled.
   *
   * @param block the scheduled block.
   * @return the [InOrderResult], which must be returned at the end of the [InOrder.schedule] block
   * to actually schedule the [block].
   *
   * @see withResult the underlying method, which can return a result.
   */
  fun withNoResult(block: ScheduledBlock): InOrderResult<Unit> = withResult(Unit, block)
}

/**
 * A class representing the result of a computation.
 *
 * @param T the type of the return value.
 * @param value the return value of the computation.
 * @param block the effects of the computation, to be scheduled.
 */
internal data class InOrderResult<T>(
    val value: T,
    val block: ScheduledBlock,
)

/**
 * The scope in which the computation block will be scheduled.
 *
 * @see CoroutineScope
 */
internal interface InOrderResultScope : CoroutineScope {

  /** A [Mutex] that guarantees exclusive access. */
  val mutex: Mutex
}

internal typealias ScheduledBlock = suspend InOrderResultScope.() -> Unit

/**
 * Returns a new [InOrder], which uses the receiver [CoroutineScope] to schedule the execution of
 * the different blocks.
 */
internal fun CoroutineScope.inOrder(): InOrder = CoroutineScopeInOrder(this)

private class CoroutineScopeInOrderResultScope(
    override val mutex: Mutex,
    scope: CoroutineScope,
) : InOrderResultScope, CoroutineScope by scope

private class CoroutineScopeInOrder(private val scope: CoroutineScope) : InOrder {

  private val mutex = Mutex()
  private val queue = Channel<ScheduledBlock>(UNLIMITED)

  init {
    scope.launch {
      for (block in queue) {
        coroutineScope { CoroutineScopeInOrderResultScope(mutex, this).block() }
      }
    }
  }

  override suspend fun <T> schedule(block: InOrderScope.() -> InOrderResult<T>): T {
    return mutex.withLock {
      val result = CoroutineScopeInOrderScope().block()
      queue.send(result.block)
      result.value
    }
  }

  inner class CoroutineScopeInOrderScope : InOrderScope, CoroutineScope by scope {

    override fun <T> withResult(
        value: T,
        block: ScheduledBlock,
    ): InOrderResult<T> = InOrderResult(value, block)
  }
}
