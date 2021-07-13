package party.markdown.ui.editor

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray

class RGAStateEffect(
    private val items: Array<RGAStateEffect>,
)

private class RGAStateEffectItem(
    val from: Int,
    val until: Int,
    val identifiers: EventIdentifierArray,
)
