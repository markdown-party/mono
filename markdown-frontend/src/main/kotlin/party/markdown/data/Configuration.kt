package party.markdown.data

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.ktor.EchoKtorPreview
import io.github.alexandrepiveteau.echo.ktor.wsExchange
import io.github.alexandrepiveteau.echo.ktor.wssExchange
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.github.alexandrepiveteau.echo.serialization.decodeFromFrame
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*

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

    /** The configuration that's used in production. */
    val Default =
        Configuration(
            host = "api.markdown.party",
            port = 443,
            secure = true,
            senderEndpoint = "sender",
            receiverEndpoint = "receiver",
        )

    /** The configuration that's used for local deployments. */
    val Local =
        Configuration(
            host = "localhost",
            port = 1234,
            secure = false,
            senderEndpoint = "sender",
            receiverEndpoint = "receiver",
        )
  }
}

/** The [HttpClient] which will be used to create the exchanges from configurations. */
private val Client = HttpClient(Js) { install(WebSockets) }

/**
 * Returns the [Exchange] that corresponds to the receiver [Configuration]. This [Exchange] can then
 * be used to sync with some sites or mutable sites locally to interpret the operations.
 */
@EchoKtorPreview
fun Configuration.toExchange(): Exchange<Incoming, Outgoing> {
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
