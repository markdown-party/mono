@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.github.alexandrepiveteau.echo.core

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.AT_MOST_ONCE
import kotlin.contracts.contract

/**
 * Requires that the given [Int] is in the range formed by [from] and [until]. If not, the exception
 * message [lazyMessage] will be used to launch an [IndexOutOfBoundsException].
 *
 * @param index the index to check for.
 * @param from the lower boundary for [Int], inclusive.
 * @param until the upper boundary for [Int], exclusive.
 * @param lazyMessage the error message builder.
 *
 * @throws IndexOutOfBoundsException if the condition is not respected.
 */
internal inline fun requireIn(index: Int, from: Int, until: Int, lazyMessage: () -> Any) {
  contract { callsInPlace(lazyMessage, AT_MOST_ONCE) }
  if (index < from || index >= until) {
    val message = lazyMessage()
    throw IndexOutOfBoundsException(message.toString())
  }
}

/**
 * Requires that the given [Int] is in the range formed by [from] and [until]. If not an
 * [IndexOutOfBoundsException] will be launched.
 *
 * @param index the index to check for.
 * @param from the lower boundary for [Int], inclusive.
 * @param until the upper boundary for [Int], exclusive.
 *
 * @throws IndexOutOfBoundsException if the condition is not respected.
 */
internal inline fun requireIn(index: Int, from: Int, until: Int) {
  requireIn(index, from, until) { "Required indices not in range." }
}

/**
 * Requires that the given [Int] is in the given [IntRange]. If not, the exception message
 * [lazyMessage] will be used to launch an [IndexOutOfBoundsException].
 *
 * @param index the index to check for.
 * @param range the target [IntRange].
 * @param lazyMessage the error message builder.
 *
 * @throws IndexOutOfBoundsException if the condition is not respected.
 */
internal inline fun requireIn(index: Int, range: IntRange, lazyMessage: () -> String) {
  contract { callsInPlace(lazyMessage, AT_MOST_ONCE) }
  require(index in range, lazyMessage)
}

/**
 * Requires that the given [Int] is in the given [IntRange]. If not an [IndexOutOfBoundsException]
 * will be launched.
 *
 * @param index the index to check for.
 * @param range the target [IntRange].
 *
 * @throws IndexOutOfBoundsException if the condition is not respected.
 */
internal inline fun requireIn(index: Int, range: IntRange) {
  requireIn(index, range) { "Required indices not in range." }
}

// ARRAY RANGE CHECKS

/**
 * Requires that the given [from] and [until] values are compatible with the provided [size]. This
 * means that the following conditions must hold :
 *
 * - [from] is positive, and has at most value [size].
 * - [until] is positive, and has at most value [size].
 * - [until] is greater than or equal to [until].
 *
 * @param from the start index of the range.
 * @param until the end index of the range.
 * @param size the size of the range.
 * @param lazyMessage the error message builder.
 */
internal inline fun requireRange(from: Int, until: Int, size: Int, lazyMessage: () -> Any) {
  contract { callsInPlace(lazyMessage, AT_MOST_ONCE) }
  requireIn(from, 0, size + 1, lazyMessage)
  requireIn(until, 0, size + 1, lazyMessage)
  require(from <= until, lazyMessage)
}

/**
 * Requires that the given [from] and [until] values are compatible with the provided [size]. This
 * means that the following conditions must hold :
 *
 * - [from] is positive, and has at most value [size].
 * - [until] is positive, and has at most value [size].
 * - [until] is greater than or equal to [until].
 *
 * @param from the start index of the range.
 * @param until the end index of the range.
 * @param size the size of the range.
 */
internal inline fun requireRange(from: Int, until: Int, size: Int) {
  requireRange(from, until, size) { "Required indices not in range." }
}

/**
 * Requires that the given [from] and [until] values are compatible with the provided `array.size`.
 * This means that the following conditions must hold :
 *
 * - [from] is positive, and has at most value `array.size`.
 * - [until] is positive, and has at most value `array.size`.
 * - [until] is greater than or equal to [until].
 *
 * @param from the start index of the range.
 * @param until the end index of the range.
 * @param array the array to check for the range.
 * @param lazyMessage the error message builder.
 */
internal inline fun requireRange(from: Int, until: Int, array: ByteArray, lazyMessage: () -> Any) {
  contract { callsInPlace(lazyMessage, AT_MOST_ONCE) }
  requireRange(from, until, array.size, lazyMessage)
}

/**
 * Requires that the given [from] and [until] values are compatible with the provided `array.size`.
 * This means that the following conditions must hold :
 *
 * - [from] is positive, and has at most value `array.size`.
 * - [until] is positive, and has at most value `array.size`.
 * - [until] is greater than or equal to [until].
 *
 * @param from the start index of the range.
 * @param until the end index of the range.
 * @param array the array to check for the range.
 */
internal inline fun requireRange(from: Int, until: Int, array: ByteArray) {
  requireRange(from, until, array.size) { "Required indices not in array" }
}

/**
 * Requires that the given [from] and [until] values are compatible with the provided `array.size`.
 * This means that the following conditions must hold :
 *
 * - [from] is positive, and has at most value `array.size`.
 * - [until] is positive, and has at most value `array.size`.
 * - [until] is greater than or equal to [until].
 *
 * @param from the start index of the range.
 * @param until the end index of the range.
 * @param array the array to check for the range.
 * @param lazyMessage the error message builder.
 */
internal inline fun requireRange(from: Int, until: Int, array: IntArray, lazyMessage: () -> Any) {
  contract { callsInPlace(lazyMessage, AT_MOST_ONCE) }
  requireRange(from, until, array.size, lazyMessage)
}

/**
 * Requires that the given [from] and [until] values are compatible with the provided `array.size`.
 * This means that the following conditions must hold :
 *
 * - [from] is positive, and has at most value `array.size`.
 * - [until] is positive, and has at most value `array.size`.
 * - [until] is greater than or equal to [until].
 *
 * @param from the start index of the range.
 * @param until the end index of the range.
 * @param array the array to check for the range.
 */
internal inline fun requireRange(from: Int, until: Int, array: IntArray) {
  requireRange(from, until, array.size) { "Required indices not in array" }
}

/**
 * Requires that the given [from] and [until] values are compatible with the provided `array.size`.
 * This means that the following conditions must hold :
 *
 * - [from] is positive, and has at most value `array.size`.
 * - [until] is positive, and has at most value `array.size`.
 * - [until] is greater than or equal to [until].
 *
 * @param from the start index of the range.
 * @param until the end index of the range.
 * @param array the array to check for the range.
 * @param lazyMessage the error message builder.
 */
internal inline fun requireRange(
    from: Int,
    until: Int,
    array: EventIdentifierArray,
    lazyMessage: () -> Any,
) {
  contract { callsInPlace(lazyMessage, AT_MOST_ONCE) }
  requireRange(from, until, array.size, lazyMessage)
}

/**
 * Requires that the given [from] and [until] values are compatible with the provided `array.size`.
 * This means that the following conditions must hold :
 *
 * - [from] is positive, and has at most value `array.size`.
 * - [until] is positive, and has at most value `array.size`.
 * - [until] is greater than or equal to [until].
 *
 * @param from the start index of the range.
 * @param until the end index of the range.
 * @param array the array to check for the range.
 */
internal inline fun requireRange(from: Int, until: Int, array: EventIdentifierArray) {
  requireRange(from, until, array.size) { "Required indices not in array" }
}
