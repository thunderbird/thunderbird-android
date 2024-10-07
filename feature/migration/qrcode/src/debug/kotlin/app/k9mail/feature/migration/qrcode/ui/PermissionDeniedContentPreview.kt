package app.k9mail.feature.migration.qrcode.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.core.ui.compose.designsystem.atom.Surface

@PreviewScreenSizes
@Composable
fun PermissionDeniedContentPreview() {
    PreviewWithTheme(isDarkTheme = true) {
        Surface {
            PermissionDeniedContent(
                onGoToSettingsClick = {},
            )
        }
    }
}
