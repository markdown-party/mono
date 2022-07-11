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

/**
 * Sets the local [SessionDescription] for this [RTCPeerConnection], and awaits.
 *
 * @receiver the [RTCPeerConnection] to which the session description is set.
 * @param description the [SessionDescription] which is set.
 */
suspend fun RTCPeerConnection.setLocalDescriptionSuspend(description: SessionDescription): Unit =
    setLocalDescription(JSON.parse(description.json)).await()

/**
 * Sets the remote [SessionDescription] for this [RTCPeerConnection], and awaits.
 *
 * @receiver the [RTCPeerConnection] to which the session description is set.
 * @param description the [SessionDescription] which is set.
 */
suspend fun RTCPeerConnection.setRemoteDescriptionSuspend(description: SessionDescription): Unit =
    setRemoteDescription(JSON.parse(description.json)).await()

/** Returns the offer [SessionDescription] for this [RTCPeerConnection]. */
suspend fun RTCPeerConnection.createOfferSuspend(): SessionDescription =
    SessionDescription(JSON.stringify(createOffer().await()))

/** Returns the answer [SessionDescription] for this [RTCPeerConnection]. */
suspend fun RTCPeerConnection.createAnswerSuspend(): SessionDescription =
    SessionDescription(JSON.stringify(createAnswer().await()))

/**
 * Adds an [IceCandidate] for this [RTCPeerConnection], and awaits.
 *
 * @receiver the [RTCPeerConnection] to which the candidate is added.
 * @param candidate the [IceCandidate] which is added to the [RTCPeerConnection].
 */
suspend fun RTCPeerConnection.addIceCandidateSuspend(candidate: IceCandidate): Unit =
    addIceCandidate(JSON.parse<RTCIceCandidateInit>(candidate.json)).await()
