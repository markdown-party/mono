package io.github.alexandrepiveteau.echo.webrtc.client

import kotlin.time.Duration.Companion.milliseconds

/** The delay before retrying to a lost signaling server. */
internal val RetryDelaySignalingServer = 1000.milliseconds

/** The delay before retrying to connect to a lost data channel. */
internal val RetryDelayDataChannel = 1000.milliseconds
