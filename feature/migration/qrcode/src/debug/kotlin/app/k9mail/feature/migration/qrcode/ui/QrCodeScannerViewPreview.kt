package app.k9mail.feature.migration.qrcode.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.core.ui.compose.designsystem.atom.Surface

@Preview
@Composable
fun QrCodeScannerViewPreview_initial() {
    PreviewWithTheme {
        Surface {
            QrCodeScannerView(
                cameraUseCasesProvider = { emptyList() },
                scannedCount = 0,
                totalCount = 0,
                onDoneClick = {},
            )
        }
    }
}

@Preview
@Composable
fun QrCodeScannerViewPreview_one_qr_code_scanned() {
    PreviewWithTheme {
        Surface {
            QrCodeScannerView(
                cameraUseCasesProvider = { emptyList() },
                scannedCount = 1,
                totalCount = 2,
                onDoneClick = {},
            )
        }
    }
}
