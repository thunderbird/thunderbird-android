package app.k9mail.feature.migration.qrcode.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.Effect
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.Event
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.State
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.UiPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class QrCodeScannerViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), QrCodeScannerContract.ViewModel {

    override fun event(event: Event) {
        when (event) {
            Event.StartScreen -> handleOneTimeEvent(event, ::handleStartScreen)
            is Event.CameraPermissionResult -> handleCameraPermissionResult(event.success)
            Event.GoToSettingsClicked -> handleGoToSettingsClicked()
            Event.ReturnedFromAppInfoScreen -> handleReturnedFromAndroidSettings()
        }
    }

    private fun handleStartScreen() {
        requestCameraPermission()
    }

    private fun handleCameraPermissionResult(success: Boolean) {
        updateState {
            it.copy(cameraPermissionState = if (success) UiPermissionState.Granted else UiPermissionState.Denied)
        }
    }

    private fun handleGoToSettingsClicked() {
        emitEffect(Effect.GoToAppInfoScreen)

        viewModelScope.launch {
            // Delay updating the UI to make sure Android's app settings screen is active and our activity is paused.
            // We want to prevent QrCodeScannerContent triggering the permission dialog again before the user had a
            // chance to enter Android's app settings screen.
            delay(APP_SETTINGS_DELAY)

            updateState {
                it.copy(cameraPermissionState = UiPermissionState.Waiting)
            }
        }
    }

    private fun handleReturnedFromAndroidSettings() {
        updateState {
            it.copy(cameraPermissionState = UiPermissionState.Unknown)
        }

        requestCameraPermission()
    }

    private fun requestCameraPermission() {
        emitEffect(Effect.RequestCameraPermission)
    }
}

private const val APP_SETTINGS_DELAY = 100L
