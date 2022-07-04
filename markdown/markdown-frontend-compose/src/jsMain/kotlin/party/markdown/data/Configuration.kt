package party.markdown.data

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.ktor.wsExchange
import io.github.alexandrepiveteau.echo.ktor.wssExchange
import io.github.alexandrepiveteau.echo.protocol.Message
import io.github.alexandrepiveteau.echo.serialization.decodeFromFrame
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * A data class representing the [Configuration] that should be used when communicating with a
 * remote server.
 *
 * @param host the host name to connect to.
 * @param port the port number on which the API is hosted.
 * @param secure `true` is WSS should be used rather than WS.
 * @param senderEndpoint the endpoint for the sender side of the protocol.
 * @param receiverEndpoint the endpoint for the receiver side of the protocol.
 */
data class Configuration(
    val host: String,
    val port: Int,
    val secure: Boolean,
    val senderEndpoint: String,
    val receiverEndpoint: String,
) {
  companion object {

    /**
     * A remote configuration that's used in production.
     *
     * @param session the unique session identifier for this edition.
     */
    fun remote(session: String): Configuration =
        Configuration(
            host = "api.markdown.party",
            port = 443,
            secure = true,
            senderEndpoint = "v1/$session/snd",
            receiverEndpoint = "v1/$session/rcv",
        )

    /**
     * A local configuration that's used for local deployments.
     *
     * @param session the unique session identifier for this edition.
     */
    fun local(session: String): Configuration =
        Configuration(
            host = "localhost",
            port = 1234,
            secure = false,
            senderEndpoint = "v1/$session/snd",
            receiverEndpoint = "v1/$session/rcv",
        )
  }
}

/** The [HttpClient] which will be used to create the exchanges from configurations. */
private val Client = HttpClient(Js) { install(WebSockets) }

/**
 * Returns the [Exchange] that corresponds to the receiver [Configuration]. This [Exchange] can then
 * be used to sync with some sites or mutable sites locally to interpret the operations.
 */
fun Configuration.toExchange(): Exchange<Message.Incoming, Message.Outgoing> {
  val builder = if (secure) Client::wssExchange else Client::wsExchange
  return builder(
          // Receiver.
          {
            this.port = this@toExchange.port
            url {
              this.host = this@toExchange.host
              path(this@toExchange.receiverEndpoint)
            }
          },
          // Sender.
          {
            this.port = this@toExchange.port
            url {
              this.host = this@toExchange.host
              path(this@toExchange.senderEndpoint)
            }
          },
      )
      .decodeFromFrame()
}
