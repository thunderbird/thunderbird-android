package net.thunderbird.components.ui.bolt.atom.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import net.thunderbird.components.ui.bolt.PreviewWithThemesLightDark
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.theme.MainTheme

@PreviewLightDark
@Composable
private fun CardOutlinedPreview() {
    PreviewWithThemesLightDark {
        Surface(modifier = Modifier.padding(MainTheme.spacings.quadruple)) {
            CardOutlined {
                Box(modifier = Modifier.padding(MainTheme.spacings.double)) {
                    TextBodyMedium("Text in card")
                }
            }
        }
    }
}
