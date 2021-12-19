@file:JvmName("Exchanges")
@file:JvmMultifileClass

package io.github.alexandrepiveteau.echo

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * The [BinaryFormat] that's used by default for serialization of events and changes when events are
 * inserted in the event and change logs.
 */
@PublishedApi internal val DefaultSerializationFormat: BinaryFormat = ProtoBuf
