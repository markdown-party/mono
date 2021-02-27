package markdown.echo.memory.causal

import markdown.echo.causal.SiteIdentifier
import kotlin.test.Test
import kotlin.test.assertNotEquals

class SiteIdentifierTest {

    @Test
    fun `Random site identifiers are different`() {
        assertNotEquals(SiteIdentifier.random(), SiteIdentifier.random())
    }
}
