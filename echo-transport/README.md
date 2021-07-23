This document features multiple integrations of the Echo library, and explains how to replicate data over websockets.

<!--- TOC --->
- [Integrations](#integrations)
  - [Replication basics](#replication-basics)
  - [Websocket replication](#websocket-replication)
    - [Server](#server)
    - [Client](#client)
<!--- END --->

# Integrations

## Replication basics

Up until now, all the interactions we had with a _site_ occurred on a single machine. Also, we didn't really look at the API surface that lets _sites_ communicate with each other, and how we may use it to replicate content across multiple physical machines.

Additionally, while _sites_ do, indeed, offer replication, they're not the lowest level abstraction that does so. Let's look at the inheritance hierarchy of `MutableSite` :

```text
The Site and MutableSite inheritance hierarchy:

 +-----------------+  +--------------+    	               
 | ReceiveExchange |  | SendExchange |                    
 +-----------------+  +--------------+                    
         ^                   ^                            
         |                   |                            
         +---------+---------+                            
                   |                         
             +----------+                                
             | Exchange |                                
             +----------+                                
                   |                                          
               +------+                                  
               | Site |                                  
               +------+                                  
                   |                                     
            +-------------+                               
            | MutableSite |                               
            +-------------+       
```

In fact, the `ReceiveExchange` and `SendExchange` interfaces are the lowest-level components that can sync content. The replication protocol is asymmetric : a `ReceiveExchange` will receive requests from other _sites_, and respond with the events it has. A `SendExchange` starts by sending some requests, and will then receive the events from a `ReceiveExchange`.

Both `ReceiveExchange` and `SendExchange` offer a `Link` that lets them emit and send some messages :

```kotlin
// Link.kt
fun interface Link<in I, out O> {
  fun talk(incoming: Flow<I>): Flow<O>
}

 // Exchange.kt
fun interface SendExchange<in I, out O> {
  fun outgoing(): Link<I, O>
}
fun interface ReceiveExchange<out I, in O> {
  fun incoming(): Link<O, I>
}
```

A `Link` exposes a cold asymmetric communication channel, based on flows. The protocol messages will then flow on the links. The two `sync()` methods then simply "glue" two opposite `Link` together, or two compatible `Exchange<I, O>` :

```kotlin
// The first sync method makes use of some rendez-vous channels to establish
// communication between two opposing Link.
suspend fun <I, O> sync(
    first: Link<I, O>,
    second: Link<O, I>,
): Unit = coroutineScope {
  val fToS = Channel<O>()
  val sToF = Channel<I>()
  launch {
    first
        .talk(sToF.consumeAsFlow())
        .onEach { fToS.send(it) }
        .onCompletion { fToS.close() }
        .collect()
  }
  launch {
    second
        .talk(fToS.consumeAsFlow())
        .onEach { sToF.send(it) }
        .onCompletion { sToF.close() }
        .collect()
  }
}

// The second sync method groups exchanges by pairs, and establishes the sync
// process for each pair <incoming(), outgoing()>.
suspend fun <I, O> sync(
    vararg exchanges: Exchange<I, O>,
): Unit = coroutineScope {
  exchanges.asSequence().windowed(2).forEach { (left, right) ->
    launch { sync(left.incoming(), right.outgoing()) }
    launch { sync(left.outgoing(), right.incoming()) }
  }
}
```

> The complete source is available [here](https://github.com/markdown-party/mono/tree/main/echo/src/commonMain/kotlin/io/github/alexandrepiveteau/echo/Sync.kt).

As we've seen, an `Exchange` simply implements both `SendExchange` and `ReceiveExchange`. What's the difference with a `Site` then ?

_Sites_ have two additional properties : they provide access to an **observable aggregated model** from the events, and they do not manage generic messages (instead, they use the [echo protocol messages, which are necessary to replicate events](https://github.com/markdown-party/mono/tree/main/echo/src/commonMain/kotlin/io/github/alexandrepiveteau/echo/protocol/Message.kt)). The aggregated value is the `Site.value` flow that we have been using throughout the examples. On the other hand, _exchanges_ are a great abstraction for the replication protocol, since they do not care about the type of the underlying events, as they do not try to aggregate them.

## Websocket replication

Now that we've seen that the communication protocol is implemented with flows of messages, and that the `sync()` function can work with generic messages, it becomes clear that it will be easy to replicate _sites_ across different machines, assuming we can transmit the protocol messages. For example, to send messages over websockets, two steps are needed :

1. Transform the protocol messages to websocket frames
2. Send the websocket frames through a dedicated websocket library

The [echo-transport](https://github.com/markdown-party/mono/tree/main/echo-transport) library solves the first problem by offering some `encodeToFrame()` and `decodeToFrame()` methods on `Exchange` :

```kotlin
val remote: Exchange<Frame, Frame> = remote() // obtained through the client or server integrations
val local = mutableSite(identifier, initial, projection)

sync(remote, local.encodeToFrame()) // sync over the network
```

Internally, the `encodeToFrame()` and `decodeToFrame()` functions make use of Protobufs to efficiently store the messages in binary format, which are then mapped to binary websocket frames.

### Server

The [echo-ktor-server](https://github.com/markdown-party/mono/tree/main/echo-ktor-server) module provides some server-side integration to serve a websocket-based `Exchange` over a websocket connection. In fact, building a generic replication server can be done in exactly 10 lines of code:

```kotlin
fun main() {
  val site = exchange()
  embeddedServer(CIO, port = 1234) {
        install(WebSockets)
        routing {
          route("snd") { sender { site.encodeToFrame() } }
          route("rcv") { receiver { site.encodeToFrame() } }
        }
      }
      .start(wait = true)
}
```

> Check the sample out [here](https://github.com/markdown-party/mono/tree/main/sample-walkthrough/src/main/kotlin/io/github/alexandrepiveteau/echo/samples/integrations/server/main.kt).

In this example, a generic `Exchange`, which handles encoded events, is served through the `/snd` and `/rcv` endpoints. Note that you could also decide to specify a different `SyncStrategy` on the exchange, for instance if you'd like to enforce one-shot sync rather than continuous sync.

The `sender { }` and `receiver { }` are similar to a `webSocket { }` block, and let you use the request parameters to decide which instance of `Exchange` the caller should interact with. This is particularly useful when you want to feature [multiple independent collaborative sessions](https://github.com/markdown-party/mono/blob/main/markdown-backend/src/main/kotlin/io/github/alexandrepiveteau/markdown/backend/main.kt#L26-L37).

### Client

The [echo-ktor-client](https://github.com/markdown-party/mono/tree/main/echo-ktor-client) modules integrates well with websockets managed by the server library :

```kotlin
private val Client = HttpClient(Js) { 
    install(WebSockets) 
}

val remote = Client.wssExchange(receiver = { url("/snd") }, sender = { url("/rcv") })
val local = mutableSite(identifier, initial, projection)

sync(remote, local.encodeToFrame()) // sync over the network
```

> Check the sample out [here](https://github.com/markdown-party/mono/tree/main/sample-walkthrough/src/main/kotlin/io/github/alexandrepiveteau/echo/samples/integrations/client/main.kt).

In this example, an exchange is generated over a secure websocket connection, using an `HttpClient`. It's also possible to create exchanges over insecure connections using the `HttpClient.wsExchange(...)` builder.
