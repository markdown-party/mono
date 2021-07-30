This documents shows the process being integrating a new operation to the Markdown Party editor.

<!--- TOC --->
- [Example : adding a move operation for cursors](#example--adding-a-move-operation-for-cursors)
  - [Replicated growable arrays](#replicated-growable-arrays)
  - [Adding a new operation](#adding-a-new-operation)
  - [The projection](#the-projection)
    - [Compacting the history](#compacting-the-history)
  - [Final remarks](#final-remarks)
<!--- END --->

# Example : adding a move operation for cursors

In the previous sections, we've looked at the different primitives offered by the replication library. Let's now take a closer look at what it means to add a new operation to the Markdown Party editor.

When editing text, it's sometimes useful to know what the other users are doing. We'll therefore add a new feature to the text editor : we'll display the cursors of all the other editors.

## Replicated growable arrays

The text in Markdown Party is modelled using replicated growable arrays. A replicated growable array is defined by two operations : the **insertion**, which inserts a character _after_ an existing character, and the **removal**, which marks a character as deleted (but keeps a tombstone of the character identifier in the data structure). Because each character references its _anchor_, all the characters form a tree, which can be traversed using a depth-first search to obtain the final result.

```text
1. A very simple RGA, which (traversed via DFS) has the value 'alex!?'

+---+   +---+   +---+   +---+   +---+
| a |<--| l |<--| e |<--| x |<--| ! |
+---+   +---+   +---+   +---+   +---+
                  ^                  
                  |     +---+        
                  +-----| ? |        
                        +---+        

2. When we delete the 'e', we simply tombstone it, and obtain 'alx!?'.

+---+   +---+   +-+-+   +---+   +---+
| a |<--| l |<--+-+-+<--| x |<--| ! |
+---+   +---+   +-+-+   +---+   +---+
                  ^                  
                  |     +---+        
                  +-----| ? |        
                        +---+      

3. Notice how we insert a character in the middle of the string ... (yields 'alx!!?')

+---+   +---+   +-+-+   +---+   +---+   +---+
| a |<--| l |<--+-+-+<--| x |<--| ! |<--| ! |
+---+   +---+   +-+-+   +---+   +---+   +---+
                  ^                 
                  |     +---+        
                  +-----| ? |        
                        +---+ 

4. ... or at the end (yields 'alx!!?!')

+---+   +---+   +-+-+   +---+   +---+   +---+
| a |<--| l |<--+-+-+<--| x |<--| ! |<--| ! |
+---+   +---+   +-+-+   +---+   +---+   +---+
                  ^                 
                  |     +---+   +---+ 
                  +-----| ? |<--| ! |    
                        +---+   +---+
```

## Adding a new operation

We see that we can implement cursors moves through two operations :

1. Moving the cursor whenever a _site_ appends a new character to the sequence.
2. Adding a supplementary dedicated _move_ operation, when the user doesn't insert a new character, but simply repositions the cursor.

```kotlin
typealias CursorAnchorIdentifier = RGANodeIdentifier

/** The initial anchor for the cursor, when no events have been issued yet. */
val CursorRoot = RGANodeIdentifier.Unspecified

/** Our new operation */
@Serializable
sealed class CursorEvent {

  @Serializable
  data class MoveAfter(
      val anchor: CursorAnchorIdentifier,
  ) : CursorEvent()
}
```

> You can find the complete source [here](https://github.com/markdown-party/mono/tree/main/markdown/src/commonMain/kotlin/party/markdown/cursors/CursorEvent.kt).

## The projection

Our projection needs to have the following behavior : whenever a _site_  inserts a new character or moves the cursor, the character identifier on which the cursor is pointing should be updated. Therefore, one should only update the cursor if the _event identifier_ for the operation is greater than all the previous _event identifiers_ for the _site_.

Because these insertions and moves are **idempotent**, **commutative** and **associative** (they follow a LWW-register logic), we may can use a `OneWayProjection`. The projection simply delegates the logic to a `MutableCursors` instance :

```kotlin
// CursorProjection.kt
object CursorProjection : OneWayProjection<MutableCursors, MarkdownPartyEvent> {

  override fun forward(
      model: MutableCursors,
      identifier: EventIdentifier,
      event: MarkdownPartyEvent,
  ): MutableCursors {
    when (event) {
      is Cursor ->
          when (event.event) {
            is MoveAfter ->
                model.move(
                    id = identifier,
                    anchor = event.event.anchor,
                )
          }
      is RGA ->
          when (event.event) {
            is Insert ->
                model.insert(
                    id = identifier,
                    anchor = event.event.offset,
                )
          }
    }
    return model
  }
}
```

> You can find the complete source [here](https://github.com/markdown-party/mono/tree/main/markdown/src/commonMain/kotlin/party/markdown/cursors/CursorProjection.kt).

```kotlin
// MutableCursors.kt
class MutableCursors {

  internal val ids = mutableEventIdentifierGapBufferOf()
  internal val anchors = mutableEventIdentifierGapBufferOf()
  private val shouldCompact = mutableGapBufferOf<Boolean>() // true if the previous op can be compacted

  private fun put(
      id: EventIdentifier,
      anchor: CursorAnchorIdentifier,
      compact: Boolean,
  ) {
    val index = ids.binarySearchBySite(id.site)
    if (index < 0) { // insert the move for the cursor, since the site does not exist
      val insertion = -(index + 1)
      ids.push(id, offset = insertion)
      anchors.push(anchor, offset = insertion)
      shouldCompact.push(compact, offset = insertion)
    } else {
      if (ids[index].seqno >= id.seqno) return // only update if we have a greater sequence number
      ids[index] = id
      anchors[index] = anchor
      shouldCompact[index] = compact
    }
  }

  fun move(
      id: EventIdentifier,
      anchor: CursorAnchorIdentifier,
  ): Unit = put(id, anchor, compact = true)

  fun insert(
      id: EventIdentifier,
      anchor: CursorAnchorIdentifier,
  ): Unit = put(id, anchor, compact = false)

  internal fun previous(id: EventIdentifier): EventIdentifier {
    val index = ids.binarySearchBySite(id.site)
    if (index >= 0 && ids[index].seqno < id.seqno && shouldCompact[index]) return ids[index]
    return EventIdentifier.Unspecified
  }
}
```

> You can find the complete source [here](https://github.com/markdown-party/mono/tree/main/markdown/src/commonMain/kotlin/party/markdown/cursors/MutableCursors.kt). The actual implemention handles more things, such as multiple documents, and physical timestamps management.

The cursor for a given _site_ could then be extracted as follows :

1. Check whether the `ids` buffer contains the site identifier.
2. If so, access the associated anchor identifier. This is where the cursor points.
3. Finally, look at the RGA to find the anchor identifier, and return the cursor position.

### Compacting the history

It turns out that the `MoveAfter` operation may be compacted, because it follows LWW semantics and is not required by other operations after it's been overriden by a new operation (unlike the `Insert` operation, which is still required by the RGA). We can therefore have a custom `MutableHistory` implementation which removes these irrerelevant operations :

```kotlin
class MarkdownPartyHistory :
    AbstractMutableHistory<MutableMarkdownParty>(
        initial = MutableMarkdownParty(),
        projection = Projection,
    ) {

  override fun partialInsert(
      id: EventIdentifier,
      array: ByteArray,
      from: Int,
      until: Int,
  ) {
    val operationArray = array.copyOfRange(from, until)

    // 1. Look at the type of the operation.
    // 2. Query the identifier of the operation to remove from the history.
    // 3. Partially remove the operation if appropriate.
    when (val event = decodeFromByteArray(MarkdownPartyEvent.serializer(), operationArray)) {
      is Cursor -> handleCursor(id)
      is RGA ->
          when (event.event) {
            is Insert -> handleCursor(id)
            is Remove -> Unit
          }
    }

    // 4. Insert the operation. Because we know that our operation will only be inserted for a given
    // site if it has a greater sequence number than all the previously inserted operations, we are
    // guaranteed never to have to skip the partial insertion.
    super.partialInsert(id, array, from, until)
  }

  private fun handleCursor(id: EventIdentifier) {
    val previous = current.cursors.previous(id)
    if (previous.isUnspecified || previous >= id) return // Do not remove that op.
    partialRemove(previous.seqno, previous.site)
  }
}
```

> You can find the complete source [here](https://github.com/markdown-party/mono/tree/main/markdown/src/commonMain/kotlin/party/markdown/MarkdownPartyHistory.kt).

Notice how the `handleCursor` method calls `partialRemove` only if the previous event is a compactible operation (aka a `MoveAfter`).

## Final remarks

Finally, we can integrate the resulting CRDT in the Markdown Party user interface. In the case of the CodeMirror integration, the [reconciliation algorithm](https://github.com/markdown-party/mono/blob/main/markdown-frontend/src/main/kotlin/party/markdown/ui/editor/Editor.kt) was slightly tweaked to also account for cursors, and to make sure the current cursor position is emitted if it has changed synce the last sync. The final result with the integration can be seen [PR #136](https://github.com/markdown-party/mono/pull/136).
