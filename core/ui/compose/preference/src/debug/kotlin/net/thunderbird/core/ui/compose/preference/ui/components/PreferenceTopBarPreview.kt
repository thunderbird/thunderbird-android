package net.thunderbird.core.ui.compose.preference.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun PreferenceTopBarPreview() {
    PreviewWithThemes {
        PreferenceTopBar(
            title = "Title",
            subtitle = null,
            onBack = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun PreferenceTopBarWithSubtitlePreview() {
    PreviewWithThemes {
        PreferenceTopBar(
            title = "Title",
            subtitle = "Subtitle",
            onBack = {},
        )
    }
}
