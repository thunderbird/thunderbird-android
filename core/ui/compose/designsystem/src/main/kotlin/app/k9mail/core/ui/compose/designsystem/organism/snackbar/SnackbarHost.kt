package app.k9mail.core.ui.compose.designsystem.organism.snackbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.material3.SnackbarHost as Material3SnackbarHost
import androidx.compose.material3.SnackbarHostState as Material3SnackbarHostState

/**
 * Snackbars provide brief messages about app processes at the bottom of the screen.
 *
 * It uses the Material 3 [SnackbarHost] implementation under the hood.
 *
 * @param hostState state of this component to manage Snackbar show/dismiss timings.
 * @param modifier the [Modifier] to be applied to this component.
 */
@Composable
fun SnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Material3SnackbarHost(
        hostState = (hostState as Material3BackedSnackbarHostState).m3State,
        modifier = modifier,
    )
}

/**
 * Creates a [SnackbarHostState] that is remembered across compositions.
 */
@Composable
fun rememberSnackbarHostState(): SnackbarHostState {
    return remember { Material3BackedSnackbarHostState(m3State = Material3SnackbarHostState()) }
}
