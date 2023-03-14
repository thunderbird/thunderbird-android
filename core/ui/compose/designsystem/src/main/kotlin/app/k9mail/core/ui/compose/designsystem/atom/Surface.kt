package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.Surface as MaterialSurface

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    color: Color = MainTheme.colors.surface,
    content: @Composable () -> Unit,
) {
    MaterialSurface(
        modifier = modifier,
        content = content,
        color = color,
    )
}

@Preview(showBackground = true)
@Composable
internal fun SurfacePreview() {
    PreviewWithThemes {
        Surface(
            modifier = Modifier.fillMaxSize(),
            content = {},
        )
    }
}
