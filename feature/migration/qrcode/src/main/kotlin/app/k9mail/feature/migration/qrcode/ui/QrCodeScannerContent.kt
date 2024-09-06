package app.k9mail.feature.migration.qrcode.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.Event
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.State
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.UiPermissionState

@Composable
internal fun QrCodeScannerContent(
    state: State,
    onEvent: (Event) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        when (state.cameraPermissionState) {
            UiPermissionState.Unknown -> {
                // Display empty surface while we're waiting for the camera permission request to return a result
            }
            UiPermissionState.Granted -> {
                QrCodeScannerView()
            }
            UiPermissionState.Denied -> {
                PermissionDeniedContent(
                    onGoToSettingsClick = { onEvent(Event.GoToSettingsClicked) },
                )
            }
            UiPermissionState.Waiting -> {
                // We've launched Android's app info screen and are now waiting for the user to return to our app.

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    // Once the user has returned to the app, notify the view model about it.
                    onEvent(Event.ReturnedFromAppInfoScreen)
                }
            }
        }
    }
}
