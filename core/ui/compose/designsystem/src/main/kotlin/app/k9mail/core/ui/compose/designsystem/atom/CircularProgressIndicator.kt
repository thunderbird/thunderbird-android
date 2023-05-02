package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.CircularProgressIndicator as MaterialCircularProgressIndicator

@Composable
fun CircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MainTheme.colors.secondary,
) {
    MaterialCircularProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
    )
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MainTheme.colors.secondary,
) {
    MaterialCircularProgressIndicator(
        modifier = modifier,
        color = color,
    )
}

@Preview
@Composable
internal fun CircularProgressIndicatorPreview() {
    PreviewWithThemes {
        CircularProgressIndicator(progress = 0.75f)
    }
}
