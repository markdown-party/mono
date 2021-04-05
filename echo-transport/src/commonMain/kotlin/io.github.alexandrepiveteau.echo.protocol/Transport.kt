package io.github.alexandrepiveteau.echo.protocol

import kotlinx.serialization.Serializable

object Transport {

  /** @see Message.V1 */
  @Serializable
  sealed class V1<out T> {

    /** @see Message.V1.Incoming */
    @Serializable
    sealed class Incoming<out T> : V1<T>() {

      /** @see Message.V1.Incoming.Advertisement */
      @Serializable
      data class Advertisement<out T>(
          val site: Int,
      ) : Incoming<T>()

      /** @see Message.V1.Incoming.Ready */
      @Serializable class Ready<out T> : Incoming<T>()

      /** @see Message.V1.Incoming.Event */
      @Serializable
      data class Event<out T>(
          val seqno: Int,
          val site: Int,
          val body: T,
      ) : Incoming<T>()

      /** @see Message.V1.Incoming.Done */
      @Serializable class Done<out T> : Incoming<T>()
    }

    /** @see Message.V1.Outgoing */
    @Serializable
    sealed class Outgoing<out T> : V1<T>() {

      /** @see Message.V1.Outgoing.Request */
      @Serializable
      data class Request<out T>(
          val nextForAll: Int,
          val nextForSite: Int,
          val site: Int,
          val count: Long,
      ) : Outgoing<T>()

      /** @see Message.V1.Outgoing.Done */
      @Serializable class Done<out T> : Outgoing<T>()
    }
  }
}
