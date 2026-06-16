package net.thunderbird.components.ui.bolt.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
@Preview(showBackground = true)
internal fun CircularProgressIndicatorPreview() {
    PreviewWithThemes {
        CircularProgressIndicator(
            progress = { 0.75f },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun CircularProgressIndicatorColoredPreview() {
    PreviewWithThemes {
        CircularProgressIndicator(
            progress = { 0.75f },
            color = MainTheme.colors.secondary,
        )
    }
}
