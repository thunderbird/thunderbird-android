package app.k9mail.feature.migration.qrcode.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
internal fun QrCodeScannerView() {
    Column(modifier = Modifier.testTag("QrCodeScannerView")) {
        CameraPreviewView(
            modifier = Modifier.fillMaxSize(),
        )
    }
}
