package io.github.alexandrepiveteau.echo.webrtc.signaling

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/** A session description, encoded in [json]. */
@Serializable
@JvmInline
value class SessionDescription(val json: String) {
  override fun toString() = json
}
