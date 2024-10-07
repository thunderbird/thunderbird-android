package app.k9mail.feature.migration.qrcode.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.migration.qrcode.R

@Composable
internal fun QrCodeScannerBottomContent(
    scannedCount: Int,
    totalCount: Int,
    onDoneClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (totalCount > 0) {
            TextBodyLarge(
                text = stringResource(R.string.migration_qrcode_scanning_progress, scannedCount, totalCount),
                modifier = Modifier
                    .testTag("ScannedStatus")
                    .padding(vertical = MainTheme.spacings.double)
                    .padding(start = MainTheme.spacings.double)
                    .weight(1f),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        ButtonOutlined(
            text = stringResource(R.string.migration_qrcode_done_button_text),
            onClick = onDoneClick,
            modifier = Modifier
                .testTag("DoneButton")
                .padding(MainTheme.spacings.double),
        )
    }
}
