package io.github.alexandrepiveteau.echo.webrtc.client.peerToPeer

import io.github.alexandrepiveteau.echo.webrtc.client.PeerToPeerConnection

internal interface PeerToPeerConnectionFactory {
  fun create(): PeerToPeerConnection
}
