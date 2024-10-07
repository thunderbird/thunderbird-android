package app.k9mail.feature.migration.qrcode.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.State
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.UiPermissionState

@Preview
@Composable
fun QrCodeScannerScreenPreview_permission_unknown() {
    PreviewWithTheme(isDarkTheme = true) {
        QrCodeScannerScreen(
            finishWithResult = {},
            finish = {},
            viewModel = NoOpQrCodeScannerViewModel(
                initialState = State(cameraPermissionState = UiPermissionState.Unknown),
            ),
        )
    }
}

@Preview
@Composable
fun QrCodeScannerScreenPreview_permission_granted() {
    PreviewWithTheme(isDarkTheme = true) {
        QrCodeScannerScreen(
            finishWithResult = {},
            finish = {},
            viewModel = NoOpQrCodeScannerViewModel(
                initialState = State(cameraPermissionState = UiPermissionState.Granted),
            ),
        )
    }
}

@Preview
@Composable
fun QrCodeScannerScreenPreview_permission_denied() {
    PreviewWithTheme(isDarkTheme = true) {
        QrCodeScannerScreen(
            finishWithResult = {},
            finish = {},
            viewModel = NoOpQrCodeScannerViewModel(
                initialState = State(cameraPermissionState = UiPermissionState.Denied),
            ),
        )
    }
}
