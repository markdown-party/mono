package party.markdown.p2p.wrappers

import kotlinx.coroutines.await
import party.markdown.p2p.IceCandidate
import party.markdown.p2p.SessionDescription
import webrtc.RTCIceCandidateInit
import webrtc.RTCPeerConnection
import webrtc.RTCPeerConnectionIceEvent

val RTCPeerConnectionIceEvent.candidate: IceCandidate?
  get() {
    val candidate = asDynamic().candidate.unsafeCast<RTCIceCandidateInit?>()
    return candidate?.let { IceCandidate(JSON.stringify(it)) }
  }

fun RTCPeerConnection.onicecandidate(callback: (RTCPeerConnectionIceEvent) -> Unit) =
    addEventListener(
        type = "icecandidate",
        callback = { callback(it as RTCPeerConnectionIceEvent) },
    )

suspend fun RTCPeerConnection.setLocalDescriptionSuspend(description: SessionDescription) =
    setLocalDescription(JSON.parse(description.json)).await()

suspend fun RTCPeerConnection.setRemoteDescriptionSuspend(description: SessionDescription) =
    setRemoteDescription(JSON.parse(description.json)).await()

suspend fun RTCPeerConnection.createOfferSuspend() =
    SessionDescription(JSON.stringify(createOffer().await()))

suspend fun RTCPeerConnection.createAnswerSuspend() =
    SessionDescription(JSON.stringify(createAnswer().await()))

suspend fun RTCPeerConnection.addIceCandidateSuspend(candidate: IceCandidate) =
    addIceCandidate(JSON.parse<RTCIceCandidateInit>(candidate.json)).await()
