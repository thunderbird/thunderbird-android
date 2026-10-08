package app.k9mail.feature.migration.qrcode.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.DisplayText
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.Surface

@Preview
@Composable
fun QrCodeScannerViewPreview_initial() {
    PreviewWithTheme(isDarkTheme = true) {
        Surface {
            QrCodeScannerView(
                cameraUseCasesProvider = { emptyList() },
                displayText = DisplayText.HelpText,
                onDoneClick = {},
            )
        }
    }
}

@Preview
@Composable
fun QrCodeScannerViewPreview_one_qr_code_scanned() {
    PreviewWithTheme(isDarkTheme = true) {
        Surface {
            QrCodeScannerView(
                cameraUseCasesProvider = { emptyList() },
                DisplayText.ProgressText(scannedCount = 1, totalCount = 2),
                onDoneClick = {},
            )
        }
    }
}
