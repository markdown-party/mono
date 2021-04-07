package io.github.alexandrepiveteau.echo.samples.drawing.data.config

/**
 * A [Participant] is a site that will participate in the collaborative editing session. It can be
 * reached through websockets as a receiver.
 *
 * In this example, each [Participant] hosts a sender, and receives from other sites.
 *
 * @param host the host name.
 * @param port the connection port.
 * @param path the path to which to connect.
 * @param name the display name of the participant.
 */
data class Participant(
    val host: String,
    val port: Int,
    val path: String,
    val name: String,
)

/**
 * A [Config] contains configuration information for the network.
 *
 * @param me the [Participant] for the current site.
 * @param participant the [List] of all [Participant] to which we may connect.
 */
data class Config(
    val me: Participant,
    val participant: List<Participant>,
) {

  companion object {

    // Members.
    private val Alice = Participant("localhost", 8080, "sync", name = "Alice")
    private val Bob = Participant("localhost", 8081, "sync", name = "Bob")
    private val Charlie = Participant("localhost", 8082, "sync", name = "Charlie")

    // Site configs.
    private val AliceConfig = Config(Alice, listOf(Bob, Charlie))
    private val BobConfig = Config(Bob, listOf(Alice, Charlie))
    private val CharlieConfig = Config(Charlie, listOf(Alice, Bob))

    /** Parses a [Config] from some command-line arguments. */
    fun parse(args: Array<String>): Config? {
      // TODO : extract config from CLI arguments instead ?
      if (args[0].toLowerCase() == "alice") return AliceConfig
      if (args[0].toLowerCase() == "bob") return BobConfig
      if (args[0].toLowerCase() == "charlie") return CharlieConfig
      println(args[0])
      return null
    }
  }
}
