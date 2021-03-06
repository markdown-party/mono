package markdown.echo.events

import markdown.echo.SendEcho
import markdown.echo.causal.SiteIdentifier

/**
 * An extension of a [SendEcho] that has a unique site identifier, and globally unique ownership of
 * this [SiteIdentifier]. No other site in the distributed system is allowed to share this [site]
 * identifier.
 *
 * @param I the type of the domain-specific incoming events for this [SiteSendEcho].
 * @param O the type of the domain-specific outgoing events for this [SiteSendEcho].
 */
interface SiteSendEcho<I, O> : SendEcho<I, O> {

  /** A globally unique [SiteIdentifier], owned by this [SiteSendEcho]. */
  val site: SiteIdentifier
}
