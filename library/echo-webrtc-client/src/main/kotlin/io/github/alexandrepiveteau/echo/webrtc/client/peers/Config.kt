package io.github.alexandrepiveteau.echo.webrtc.client.peers

import kotlinx.js.jso
import webrtc.RTCIceServer

/** The [Array] of [RTCIceServer] which can be used for computing the ICE candidates. */
internal val GoogleIceServers = arrayOf<RTCIceServer>(jso { urls = "stun:stun.l.google.com:19302" })
