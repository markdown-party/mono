package markdown.echo

import kotlin.RequiresOptIn.Level.WARNING

/**
 * A marker for experimental APIs in the Echo library.
 */
@RequiresOptIn(
    level = WARNING,
    message = "This API is still experimental and should be used with care.",
)
annotation class EchoPreview
