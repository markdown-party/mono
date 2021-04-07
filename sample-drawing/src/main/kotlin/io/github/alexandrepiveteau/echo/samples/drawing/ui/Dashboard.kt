package io.github.alexandrepiveteau.echo.samples.drawing.ui

import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A composable which displays the dashboard with various meta-data on the current state of the
 * application, and lets the user tweak connectivity settings.
 *
 * @param syncing true if the app is currently syncing.
 * @param onSyncingToggled a callback called when the sync status is changed by the user.
 * @param modifier the [Modifier] for this composable.
 */
@Composable
fun Dashboard(
    syncing: Boolean,
    onSyncingToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
  Switch(
      checked = syncing,
      onCheckedChange = onSyncingToggled,
      modifier = modifier,
  )
}
