package io.github.alexandrepiveteau.echo.causal

import kotlin.test.Test
import kotlin.test.assertNotEquals

class SiteIdentifierTest {

  @Test
  fun `Random site identifiers are different`() {
    assertNotEquals(SiteIdentifier.random(), SiteIdentifier.random())
  }
}
