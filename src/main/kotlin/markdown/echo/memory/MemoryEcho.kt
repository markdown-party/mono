@file:OptIn(EchoPreview::class)

package markdown.echo.memory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import markdown.echo.Echo
import markdown.echo.EchoPreview
import markdown.echo.Exchange
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SiteIdentifier
import markdown.echo.channelExchange
import markdown.echo.events.SiteSendEcho
import markdown.echo.memory.log.MutableEventLog
import markdown.echo.memory.log.mutableEventLogOf
import markdown.echo.Message.V1.Incoming as I
import markdown.echo.Message.V1.Outgoing as O

/**
 * Creates a new [MemoryEcho] instance, that uses a memory-backed store for the events. This [Echo]
 * has a unique identifier, and considers that it is the unique issuer of the events for this
 * specific site identifier.
 *
 * This [Echo] offers bi-directional syncing capabilities, with one-off and continuous sync. It
 * issues push updates when the events in the memory store are modified by a different site.
 *
 * @param site the unique identifier for the site of this [Echo].
 * @param log the [MutableEventLog] backing this [MemoryEcho].
 *
 * @param T the type of the domain-specific events that this [MemoryEcho] supports.
 */
fun <T> Echo.Companion.memory(
    site: SiteIdentifier,
    log: MutableEventLog<T> = mutableEventLogOf(),
): MemoryEcho<T> = MemoryEcho(site, log)

/**
 * An implementation of [Echo] and [SiteSendEcho] that uses an in-memory [MutableEventLog], and
 * has the authority to issue operations for a given [site].
 *
 * @param site the unique identifier for the site of this [Echo].
 *
 * @param T the type of the domain-specific events that this [MemoryEcho] supports.
 */
@OptIn(ExperimentalCoroutinesApi::class)

class MemoryEcho<T>(
    override val site: SiteIdentifier,
    private val log: MutableEventLog<T> = mutableEventLogOf(),
) : Echo<I<T>, O<T>>, SiteSendEcho<I<T>, O<T>> {

    // TODO : Handle variable request size.

    private val mutex = Mutex()
    private val lastInserted = MutableStateFlow<EventIdentifier?>(null)

    override fun outgoing(): Exchange<I<T>, O<T>> = channelExchange { incoming ->
        // TODO : Support outgoing exchanges.
    }

    override fun incoming(): Exchange<O<T>, I<T>> = channelExchange { incoming ->
        // TODO : Support incoming exchanges.
    }
}
