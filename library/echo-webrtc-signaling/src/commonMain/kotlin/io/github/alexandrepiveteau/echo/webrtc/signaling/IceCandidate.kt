package io.github.alexandrepiveteau.echo.webrtc.signaling

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/** An ICE candidate, encoded in [json]. */
@Serializable @JvmInline value class IceCandidate(val json: String)
