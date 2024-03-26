package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.theme2.MainTheme

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
