package app.k9mail.core.ui.compose.designsystem.atom.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
@Preview(showBackground = true)
internal fun CardElevatedPreview() {
    PreviewWithThemes {
        CardElevated {
            Box(modifier = Modifier.padding(MainTheme.spacings.double)) {
                TextBodyMedium("Text in card")
            }
        }
    }
}
