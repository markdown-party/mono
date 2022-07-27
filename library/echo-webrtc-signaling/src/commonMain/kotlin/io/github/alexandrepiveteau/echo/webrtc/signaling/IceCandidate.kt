package io.github.alexandrepiveteau.echo.webrtc.signaling

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/** An ICE candidate, encoded in [json]. */
@Serializable
@JvmInline
public value class IceCandidate(public val json: String) {
  override fun toString(): String = json
}
