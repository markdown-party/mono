package io.github.alexandrepiveteau.echo.webrtc.server

import io.github.alexandrepiveteau.echo.webrtc.server.groups.Group
import io.github.alexandrepiveteau.echo.webrtc.server.groups.Outbox
import io.github.alexandrepiveteau.echo.webrtc.signaling.SignalingMessage.ServerToClient
import io.ktor.util.logging.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.test.runTest

class GroupTest {

  internal data class Participant(
      val channel: Channel<ServerToClient> = Channel(Channel.UNLIMITED),
      val outbox: Outbox<ServerToClient> = Outbox.wrap(channel),
  )

  @Test
  fun `does not receive PeerJoined nor PeerLeft when alone in group`() = runTest {
    val job = Job()
    val group = Group(CoroutineScope(job), SessionIdentifier(""), KtorSimpleLogger("Test"))
    val participant = Participant()

    group.session(participant.outbox) { /* No-op. */}

    assertTrue(participant.channel.tryReceive().isFailure)
  }

  @Test
  fun `member receives PeerJoined and PeerLeft when another participant joins`() = runTest {
    val job = Job()
    val group = Group(CoroutineScope(job), SessionIdentifier(""), KtorSimpleLogger("Test"))
    val alice = Participant()
    val bob = Participant()

    val sync = Semaphore(permits = 1)

    sync.acquire()
    val aliceJob = launch {
      group.session(alice.outbox) {
        sync.release()
        suspendCancellableCoroutine {} // Suspend until cancellation.
      }
    }

    sync.acquire()
    group.session(bob.outbox) {}

    // Alice must have received joined and left
    val msg1 = alice.channel.receive() as ServerToClient.PeerJoined
    val msg2 = alice.channel.receive() as ServerToClient.PeerLeft
    assertEquals(msg1.peer, msg2.peer)
    assertTrue(alice.channel.tryReceive().isFailure)

    // Bob must have received only join.
    val msg3 = bob.channel.receive() as ServerToClient.PeerJoined
    assertNotEquals(msg1.peer, msg3.peer)
    assertTrue(bob.channel.tryReceive().isFailure)

    aliceJob.cancelAndJoin()
  }
}
