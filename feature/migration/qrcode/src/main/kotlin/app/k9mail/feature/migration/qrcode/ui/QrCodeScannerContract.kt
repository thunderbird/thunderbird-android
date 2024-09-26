package app.k9mail.feature.migration.qrcode.ui

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.migration.qrcode.domain.QrCodeDomainContract.UseCase

internal interface QrCodeScannerContract {
    interface ViewModel : UnidirectionalViewModel<State, Event, Effect> {
        val cameraUseCasesProvider: UseCase.CameraUseCasesProvider
    }

    data class State(
        val cameraPermissionState: UiPermissionState = UiPermissionState.Unknown,
        val scannedCount: Int = 0,
        val totalCount: Int = 0,
    )

    sealed interface Event {
        data object StartScreen : Event
        data class CameraPermissionResult(val success: Boolean) : Event
        data object GoToSettingsClicked : Event
        data object ReturnedFromAppInfoScreen : Event
        data object DoneClicked : Event
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
