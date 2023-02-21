package app.k9mail.core.ui.compose.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun PreviewWithThemes(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        PreviewHeader(themeName = "K9Theme")
        K9Theme {
            PreviewSurface(content = content)
        }
        K9Theme(darkTheme = true) {
            PreviewSurface(content = content)
        }

        PreviewHeader(themeName = "ThunderbirdTheme")
        ThunderbirdTheme {
            PreviewSurface(content = content)
        }
        ThunderbirdTheme(darkTheme = true) {
            PreviewSurface(content = content)
        }
    }
}

@Composable
private fun PreviewHeader(
    themeName: String,
) {
    Surface(
        color = Color.Cyan,
    ) {
        Text(
            text = themeName,
            fontSize = 4.sp,
            modifier = Modifier.padding(
                start = MainTheme.spacings.half,
                end = MainTheme.spacings.half,
            ),
        )
    }
}

@Composable
private fun PreviewSurface(
    content: @Composable () -> Unit,
) {
    Surface(
        color = MainTheme.colors.background,
        content = content,
    )
}
