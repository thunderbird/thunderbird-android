package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Preview
@Preview(showBackground = true)
@Composable
private fun LinearProgressIndicatorPreview() {
    PreviewWithThemes {
        LinearProgressIndicator(
            progress = 10000,
            visible = true,
        )
    }
}
