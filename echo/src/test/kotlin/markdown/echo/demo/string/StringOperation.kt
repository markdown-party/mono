package markdown.echo.demo.string

import markdown.echo.causal.EventIdentifier

/**
 * A [StringOperation] represents the different operations which are supported on strings. It
 * supports character-wise operations.
 */
sealed class StringOperation {

  /**
   * Inserts a [Char] after a certain [EventIdentifier], or, if not provided, at the beginning of
   * the [String].
   */
  data class InsertAfter(
      val character: Char,
      val after: EventIdentifier? = null,
  ) : StringOperation()

  /** Removes the [Char] at the provided [EventIdentifier]. */
  data class Remove(
      val identifier: EventIdentifier,
  ) : StringOperation()
}
