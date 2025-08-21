package app.k9mail.core.ui.compose.designsystem.organism.snackbar

import androidx.compose.material3.SnackbarDuration as Material3SnackbarDuration

/** Possible durations of the [Snackbar] in [SnackbarHost] */
enum class SnackbarDuration {
    /** Show the Snackbar for a short period of time */
    Short,

    /** Show the Snackbar for a long period of time */
    Long,

    /** Show the Snackbar indefinitely until explicitly dismissed or action is clicked */
    Indefinite,
}

internal fun SnackbarDuration.toMaterial3SnackbarDuration(): Material3SnackbarDuration = when (this) {
    SnackbarDuration.Short -> Material3SnackbarDuration.Short
    SnackbarDuration.Long -> Material3SnackbarDuration.Long
    SnackbarDuration.Indefinite -> Material3SnackbarDuration.Indefinite
}
