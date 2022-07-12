package io.github.alexandrepiveteau.echo.webrtc.signaling

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * A channel identifier, which lets clients multiplex over a single connection and which has a
 * unique [id].
 */
@Serializable @JvmInline value class ChannelId(val id: Int)
