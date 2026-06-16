package net.thunderbird.feature.debug.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import net.thunderbird.components.ui.bolt.PreviewWithThemesLightDark
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.theme.MainTheme

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
