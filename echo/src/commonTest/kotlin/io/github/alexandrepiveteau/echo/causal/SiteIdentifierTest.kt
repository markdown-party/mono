package io.github.alexandrepiveteau.echo.causal

import kotlin.test.Test
import kotlin.test.assertNotEquals

class SiteIdentifierTest {

  @Test
  fun randomSites_AreNotEqual() {
    assertNotEquals(SiteIdentifier.random(), SiteIdentifier.random())
  }
}
