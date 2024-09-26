package app.k9mail.feature.migration.qrcode.ui

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel

interface QrCodeScannerContract {
    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val cameraPermissionState: UiPermissionState = UiPermissionState.Unknown,
    )

    sealed interface Event {
        data object StartScreen : Event
        data class CameraPermissionResult(val success: Boolean) : Event
        data object GoToSettingsClicked : Event
        data object ReturnedFromAppInfoScreen : Event
    }

    sealed interface Effect {
        data object RequestCameraPermission : Effect
        data object GoToAppInfoScreen : Effect
    }

    enum class UiPermissionState {
        Unknown,
        Granted,
        Denied,
        Waiting,
    }
}
