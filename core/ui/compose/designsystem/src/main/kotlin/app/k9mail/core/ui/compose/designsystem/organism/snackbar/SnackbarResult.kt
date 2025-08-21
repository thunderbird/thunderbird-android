package app.k9mail.core.ui.compose.designsystem.organism.snackbar

import androidx.compose.material3.SnackbarResult as Material3SnackbarResult

/** Possible results of the [SnackbarHostState.showSnackbar] call */
enum class SnackbarResult {
    /** [Snackbar] that is shown has been dismissed either by timeout of by user */
    Dismissed,

    /** Action on the [Snackbar] has been clicked before the time out passed */
    ActionPerformed,
}

internal fun Material3SnackbarResult.toSnackbarResult(): SnackbarResult = when (this) {
    Material3SnackbarResult.Dismissed -> SnackbarResult.Dismissed
    Material3SnackbarResult.ActionPerformed -> SnackbarResult.ActionPerformed
}
