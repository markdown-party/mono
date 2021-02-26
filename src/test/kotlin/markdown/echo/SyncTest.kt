package markdown.echo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class SyncTest {

    @Test
    @ExperimentalCoroutinesApi
    fun `NoOp exchange sync eventually terminates`() = runBlocking {
        val alice = NoOpEcho
        val bob = NoOpEcho

        sync(alice, bob)
    }

    @Test
    fun `NoOp chain sync eventually terminates`() = runBlocking {
        val head = NoOpEcho
        val tail = Array(10) { NoOpEcho }

        sync(head, *tail)
    }
}

object NoOpEcho : Echo<Nothing, Nothing> {
    override fun incoming() = exchange<Nothing, Nothing> { }
    override fun outgoing() = exchange<Nothing, Nothing> { }
}
