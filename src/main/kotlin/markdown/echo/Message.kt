package markdown.echo

object Message {

    sealed class V1<out T> {
        sealed class Incoming<out T> : V1<T>()
        sealed class Outgoing<out T> : V1<T>()
    }
}
