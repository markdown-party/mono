package party.markdown.rga

import kotlin.test.Test
import kotlin.test.assertEquals

// Eventually make a non-recursive and mutable implementation to have faster tests.
internal fun <T> List<T>.permutations(): Sequence<List<T>> {
  if (isEmpty()) return sequenceOf(emptyList())
  val head = get(0)
  val tails = drop(1).permutations()
  return sequence {
    for (tail in tails) {
      for (index in 0..tail.size) {
        yield(tail.toMutableList().apply { add(index, head) })
      }
    }
  }
}

class PermutationsTest {

  @Test
  fun empty() {
    assertEquals(setOf(emptyList()), emptyList<Nothing>().permutations().toSet())
  }

  @Test
  fun one() {
    assertEquals(setOf(listOf(1)), listOf(1).permutations().toSet())
  }

  @Test
  fun two() {
    assertEquals(setOf(listOf(1, 2), listOf(2, 1)), listOf(1, 2).permutations().toSet())
  }

  @Test
  fun three() {
    assertEquals(
        setOf(
            listOf(1, 2, 3),
            listOf(1, 3, 2),
            listOf(2, 1, 3),
            listOf(2, 3, 1),
            listOf(3, 1, 2),
            listOf(3, 2, 1),
        ),
        listOf(1, 2, 3).permutations().toSet(),
    )
  }

  @Test
  fun four() {
    assertEquals(
        setOf(
            // Starting with 1
            listOf(1, 2, 3, 4),
            listOf(1, 2, 4, 3),
            listOf(1, 3, 2, 4),
            listOf(1, 3, 4, 2),
            listOf(1, 4, 2, 3),
            listOf(1, 4, 3, 2),
            // Starting with 2
            listOf(2, 1, 3, 4),
            listOf(2, 1, 4, 3),
            listOf(2, 3, 1, 4),
            listOf(2, 3, 4, 1),
            listOf(2, 4, 1, 3),
            listOf(2, 4, 3, 1),
            // Starting with 3
            listOf(3, 1, 2, 4),
            listOf(3, 1, 4, 2),
            listOf(3, 2, 1, 4),
            listOf(3, 2, 4, 1),
            listOf(3, 4, 1, 2),
            listOf(3, 4, 2, 1),
            // Starting with 4
            listOf(4, 1, 2, 3),
            listOf(4, 1, 3, 2),
            listOf(4, 2, 1, 3),
            listOf(4, 2, 3, 1),
            listOf(4, 3, 1, 2),
            listOf(4, 3, 2, 1),
        ),
        listOf(1, 2, 3, 4).permutations().toSet(),
    )
  }
}
