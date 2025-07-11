package app.k9mail.feature.migration.qrcode.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.migration.qrcode.R
import app.k9mail.feature.migration.qrcode.domain.QrCodeDomainContract.UseCase
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.DisplayText
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId

@Composable
internal fun QrCodeScannerView(
    cameraUseCasesProvider: UseCase.CameraUseCasesProvider,
    displayText: DisplayText,
    onDoneClick: () -> Unit,
) {
    Column(modifier = Modifier.testTagAsResourceId("QrCodeScannerView")) {
        CameraPreviewView(
            cameraUseCasesProvider = cameraUseCasesProvider,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = MainTheme.spacings.double,
                    start = MainTheme.spacings.double,
                    end = MainTheme.spacings.double,
                )
                .weight(1f),
        )

        QrCodeScannerBottomContent(
            text = buildString(displayText),
            onDoneClick = onDoneClick,
        )
    }
}

@Composable
@ReadOnlyComposable
private fun buildString(text: DisplayText): String {
    return when (text) {
        DisplayText.HelpText -> {
            stringResource(R.string.migration_qrcode_scanning_instructions)
        }

        is DisplayText.ProgressText -> {
            stringResource(R.string.migration_qrcode_scanning_progress, text.scannedCount, text.totalCount)
        }
    }
}
