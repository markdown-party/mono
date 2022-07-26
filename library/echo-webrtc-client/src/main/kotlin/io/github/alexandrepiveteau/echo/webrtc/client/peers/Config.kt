package io.github.alexandrepiveteau.echo.webrtc.client.peers

import webrtc.RTCIceServer

/** The [Array] of [RTCIceServer] which can be used for computing the ICE candidates. */
// KLUDGE : Dokka doesn't handle parse `jso { urls = "stun:stun.l.google.com:19302" }` properly,
//          so the corresponding JS code is used directly. See
//          https://github.com/Kotlin/dokka/issues/2327 on GitHub.
internal val GoogleIceServers =
    arrayOf(js("{urls:'stun:stun.l.google.com:19302'}").unsafeCast<RTCIceServer>())
