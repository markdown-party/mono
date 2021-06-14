@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.github.alexandrepiveteau.echo.core.internal

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.AT_MOST_ONCE
import kotlin.contracts.contract

/**
 * Requires that the given [Int] is in the range formed by [from] and [until]. If not, the exception
 * message [f] will be used to launch an [IndexOutOfBoundsException].
 *
 * @param index the index to check for.
 * @param from the lower boundary for [Int], inclusive.
 * @param until the upper boundary for [Int], exclusive.
 * @param f the error message builder.
 *
 * @throws IndexOutOfBoundsException if the condition is not respected.
 */
internal inline fun requireIn(index: Int, from: Int, until: Int, f: () -> String) {
  contract { callsInPlace(f, AT_MOST_ONCE) }
  if (index < from || index >= until) {
    val lazyMessage = f()
    throw IndexOutOfBoundsException(lazyMessage)
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
  if (index < from || index >= until) throw IndexOutOfBoundsException()
}

/**
 * Requires that the given [Int] is in the given [IntRange]. If not, the exception message [f] will
 * be used to launch an [IndexOutOfBoundsException].
 *
 * @param index the index to check for.
 * @param range the target [IntRange].
 * @param f the error message builder.
 *
 * @throws IndexOutOfBoundsException if the condition is not respected.
 */
internal inline fun requireIn(index: Int, range: IntRange, f: () -> String) {
  contract { callsInPlace(f, AT_MOST_ONCE) }
  if (index !in range) {
    val lazyMessage = f()
    throw IndexOutOfBoundsException(lazyMessage)
  }
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
  if (index !in range) throw IndexOutOfBoundsException()
}
