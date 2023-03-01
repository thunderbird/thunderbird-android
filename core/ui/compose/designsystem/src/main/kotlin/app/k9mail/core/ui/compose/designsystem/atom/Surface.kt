package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.Surface as MaterialSurface

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    MaterialSurface(
        modifier = modifier,
        content = content,
        color = MainTheme.colors.surface,
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
