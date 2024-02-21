package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.Event
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A sealed class for all the messages defined in the protocol.
 *
 * ## Basics
 *
 * The protocol is separated in two roles : a sender of [Incoming] messages, and a receiver of
 * [Outgoing] messages. At a high-level, each role does the following :
 *
 * - The receiver originates the sync, and tells which events it wants to receive.
 * - The sender answers the requests with the right events, and advertises when new events are
 * available that the receiver might not know about.
 *
 * In a real-world [Exchange], both sites play both roles simultaneously. Because messages and roles
 * are clearly separated between the server and the receiver, it's possible for sites to perform as
 * a sender and a receiver simultaneously with a single communication channel.
 *
 * ## Backpressure and termination
 *
 * This iteration of the protocol introduces a notion of backpressure by the receiver. Instead of
 * receiving all the events of the sender as they are produced, it becomes the responsibility of the
 * receiver to clearly indicate how many events they want delivered from the sender.
 *
 * This has a few benefits :
 *
 * - The receiver may not request any new events, and it's therefore possible to have a
 * "sender-only" implementation with this flexible behavior.
 * - Sites that need to process messages before requesting additional events can actually do so.
 *
 * Additionally, the sender and the receiver protocol messages offer some _Done_ messages. This
 * allows the following :
 *
 * - The sender may tell that it will not generate any additional [Incoming.Advertisement] and
 * [Incoming.Event].
 * - the receiver may tell that it not generate any additional [Outgoing.Request].
 *
 * Termination is cooperative :
 *
 * - If you're the sender and you receive a [Outgoing.Done] event, you should not send any
 * additional advertisement, nor should you send any additional event (event if they were
 * requested). In-flight messages will still be processed by the receiver but they might be ignored.
 * You should send a _Done_ message right away, before emptying the in-flight queue.
 *
 * - If you're the receiver and you receive an [Incoming.Done] event, you should not send any
 * additional request. You are still allowed to process in-flight messages for advertisements and
 * events, but may not issue any additional request. You should send a _Done_ message right away,
 * before emptying the in-flight queue.
 */
public sealed class Message {

  @Serializable
  public sealed class Incoming : Message() {

    /**
     * This message is sent to let the other site know that we have some events at our disposable
     * for a certain site. When an exchange is started, the [Incoming] site will send some
     * [Advertisement] for all the sites it knows about, before sending a [Ready] message.
     *
     * Afterwards, if messages from new sites become available, some additional [Advertisement] may
     * eventually get sent.
     *
     * Additionally, the next expected [SequenceNumber] when the [Advertisement] is issued is
     * transmitted. This allows one-shot sync support, because sites may terminate after they have
     * synced all the events advertised before the [Ready] message was sent.
     *
     * @param site the [SiteIdentifier] for the available site.
     * @param nextSeqno the next expected [SequenceNumber] for the available site.
     */
    @Serializable
    @SerialName("adv")
    public data class Advertisement(
        val site: SiteIdentifier,
        val nextSeqno: SequenceNumber,
    ) : Incoming()

    /**
     * This message is sent once the sender is done advertising all of its initial sites. This does
     * not mean that additional [Advertisement] messages will not be sent later, but instead
     * provides a "best-effort guarantee" that the site has issued advertisements for all the
     * messages it was aware of when the [Link] was established.
     *
     * When you're interested in one-off syncs, this is usually a good place to start ignoring new
     * [Advertisement] messages.
     */
    @Serializable @SerialName("rdy") public object Ready : Incoming()

    /**
     * Sends multiple events, alongside their body, to the [Outgoing] side. When sending an event,
     * you guarantee that you'll never send any [Event] for the same [SiteIdentifier] and with a
     * smaller [SequenceNumber].
     *
     * @param events the bodies of the events that are sent.
     * @param seqno the sequence number for this [Event].
     * @param site the site that issued this [Event].
     * @param body the domain-specific body of the [Event].
     */
    @Serializable
    @SerialName("e")
    public data class Events(
        val events: List<Event> = emptyList(),
    ) : Incoming()

    public companion object
  }

  @Serializable
  public sealed class Outgoing : Message() {

    /**
     * Indicates the next sequence number that is not known by the [Outgoing] side for a specific
     * site. The [Incoming] side will only messages for acknowledged sites with a remaining request
     * count greater than 0.
     *
     * Receiving an [Acknowledge] resets the [Request] count total to zero.
     *
     * @param site the identifier for the site.
     * @param nextSeqno the next expected sequence number for this site.
     */
    @Serializable
    @SerialName("ack")
    public data class Acknowledge(
        val site: SiteIdentifier,
        val nextSeqno: SequenceNumber,
    ) : Outgoing()

    /**
     * Indicates that the [Outgoing] side of the [Link] is ready to receive some events. A [Request]
     * message can not be sent before the [Incoming.Ready] message has already been received.
     *
     * A [Request] works as follows :
     *
     * - The request is tied to a specific [site]. You may not issue a [Request] for a
     * [SiteIdentifier] that has not been advertised through an [Incoming.Advertisement].
     *
     * @param site the site identifier for which we're interested in this sequence number.
     * @param count how many events were requested.
     */
    @Serializable
    @SerialName("req")
    public data class Request(
        val site: SiteIdentifier,
        val count: UInt,
    ) : Outgoing()

    public companion object
  }
}
