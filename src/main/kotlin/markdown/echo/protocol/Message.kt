package markdown.echo.protocol

import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
import markdown.echo.protocol.Message.V1.Incoming
import markdown.echo.protocol.Message.V1.Outgoing

/**
 * A global namespace for the messages that are supported by the Echo replication protocol. You'll
 * find different dedicated classes with various iterations of the protocol.
 *
 * Currently, only [Message.V1] is supported.
 */
object Message {

  /**
   * A sealed class for all the messages defined in the first iteration of the protocol.
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
   * In a real-world [Exchange], both sites play both roles simultaneously. Because messages and
   * roles are clearly separated between the server and the receiver, it's possible for sites to
   * perform as a sender and a receiver simultaneously with a single communication channel.
   *
   * ## Backpressure and termination
   *
   * This iteration of the protocol introduces a notion of backpressure by the receiver. Instead of
   * receiving all the events of the sender as they are produced, it becomes the responsibility of
   * the receiver to clearly indicate how many events they want delivered from the sender.
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
   * requested). In-flight messages will still be processed by the receiver but they might be
   * ignored. You should send a _Done_ message right away, before emptying the in-flight queue.
   *
   * - If you're the receiver and you receive an [Incoming.Done] event, you should not send any
   * additional request. You are still allowed to process in-flight messages for advertisements and
   * events, but may not issue any additional request. You should send a _Done_ message right away,
   * before emptying the in-flight queue.
   */
  sealed class V1<out T> {

    sealed class Incoming<out T> : V1<T>() {

      /**
       * This message is sent to let the other site know that we have some events at our disposable
       * for a certain site. When an [Link] is started, the [Incoming] site will send some
       * [Advertisement] for all the sites it knows about, before sending a [Ready] message.
       *
       * Afterwards, if messages from new sites become available, some additional [Advertisement]
       * may eventually get sent.
       *
       * TODO : Eventually provide a [SequenceNumber] with the highest event available ?
       * ```
       *        This could help with one-off syncs, letting the other side know what's a good
       *        request if one wants to only stick to the pre-[Ready] events.
       *
       * @param site
       * ```
       * the [SiteIdentifier] for the available site.
       */
      data class Advertisement(
          val site: SiteIdentifier,
      // val seqno: SequenceNumber?,
      ) : Incoming<Nothing>()

      /**
       * This message is sent once the sender is done advertising all of its initial sites. This
       * does not mean that additional [Advertisement] messages will not be sent later, but instead
       * provides a "best-effort guarantee" that the site has issued advertisements for all the
       * messages it was aware of when the [Link] was established.
       *
       * When you're interested in one-off syncs, this is usually a good place to start ignoring new
       * [Advertisement] messages.
       */
      object Ready : Incoming<Nothing>()

      /**
       * Sends an event, alongside its body, to the [Outgoing] side. When sending an event, you
       * guarantee that you'll never send any [Event] for the same [SiteIdentifier] and with a
       * smaller [SequenceNumber].
       *
       * @param seqno the sequence number for this [Event].
       * @param site the site that issued this [Event].
       * @param body the domain-specific body of the [Event].
       */
      data class Event<out T>(
          val seqno: SequenceNumber,
          val site: SiteIdentifier,
          val body: T,
      ) : Incoming<T>()

      /**
       * Indicates that the [Incoming] side of the [Link] would like to terminate the communication.
       * Once the [Done] message is emitted, the [Incoming] side will drain all the in-flight
       * messages until the other side's [Outgoing.Done] message is received.
       *
       * Afterwards, both sites may disconnect.
       */
      object Done : Incoming<Nothing>()
    }

    sealed class Outgoing<out T> : V1<T>() {

      /**
       * Indicates that the [Outgoing] side of the [Link] is ready to receive some events. A
       * [Request] message can not be sent before the [Incoming.Ready] message has already been
       * received.
       *
       * A [Request] works as follows :
       *
       * - The request is tied to a specific [site]. You may not issue a [Request] for a
       * [SiteIdentifier] that has not been advertised through an [Incoming.Advertisement].
       * - The [nextForSite] indicates what the requested sequence number for the specific [site]
       * is.
       * - The [nextForAll] indicates the requested sequence number if the other site was to
       * generate an event that's definitely higher than your current knowledge.
       *
       * @param nextForAll the next [SequenceNumber] that is expected for all the sites.
       * @param nextForSite the next [SequenceNumber] that is expected for the [site].
       * @param site the site identifier for which we're interested in this sequence number.
       * @param count how many events were requested.
       */
      data class Request(
          val nextForAll: SequenceNumber,
          val nextForSite: SequenceNumber,
          val site: SiteIdentifier,
          val count: Long = Long.MAX_VALUE,
      ) : Outgoing<Nothing>()

      /**
       * Indicates that the [Outgoing] side of the [Link] would like to terminate the communication.
       * Once the [Done] message is emitted, the [Outgoing] side will drain all the in-flight
       * messages until the other's side [Incoming.Done] message is received.
       *
       * Afterwards, both sites may disconnect.
       */
      object Done : Outgoing<Nothing>()
    }
  }
}
