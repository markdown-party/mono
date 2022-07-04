package party.markdown.ui.editor

import codemirror.state.Annotation
import codemirror.state.StateField
import codemirror.state.StateFieldConfig
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Sets the current instance using an [Annotation]. This should be regularly set by the container,
 * in order to make the cursors synchronously disappear.
 */
val SetNowAnnotation = Annotation.define<Instant>()

/**
 * A [StateField] with the latest [Instant]. On conflicting values, the highest [Instant] is kept,
 * which guarantees the monotonicity of this clock, but not its progress.
 */
val NowStateField =
    StateField.define(
        StateFieldConfig(
            create = { Clock.System.now() },
            update = { value, transaction ->
              if (transaction.annotation(SetNowAnnotation) === undefined) {
                value
              } else maxOf(transaction.annotation(SetNowAnnotation), value)
            },
        ),
    )
