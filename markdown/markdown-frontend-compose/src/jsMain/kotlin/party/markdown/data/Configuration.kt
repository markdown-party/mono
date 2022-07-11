package party.markdown.data

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import party.markdown.p2p.sync
import party.markdown.p2p.wsSignalingServer
import party.markdown.p2p.wssSignalingServer

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
     * A remote configuration that's used in production.
     *
     * @param session the unique session identifier for this edition.
     */
    fun remote(session: String): Configuration =
        Configuration(
            host = "api.markdown.party",
            port = 443,
            secure = true,
            signalingServerPath = "groups/$session",
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
            signalingServerPath = "groups/$session",
        )
  }
}

/** The [HttpClient] which will be used to create the exchanges from configurations. */
private val Client = HttpClient(Js) { install(WebSockets) }

suspend fun Configuration.sync(exchange: Exchange<Incoming, Outgoing>) {
  val config = this
  val server = if (secure) Client::wssSignalingServer else Client::wsSignalingServer
  server(
      exchange,
      {
        port = config.port
        url {
          host = config.host
          path(config.signalingServerPath)
        }
      },
  ) {
    this.sync(exchange)
  }
}
