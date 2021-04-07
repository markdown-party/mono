package io.github.alexandrepiveteau.echo.samples.drawing.ui.features.network

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FakeParticipants(
    modifier: Modifier = Modifier,
) {
  Column(modifier, Arrangement.spacedBy(8.dp), Alignment.End) {
    FakeParticipant("Alice")
    FakeParticipant("Bob")
    FakeParticipant("Charlie")
  }
}

@Composable
fun FakeParticipant(
    name: String,
    modifier: Modifier = Modifier,
) {
  var connected by remember { mutableStateOf(true) }
  Participant(
      name = name,
      connected = connected,
      onClick = { connected = !connected },
      modifier = modifier,
  )
}

/**
 * A pill displaying some information related to a participant, and provides a way to toggle it
 * on/off as needed. Usually, a participant is a site from whom we are actively receiving events.
 *
 * @param name the display name of the participant.
 * @param connected true if we're currently connected to the participant.
 * @param onClick a callback that's called when the participant pill is clicked.
 * @param modifier the [Modifier] for this composable.
 */
@Composable
fun Participant(
    name: String,
    connected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val emphasis by animateFloatAsState(if (connected) ContentAlpha.medium else ContentAlpha.disabled)
  val elevation by animateDpAsState(if (connected) 4.dp else 2.dp)
  Surface(modifier, elevation = elevation, shape = CircleShape) {
    CompositionLocalProvider(LocalContentAlpha provides emphasis) {
      Row(
          Modifier.clickable { onClick() }.padding(horizontal = 12.dp, vertical = 8.dp),
          Arrangement.spacedBy(8.dp),
          Alignment.CenterVertically,
      ) {
        Crossfade(connected) { on ->
          val icon = if (on) Icons.Outlined.Cloud else Icons.Outlined.CloudOff
          Icon(icon, null, Modifier.size(20.dp))
        }
        Text(name, style = MaterialTheme.typography.caption)
      }
    }
  }
}
