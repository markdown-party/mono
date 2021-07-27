package io.github.alexandrepiveteau.echo.site

import io.github.alexandrepiveteau.echo.core.causality.toSiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.suspendTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MutableSiteEventTest {

  @Test
  fun empty_event_terminates() = suspendTest {
    val alice = mutableSite<Int>(123U.toSiteIdentifier())
    alice.event {}
  }

  @Test
  fun multiple_empty_event_terminates() = suspendTest {
    val echo = mutableSite<Int>(456U.toSiteIdentifier())
    repeat(2) { echo.event {} }
  }

  @Test
  fun multiple_nonEmpty_event_terminates() = suspendTest {
    val site = mutableSite<Int>(456U.toSiteIdentifier())
    repeat(2) { iteration -> site.event { yield(iteration) } }
  }

  @Test
  fun singleYield_terminates() = suspendTest {
    val site = 145U.toSiteIdentifier()
    with(mutableSite<Int>(site)) { event { assertEquals(site, yield(123).site) } }
  }

  @Test
  fun multipleYield_terminates() = suspendTest {
    val site = 145U.toSiteIdentifier()
    with(mutableSite<Int>(site)) {
      event {
        val a = yield(123)
        val b = yield(456)
        assertEquals(site, a.site)
        assertEquals(site, b.site)
        assertTrue(a.seqno < b.seqno)
      }
    }
  }
}
