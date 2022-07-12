package io.github.alexandrepiveteau.echo.webrtc.client.wrappers

import io.github.alexandrepiveteau.echo.webrtc.signaling.IceCandidate
import io.github.alexandrepiveteau.echo.webrtc.signaling.SessionDescription
import kotlinx.coroutines.await
import webrtc.RTCIceCandidateInit
import webrtc.RTCPeerConnection
import webrtc.RTCPeerConnectionIceEvent

internal val RTCPeerConnectionIceEvent.candidate: IceCandidate?
  get() {
    val candidate = asDynamic().candidate.unsafeCast<RTCIceCandidateInit?>()
    return candidate?.let { IceCandidate(JSON.stringify(it)) }
  }

internal fun RTCPeerConnection.onicecandidate(callback: (RTCPeerConnectionIceEvent) -> Unit) =
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
internal suspend fun RTCPeerConnection.setLocalDescriptionSuspend(
    description: SessionDescription
): Unit = setLocalDescription(JSON.parse(description.json)).await()

/**
 * Sets the remote [SessionDescription] for this [RTCPeerConnection], and awaits.
 *
 * @receiver the [RTCPeerConnection] to which the session description is set.
 * @param description the [SessionDescription] which is set.
 */
internal suspend fun RTCPeerConnection.setRemoteDescriptionSuspend(
    description: SessionDescription
): Unit = setRemoteDescription(JSON.parse(description.json)).await()

/** Returns the offer [SessionDescription] for this [RTCPeerConnection]. */
internal suspend fun RTCPeerConnection.createOfferSuspend(): SessionDescription =
    SessionDescription(JSON.stringify(createOffer().await()))

/** Returns the answer [SessionDescription] for this [RTCPeerConnection]. */
internal suspend fun RTCPeerConnection.createAnswerSuspend(): SessionDescription =
    SessionDescription(JSON.stringify(createAnswer().await()))

/**
 * Adds an [IceCandidate] for this [RTCPeerConnection], and awaits.
 *
 * @receiver the [RTCPeerConnection] to which the candidate is added.
 * @param candidate the [IceCandidate] which is added to the [RTCPeerConnection].
 */
internal suspend fun RTCPeerConnection.addIceCandidateSuspend(candidate: IceCandidate): Unit =
    addIceCandidate(JSON.parse<RTCIceCandidateInit>(candidate.json)).await()
