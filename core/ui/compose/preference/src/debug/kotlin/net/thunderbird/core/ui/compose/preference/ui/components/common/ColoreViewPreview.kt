package net.thunderbird.core.ui.compose.preference.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun ColorViewPreview() {
    PreviewWithThemes {
        ColorView(
            color = 0xFFFF0000.toInt(),
            onClick = null,
        )
    }
}
