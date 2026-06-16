package net.thunderbird.components.ui.bolt.atom.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.theme.MainTheme

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
