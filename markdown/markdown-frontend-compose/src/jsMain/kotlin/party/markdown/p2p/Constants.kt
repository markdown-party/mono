package party.markdown.p2p

import kotlin.time.Duration.Companion.milliseconds

/** The delay before retrying to a lost signaling server. */
val RetryDelaySignalingServer = 1000.milliseconds

/** The delay before retrying to connect to a lost data channel. */
val RetryDelayDataChannel = 1000.milliseconds
