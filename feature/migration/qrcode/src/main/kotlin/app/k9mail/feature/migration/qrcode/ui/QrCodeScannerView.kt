package app.k9mail.feature.migration.qrcode.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.k9mail.feature.migration.qrcode.domain.QrCodeDomainContract.UseCase

@Composable
internal fun QrCodeScannerView(
    cameraUseCasesProvider: UseCase.CameraUseCasesProvider,
    scannedCount: Int,
    totalCount: Int,
    onDoneClick: () -> Unit,
) {
    Column(modifier = Modifier.testTag("QrCodeScannerView")) {
        CameraPreviewView(
            cameraUseCasesProvider = cameraUseCasesProvider,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        QrCodeScannerBottomContent(
            scannedCount = scannedCount,
            totalCount = totalCount,
            onDoneClick = onDoneClick,
        )
    }
}
