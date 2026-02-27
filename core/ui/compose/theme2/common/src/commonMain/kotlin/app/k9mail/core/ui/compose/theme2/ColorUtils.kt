package app.k9mail.core.ui.compose.theme2

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * Returns a surface container color that is a composite of the given color and the theme surface container color.
 *
 * The alpha value is applied to the given color before compositing.
 *
 * @param alpha The alpha value to apply to the color.
 * @return A new color that is a composite of the given color and the theme surface container color.
 */
@Composable
fun Color.toSurfaceContainer(alpha: Float): Color {
    val color = copy(alpha = alpha)
    return color.compositeOver(MainTheme.colors.surfaceContainer)
}
