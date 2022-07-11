@file:JvmName("Exchanges")
@file:JvmMultifileClass

package io.github.alexandrepiveteau.echo

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * The [BinaryFormat] that's used by default for serialization of events and changes when events are
 * inserted in the event and change logs.
 */
val DefaultBinaryFormat: BinaryFormat = ProtoBuf
