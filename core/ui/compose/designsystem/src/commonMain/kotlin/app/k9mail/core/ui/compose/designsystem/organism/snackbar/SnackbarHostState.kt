package app.k9mail.core.ui.compose.designsystem.organism.snackbar

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.runtime.Stable
import androidx.compose.material3.SnackbarHostState as Material3SnackbarHostState

/**
 * State of the [SnackbarHost], which controls the queue and the current [Snackbar] being shown
 * inside the [SnackbarHost].
 *
 * This state is usually [remembered][rememberSnackbarHostState] and used to provide a [SnackbarHost] to a [Scaffold].
 *
 * @see rememberSnackbarHostState
 */
@Stable
sealed interface SnackbarHostState {
    /**
     * Shows or queues to be shown a [Snackbar] at the bottom of the [Scaffold] to which this state
     * is attached and suspends until the snackbar has disappeared.
     *
     * [SnackbarHostState] guarantees to show at most one snackbar at a time. If this function is
     * called while another snackbar is already visible, it will be suspended until this snackbar is
     * shown and subsequently addressed. If the caller is cancelled, the snackbar will be removed
     * from display and/or the queue to be displayed.
     *
     * To change the Snackbar appearance, change it in 'snackbarHost' on the [Scaffold].
     *
     *  @param message text to be shown in the Snackbar
     *  @param actionLabel optional action label to show as button in the Snackbar
     *  @param withDismissAction a boolean to show a dismiss action in the Snackbar. This is
     *    recommended to be set to true for better accessibility when a Snackbar is set with a
     *    [SnackbarDuration.Indefinite]
     *  @param duration duration to control how long snackbar will be shown in [SnackbarHost], either
     *    [SnackbarDuration.Short], [SnackbarDuration.Long] or [SnackbarDuration.Indefinite].
     *  @return [SnackbarResult.ActionPerformed] if option action has been clicked or
     *    [SnackbarResult.Dismissed] if snackbar has been dismissed via timeout or by the user
     */
    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration =
            if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite,
    ): SnackbarResult
}

@Stable
internal data class Material3BackedSnackbarHostState(
    val m3State: Material3SnackbarHostState,
) : SnackbarHostState {
    override suspend fun showSnackbar(
        message: String,
        actionLabel: String?,
        withDismissAction: Boolean,
        duration: SnackbarDuration,
    ): SnackbarResult {
        val m3Result = m3State.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            withDismissAction = withDismissAction,
            duration = duration.toMaterial3SnackbarDuration(),
        )

        return m3Result.toSnackbarResult()
    }
}
