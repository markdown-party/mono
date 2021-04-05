package markdown.echo.projections

/**
 * A [OneWayProjection] applies a sequence of events of type [T] to a model of type [M]. This is the
 * most simple projection possible, since it only applies operations in single direction.
 *
 * @param M the type of the model.
 * @param T the type of the events.
 */
fun interface OneWayProjection<M, in T> {

  /** Applies the event [body] to the given [model], and returns a new immutable model. */
  fun forward(body: T, model: M): M
}
