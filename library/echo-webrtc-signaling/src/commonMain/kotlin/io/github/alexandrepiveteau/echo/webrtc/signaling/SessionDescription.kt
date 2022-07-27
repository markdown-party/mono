package io.github.alexandrepiveteau.echo.webrtc.signaling

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/** A session description, encoded in [json]. */
@Serializable
@JvmInline
public value class SessionDescription(public val json: String) {
  override fun toString(): String = json
}
