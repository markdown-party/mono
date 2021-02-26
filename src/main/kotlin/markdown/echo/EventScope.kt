package markdown.echo

/**
 * An interface describing the operations that can be performed when we want to issue some events.
 * Usually, commands will be described as extensions to the [EventScope] interface.
 *
 * @param T The type of the application-specific event data.
 */
@EventDsl
interface EventScope<T> {

    /**
     * Append a new event to the issued operations by this site. A happens-before relationship is
     * established with (at least) all the preceding events on this site; therefore, two subsequent
     * calls to [yield] guarantee that the order of the operations will be preserved.
     *
     * @param event The event that will be added to the log.
     */
    suspend fun yield(
        event: T,
    )

    /**
     * Appends an [Iterator] of events to the operations of this site.
     *
     * @param events The events that will be added to the log.
     */
    suspend fun yieldAll(
        events: Iterator<T>,
    ) {
        for (event in events) {
            yield(event)
        }
    }

    /**
     * Appends an [Iterable] of events to the operations of this site.
     *
     * @param events The events that will be added to the log.
     */
    suspend fun yieldAll(
        events: Iterable<T>,
    ) {
        yieldAll(events.iterator())
    }
}
