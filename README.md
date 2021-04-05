# kotlin-echo

[![.github/workflows/tests.yml](https://github.com/markdown-party/kotlin-echo/actions/workflows/tests.yml/badge.svg?branch=main)](https://github.com/markdown-party/kotlin-echo/actions/workflows/tests.yml)

`markdown-party/kotlin-echo` is a library that manages a distributed log of operations, and provides
some simple abstractions to replicate events across multiple sites. It makes heavy use of
[Kotlin coroutines](https://kotlinlang.org/docs/coroutines-guide.html), so concurrent communication
with multiple sites is possible.

### About

I'm developing this library as part of my BSc. thesis at HEIG-VD on building a distributed Markdown
editor. Feel free to reach out to me at
[alexandre.piveteau@heig-vd.ch](mailto:alexandre.piveteau@heig-vd.ch).

## Using the library

### Installation

To be announced.

### Usage example

Let's implement a distributed counter, which lets sites increment and decrement a shared value. We
start by defining the events, as well as a `OneWayProjection` that aggregates them :

```kotlin
enum class Event { Increment, Decrement }
typealias State = Int

// A simple aggregation function, which increments a value for [Increment] events, and decrements
// the same value for [Decrement] events.
val counter = OneWayProjection<Int, EventValue<Event>> { op, acc ->
    when (op.value) {
        Increment -> acc + 1
        Decrement -> acc - 1
    }
}
```

We can then create a new site, and yield some new events :

```kotlin
val site = mutableSite<Event, State>(

    // This is the site identifier. It's globally unique, and makes sure multiple sites can't
    // create identical events.
    identifier = SiteIdentifier.random(),

    // This is the initial value of the aggregating function. You can see it as the "base state"
    // of your distributed data structure, or the starting value of the data structure when nobody
    // has touched it.
    initial = 0,

    // This is the aggregation function, that aggregates the events in a local state.
    projection = counter,
)

// This is a suspend fun, which resumes once the event { ... } block will have been successfully
// applied to the underlying site.
site.event {
    yield(Increment)
}
```

It's then possible to observe the values of a site as a cold `Flow` :

```kotlin
val total: Flow<State> = site.value // emits [0, 1, ...]
```

As new events get `yield` in the `MutableSite`, the cold `Flow` will emit some additional elements
which contain the distributed counter total.

At some point, you may be interested in syncing multiple sites together. This can be done with a
suspending actor pattern, which will not resume until both sites cooperatively finish :

```kotlin
suspend fun myFun() {
    val alice = mutableSite<Event>(SiteIdentifier.random())
    val bob = mutableSite<Event>(SiteIdentifier.random())

    sync(alice, bob)
}
```

Additional examples are available in the [demo folder](src/test/kotlin/markdown/echo/demo).

## Local setup

This project uses Kotlin 1.4.32 and is build with [Gradle](https://gradle.org). To run the unit
tests locally, please proceed as follows :

```bash
# Clone the repository locally.
> git clone git@github.com:markdown-party/kotlin-echo.git && cd kotlin-echo

# Run Gradle tests.
> ./gradlew test
```
