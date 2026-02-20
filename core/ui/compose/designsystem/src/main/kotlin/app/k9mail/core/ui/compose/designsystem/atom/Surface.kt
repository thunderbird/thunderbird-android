package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.compose.theme2.ThemeColorScheme
import app.k9mail.core.ui.compose.theme2.ThemeElevations
import androidx.compose.material3.Surface as Material3Surface

/**
 * UI atom for a surface with customizable shape, color, and elevation. This is a convenience wrapper
 * around [Material3Surface], providing a more specific API for our design system.
 *
 * Example usage:
 * ```
 * Surface(
 *     modifier = Modifier.fillMaxWidth(),
 *     shape = RoundedCornerShape(8.dp),
 *     tonalElevation = MainTheme.elevations.level1,
 * ) {
 *     Text("Content inside Surface")
 * }
 * ```
 *
 * @param modifier [Modifier] to be applied to the Surface.
 * @param shape [Shape] of the Surface. Defaults to [RectangleShape].
 * @param color [Color] of the Surface background. Defaults to
 *  [MainTheme.colors.surface][ThemeColorScheme.surface].
 * @param contentColor Preferred [Color] for content inside this Surface. Defaults to a color
 * that provides appropriate contrast with [color] via [contentColorFor].
 * @param tonalElevation Tonal elevation of the Surface. Defaults to
 *  [MainTheme.elevations.level0][ThemeElevations.level0].This affects the background color
 *  based on the elevation overlay.
 * @param content Composable content to be displayed on the Surface.
 */
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MainTheme.colors.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = MainTheme.elevations.level0,
    content: @Composable () -> Unit,
) {
    Material3Surface(
        modifier = modifier,
        shape = shape,
        content = content,
        tonalElevation = tonalElevation,
        color = color,
        contentColor = contentColor,
    )
}
