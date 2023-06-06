package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.Surface as MaterialSurface

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MainTheme.colors.surface,
    elevation: Dp = MainTheme.elevations.default,
    content: @Composable () -> Unit,
) {
    MaterialSurface(
        modifier = modifier,
        shape = shape,
        content = content,
        elevation = elevation,
        color = color,
    )
}

@Preview(showBackground = true)
@Composable
internal fun SurfacePreview() {
    PreviewWithThemes {
        Surface(
            modifier = Modifier
                .requiredHeight(100.dp)
                .requiredWidth(100.dp),
            content = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun SurfaceWithShapePreview() {
    PreviewWithThemes {
        Background {
            Surface(
                modifier = Modifier
                    .requiredHeight(MainTheme.sizes.larger)
                    .requiredWidth(MainTheme.sizes.larger),
                shape = MainTheme.shapes.small,
                color = MainTheme.colors.primary,
                content = {},
            )
        }
    }
}
