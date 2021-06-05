package io.github.alexandrepiveteau.echo.core.internal

import kotlin.RequiresOptIn.Level.WARNING

/** The default capacity for an empty gap buffer. */
internal const val DefaultGapBufferSize = 32

/**
 * An annotation that marks delicate gap buffer APIs, which may be tricking to use properly and
 * might easily lead to mis-usage.
 */
@MustBeDocumented
@RequiresOptIn(level = WARNING, message = "This API is low-level and requires cautious usage.")
internal annotation class DelicateGapBufferApi
