package net.thunderbird.feature.debugSettings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.theme2.MainTheme

@PreviewLightDark
@Composable
private fun DebugSectionPreview() {
    PreviewWithThemesLightDark {
        Box(modifier = Modifier.padding(MainTheme.spacings.triple)) {
            DebugSection(
                title = "Debug section",
            ) {
                TextBodyLarge("Content")
            }
        }
    }
}
