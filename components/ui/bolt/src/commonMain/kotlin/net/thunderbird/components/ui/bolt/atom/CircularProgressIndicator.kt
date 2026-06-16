package net.thunderbird.components.ui.bolt.atom

import androidx.compose.material3.CircularProgressIndicator as Material3CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
fun CircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
) {
    Material3CircularProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
    )
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
) {
    Material3CircularProgressIndicator(
        modifier = modifier,
        color = color,
    )
}

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
