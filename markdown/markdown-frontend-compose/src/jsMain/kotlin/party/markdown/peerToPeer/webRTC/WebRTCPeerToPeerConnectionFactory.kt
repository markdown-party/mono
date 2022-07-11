package party.markdown.peerToPeer.webRTC

import party.markdown.peerToPeer.PeerToPeerConnectionFactory

interface WebRTCPeerToPeerConnectionFactory : PeerToPeerConnectionFactory {
  override fun create(): WebRTCPeerToPeerConnection
}
