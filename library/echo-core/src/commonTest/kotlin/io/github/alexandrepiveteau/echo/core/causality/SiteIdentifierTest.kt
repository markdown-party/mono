package io.github.alexandrepiveteau.echo.core.causality

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotEquals

class SiteIdentifierTest {

  @Test
  fun randomSites_AreNotEqual() {
    assertNotEquals(Random.nextSiteIdentifier(), Random.nextSiteIdentifier())
  }
}
