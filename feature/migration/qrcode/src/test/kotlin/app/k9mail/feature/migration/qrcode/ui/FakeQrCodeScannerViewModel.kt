package app.k9mail.feature.migration.qrcode.ui

import app.k9mail.core.ui.compose.testing.BaseFakeViewModel
import app.k9mail.feature.migration.qrcode.domain.QrCodeDomainContract.UseCase
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.Effect
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.Event
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.State

internal class FakeQrCodeScannerViewModel(
    initialState: State = State(),
) : BaseFakeViewModel<State, Event, Effect>(initialState), QrCodeScannerContract.ViewModel {
    override val cameraUseCasesProvider = UseCase.CameraUseCasesProvider { emptyList() }
}
