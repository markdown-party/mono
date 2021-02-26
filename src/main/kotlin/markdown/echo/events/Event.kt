package markdown.echo.events

import markdown.echo.Message.V1 as Message

/**
 * Creates some new events, that are generated in the [EventScope]. This function returns once the
 * events have been successfully added to the underlying [SiteSendEcho].
 *
 * @param T the type of the domain-specific events to add.
 */
suspend fun <T> SiteSendEcho<Message.Incoming<T>, Message.Outgoing<T>>.event(
    scope: suspend EventScope<T>.() -> Unit,
) {
    // TODO : Dispatch addition of events.
}
