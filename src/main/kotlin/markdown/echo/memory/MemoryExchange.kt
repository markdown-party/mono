@file:OptIn(
    ExperimentalCoroutinesApi::class,
    FlowPreview::class,
    InternalCoroutinesApi::class,
)

package markdown.echo.memory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import markdown.echo.Exchange
import markdown.echo.Link
import markdown.echo.Message
import markdown.echo.Message.V1.Incoming as I
import markdown.echo.Message.V1.Outgoing as O
import markdown.echo.causal.SiteIdentifier
import markdown.echo.events.SiteSendExchange
import markdown.echo.memory.log.MutableEventLog
import markdown.echo.memory.log.mutableEventLogOf
import markdown.echo.mutableSite

/**
 * Creates a new [MemoryExchange] instance, that uses a memory-backed store for the events. This
 * [Exchange] has a unique identifier, and considers that it is the unique issuer of the events for
 * this specific site identifier.
 *
 * This [Exchange] offers bi-directional syncing capabilities, with one-off and continuous sync. It
 * issues push updates when the events in the memory store are modified by a different site.
 *
 * @param site the unique identifier for the site of this [Exchange].
 * @param log the [MutableEventLog] backing this [MemoryExchange].
 *
 * @param T the type of the domain-specific events that this [MemoryExchange] supports.
 */
fun <T> Exchange.Companion.memory(
    site: SiteIdentifier,
    log: MutableEventLog<T> = mutableEventLogOf(),
): MemoryExchange<T> = MemoryExchange(site, log)

/**
 * An implementation of [Exchange] and [SiteSendExchange] that uses an in-memory [MutableEventLog],
 * and has the authority to issue operations for a given [site].
 *
 * @param site the unique identifier for the site of this [Exchange].
 *
 * @param T the type of the domain-specific events that this [MemoryExchange] supports.
 */
@OptIn(
    ExperimentalCoroutinesApi::class,
    InternalCoroutinesApi::class,
    FlowPreview::class,
)
class MemoryExchange<T>(
    override val site: SiteIdentifier,
    log: MutableEventLog<T> = mutableEventLogOf(),
) : Exchange<I<T>, O<T>>, SiteSendExchange<I<T>, O<T>> {

  private val backing = mutableSite(site, log)

  override fun outgoing(): Link<Message.V1.Incoming<T>, Message.V1.Outgoing<T>> {
    return backing.outgoing()
  }

  override fun incoming(): Link<Message.V1.Outgoing<T>, Message.V1.Incoming<T>> {
    return backing.incoming()
  }
}
