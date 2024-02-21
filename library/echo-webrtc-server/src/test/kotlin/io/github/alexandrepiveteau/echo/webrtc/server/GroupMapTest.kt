package io.github.alexandrepiveteau.echo.webrtc.server

import io.github.alexandrepiveteau.echo.webrtc.server.groups.GroupMap
import io.ktor.util.logging.*
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest

class GroupMapTest {

  @Test
  fun `get called with same identifier returns same group`() = runTest {
    val map = GroupMap(CoroutineScope(Job()), KtorSimpleLogger("Test"))
    val id = SessionIdentifier("1")
    assertSame(map.get(id), map.get(id))
  }

  @Test
  fun `get called with different identifiers returns different groups`() = runTest {
    val map = GroupMap(CoroutineScope(Job()), KtorSimpleLogger("Test"))
    val id1 = SessionIdentifier("1")
    val id2 = SessionIdentifier("2")
    assertNotSame(map.get(id1), map.get(id2))
  }
}
