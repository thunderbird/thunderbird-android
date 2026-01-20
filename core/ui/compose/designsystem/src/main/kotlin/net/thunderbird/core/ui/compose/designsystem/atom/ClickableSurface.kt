package net.thunderbird.core.ui.compose.designsystem.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.theme2.MainTheme
import androidx.compose.material3.Surface as Material3Surface

/**
 * A UI atom for a Surface with a click listener.
 *
 * This is a convenience wrapper around [Material3Surface] that provides default values from [MainTheme].
 *
 * @param onClick The lambda to be executed when the surface is clicked.
 * @param modifier The modifier to be applied to the surface.
 * @param shape The shape of the surface.
 * @param color The color of the surface.
 * @param tonalElevation The tonal elevation of the surface.
 * @param content The content to be displayed inside the surface.
 */
@Composable
fun ClickableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MainTheme.colors.surface,
    tonalElevation: Dp = MainTheme.elevations.level0,
    content: @Composable () -> Unit,
) {
    Material3Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        content = content,
        tonalElevation = tonalElevation,
        color = color,
    )
}
