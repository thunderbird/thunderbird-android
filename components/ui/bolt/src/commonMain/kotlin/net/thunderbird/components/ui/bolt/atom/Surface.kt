package net.thunderbird.components.ui.bolt.atom

import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Surface as Material3Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.MainTheme
import net.thunderbird.components.ui.bolt.theme.ThemeColorScheme
import net.thunderbird.components.ui.bolt.theme.ThemeElevations

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

@Composable
@Preview(showBackground = true)
internal fun SurfacePreview() {
    PreviewWithThemes {
        Surface(
            modifier = Modifier
                .requiredHeight(MainTheme.sizes.larger)
                .requiredWidth(MainTheme.sizes.larger),
            content = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SurfaceWithShapePreview() {
    PreviewWithThemes {
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
