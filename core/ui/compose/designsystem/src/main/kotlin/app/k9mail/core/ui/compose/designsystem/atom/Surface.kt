package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.theme2.MainTheme
import androidx.compose.material3.Surface as Material3Surface

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MainTheme.colors.surface,
    tonalElevation: Dp = MainTheme.elevations.level0,
    content: @Composable () -> Unit,
) {
    Material3Surface(
        modifier = modifier,
        shape = shape,
        content = content,
        tonalElevation = tonalElevation,
        color = color,
    )
}
