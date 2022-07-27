package io.github.alexandrepiveteau.echo.webrtc.server

/**
 * A unique identifier for a session. Clients which join the same session will automatically connect
 * in a peer-to-peer fashion.
 */
@JvmInline
public value class SessionIdentifier(private val id: String) {
  override fun toString(): String = id
}
