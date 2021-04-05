package markdown.echo.memory.causal

import kotlin.test.Test
import kotlin.test.assertNotEquals
import markdown.echo.causal.SiteIdentifier

class SiteIdentifierTest {

  @Test
  fun `Random site identifiers are different`() {
    assertNotEquals(SiteIdentifier.random(), SiteIdentifier.random())
  }
}
