package markdown.echo.memory.projections

import markdown.echo.projections.OneWayProjection

object CounterOneWayProjection : OneWayProjection<Int, Int> {
    override fun forward(body: Int, model: Int) = body + model
}
