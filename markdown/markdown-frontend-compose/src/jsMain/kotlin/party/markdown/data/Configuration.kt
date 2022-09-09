package party.markdown.data

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.ktor.wsExchange
import io.github.alexandrepiveteau.echo.ktor.wssExchange
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.github.alexandrepiveteau.echo.serialization.decodeFromFrame
import io.github.alexandrepiveteau.echo.webrtc.client.sync
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.browser.window

/**
 * A data class representing the [Configuration] that should be used when communicating with a
 * signaling server.
 *
 * @param host the host name to connect to.
 * @param port the port number on which the API is hosted.
 * @param secure `true` is WSS should be used rather than WS.
 * @param signalingServerPath the endpoint for the signaling server of the protocol.
 */
data class Configuration(
    val host: String,
    val port: Int,
    val secure: Boolean,
    val signalingServerPath: String,
) {
  companion object {

    /**
     * A configuration that's dynamically loaded from a `config.js` file located at the root of the
     * file structure.
     *
     * @param session the unique session identifier for this edition.
     */
    fun fromWindow(session: String): Configuration {
      val env = window.asDynamic().__env
      val host = env.CONF_HOST.unsafeCast<String>()
      val port = env.CONF_PORT.unsafeCast<String>()
      val path = env.CONF_PATH.unsafeCast<String>()
      val secure = env.CONF_SECURE.unsafeCast<String>()
      return Configuration(
          host = host,
          port = port.toInt(),
          signalingServerPath = "$path$session",
          secure = secure.toBooleanStrict(),
      )
    }
  }
}

private fun Configuration.toExchange(): Exchange<Incoming, Outgoing> {
  val builder = if (secure) Client::wssExchange else Client::wsExchange
  return builder(
          // Receiver.
          {
            this.port = this@toExchange.port
            url {
              this.host = this@toExchange.host
              path("$signalingServerPath/rcv")
            }
          },
          // Sender.
          {
            this.port = this@toExchange.port
            url {
              this.host = this@toExchange.host
              path("$signalingServerPath/snd")
            }
          },
      )
      .decodeFromFrame()
}

/** The [HttpClient] which will be used to create the exchanges from configurations. */
private val Client = HttpClient(Js) { install(WebSockets) }

suspend fun Configuration.sync(exchange: Exchange<Incoming, Outgoing>) {
  io.github.alexandrepiveteau.echo.sync(exchange, toExchange())
}
