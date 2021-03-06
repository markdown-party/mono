package markdown.echo

import kotlin.RequiresOptIn.Level.ERROR
import kotlin.RequiresOptIn.Level.WARNING

/**
 * This EventLog API is still experimental, and will likely be reworked and refactored. Please make
 * sure you understand the tradeoffs before using it.
 */
@RequiresOptIn(
    level = ERROR,
    message =
        """
        This EventLog API is still experimental, and will likely be reworked and refactored. Please
        make sure you understand the tradeoffs before using it.
        """,
)
@MustBeDocumented
annotation class EchoEventLogPreview

/**
 * This Sync API is still experimental, and will likely be reworked and refactored. Please make sure
 * you understand the tradeoffs before using it.
 */
@RequiresOptIn(
    level = WARNING,
    message =
        """
        This Sync API is still experimental, and will likely be reworked and refactored. Please
        make sure you understand the tradeoffs before using it.
""",
)
@MustBeDocumented
annotation class EchoSyncPreview
