package app.k9mail.feature.migration.qrcode.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.Surface

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
