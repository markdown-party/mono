package io.github.alexandrepiveteau.echo.webrtc.client.peerToPeer

internal interface PeerToPeerConnectionFactory {
  fun create(): PeerToPeerConnection
}
