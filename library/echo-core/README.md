This document briefly describes the event log and history APIs, which can be used to store a sequence of events with their aggregated value.

<!--- TOC --->
- [The low-level event log API](#the-low-level-event-log-api)
  - [Event logs](#event-logs)
    - [A simple event log](#a-simple-event-log)
    - [Adding an aggregate](#adding-an-aggregate)
<!-- END -->

# The low-level event log API

## Event logs

### A simple event log

The event log is part of the [core](https://github.com/markdown-party/mono/tree/main/echo-core)
library. It's possible to merge multiple event logs using the dedicated `merge()` function, and
efficiently iterate over _all_ the events, or over the events that were issued by a _specific site_.

> In most cases, manually interacting with the event log won't be useful. However, in some special
> cases, you can optimize the event log and remove operations which aren't necessary to converge.
>
> For instance, **last-write-wins registers** work this way : the values associated with the
> previous writes to the register can be safely discarded.

The usage of an `EventLog` or a `MutableEventLog` somehow mimics the standard behavior of
collections :

```kotlin
// A SiteIdentifier uniquely identifies a site in the library. They can be constructed with an
// instance of Random.
val alice = Random.nextSiteIdentifier()
val bob = Random.nextSiteIdentifier()

// Events are just arrays of bytes. You should use the [echo] library if you're interested in
// working with generic types, which will automatically be serialized.
val event1: ByteArray = ..
val event2: ByteArray = ..
val event3: ByteArray = ..

val log = mutableEventLogOf().apply {
    // Appending an event means that the event will get a higher sequence number than all the
    // previously issued events for the given site. This is usually what you'll want to do when
    // issuing some new events.
    //
    // In this example, we have `seq1 == SequenceNumber.Min`, and `seq2 == SequenceNumber.Min + 1u`.
    val (seq1, _) = append(site = alice, event = event1)
    val (seq2, _) = append(site = alice, event = event2)

    // Inserting an event lets you specify both the site identifier and the sequence number for an
    // event. However, you can't insert events for a given site if the log has already seen a
    // sequence number greater or equal to it.
    insert(site = bob, seqno = SequenceNumber.Min, event = event3)
    insert(site = alice, seqno = SequenceNumber.Min, event = event1) // no effect
}

log.contains(SequenceNumber.Min + 0U, alice) // true
log.contains(SequenceNumber.Min + 2U, alice) // false
```

> Check the sample out [here](https://github.com/markdown-party/mono/tree/main/sample-walkthrough/src/main/kotlin/io/github/alexandrepiveteau/echo/samples/log/main.kt).

Event logs work better with consecutive insertions of operations, since they use some gap buffers
internally to store the events. You should therefore prefer calling `merge()` over
consecutive `insert()` or `append()` of events.

### Adding an aggregate

In most cases, you'll be interested in keeping an aggregated value associated with the head of your
log. This is how all the CRDTs in this project are implemented : the event log acts as a source of
truth for "what happened", and an aggregate is stored to indicate "what the current state is".
The [core](https://github.com/markdown-party/mono/tree/main/echo-core) library provides an
abstraction for that : a `MutableHistory<T>`. It's actually very simple :

```kotlin
// file MutableHistory.kt
interface MutableHistory<out T> : MutableEventLog {
    val current: T
}
```

When creating a `MutableHistory`, you have to provide 3 things :

+ An **initial** value for the history, before any event is added to the log.
+ A **forward** function, that applies an event to the history.
+ A **backward** function, which undoes an event.

The `forward` and `backward` functions should be put in
a [`MutableProjection<T>`](https://github.com/markdown-party/mono/blob/main/echo-core/src/commonMain/kotlin/io/github/alexandrepiveteau/echo/core/log/MutableProjection.kt)
. You'll then be able to create a `MutableHistory<T>` instance :

```kotlin
val projection: MutableProjection<Int> = .. // Your own subclass of MutableProjection<Int>
val history = mutableHistoryOf(initial = 0, projection = projection)

// ... and use it exactly like an event log.
history.append(alice, byteArrayOf(1, 2, 3))
```
