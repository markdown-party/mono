package markdown.echo.events

import markdown.echo.SendExchange
import markdown.echo.causal.SiteIdentifier

/**
 * An extension of a [SendExchange] that has a unique site identifier, and globally unique ownership of
 * this [SiteIdentifier]. No other site in the distributed system is allowed to share this [site]
 * identifier.
 *
 * @param I the type of the domain-specific incoming events for this [SiteSendExchange].
 * @param O the type of the domain-specific outgoing events for this [SiteSendExchange].
 */
interface SiteSendExchange<I, O> : SendExchange<I, O> {

  /** A globally unique [SiteIdentifier], owned by this [SiteSendExchange]. */
  val site: SiteIdentifier
}
