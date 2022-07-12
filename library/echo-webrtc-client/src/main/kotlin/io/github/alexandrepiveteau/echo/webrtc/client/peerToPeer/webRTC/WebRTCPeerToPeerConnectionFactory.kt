package io.github.alexandrepiveteau.echo.webrtc.client.peerToPeer.webRTC

import io.github.alexandrepiveteau.echo.webrtc.client.peerToPeer.PeerToPeerConnectionFactory
import io.github.alexandrepiveteau.echo.webrtc.client.peerToPeer.webRTC.WebRTCPeerToPeerConnection

internal interface WebRTCPeerToPeerConnectionFactory : PeerToPeerConnectionFactory {
  override fun create(): WebRTCPeerToPeerConnection
}
