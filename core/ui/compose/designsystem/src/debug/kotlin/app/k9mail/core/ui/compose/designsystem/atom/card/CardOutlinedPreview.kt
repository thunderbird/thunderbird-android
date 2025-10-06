package app.k9mail.core.ui.compose.designsystem.atom.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme

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
