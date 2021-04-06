package io.github.alexandrepiveteau.echo.protocol

import kotlinx.serialization.Serializable

object Transport {

  /** @see Message.V1 */
  @Serializable
  sealed class V1 {

    /** @see Message.V1.Incoming */
    @Serializable
    sealed class Incoming : V1() {

      /** @see Message.V1.Incoming.Advertisement */
      @Serializable
      data class Advertisement(
          val site: Int,
      ) : Incoming()

      /** @see Message.V1.Incoming.Ready */
      @Serializable object Ready : Incoming()

      /** @see Message.V1.Incoming.Event */
      @Serializable
      data class Event(
          val seqno: Int,
          val site: Int,
          val body: String,
      ) : Incoming()

      /** @see Message.V1.Incoming.Done */
      @Serializable object Done : Incoming()
    }

    /** @see Message.V1.Outgoing */
    @Serializable
    sealed class Outgoing : V1() {

      /** @see Message.V1.Outgoing.Request */
      @Serializable
      data class Request(
          val nextForAll: Int,
          val nextForSite: Int,
          val site: Int,
          val count: Long,
      ) : Outgoing()

      /** @see Message.V1.Outgoing.Done */
      @Serializable object Done : Outgoing()
    }
  }
}
