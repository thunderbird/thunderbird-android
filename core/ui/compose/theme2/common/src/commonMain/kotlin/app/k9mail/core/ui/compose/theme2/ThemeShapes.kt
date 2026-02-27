package app.k9mail.core.ui.compose.theme2

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * The shapes used in the app.
 *
 * The shapes are defined as:
 *
 * - None
 * - ExtraSmall
 * - Small
 * - Medium
 * - Large
 * - ExtraLarge
 * - Full
 *
 * The default values are based on the Material Design guidelines.
 *
 * Shapes None and Full are omitted as None is a RectangleShape and Full is a CircleShape.
 *
 * @see: https://m3.material.io/styles/shape/overview
 */
@Immutable
data class ThemeShapes(
    val extraSmall: CornerBasedShape = RoundedCornerShape(4.dp),
    val small: CornerBasedShape = RoundedCornerShape(8.dp),
    val medium: CornerBasedShape = RoundedCornerShape(12.dp),
    val large: CornerBasedShape = RoundedCornerShape(16.dp),
    val extraLarge: CornerBasedShape = RoundedCornerShape(28.dp),
)

/**
 * Converts the [ThemeShapes] to Material 3 [Shapes].
 */
internal fun ThemeShapes.toMaterial3Shapes() = Shapes(
    extraSmall = extraSmall,
    small = small,
    medium = medium,
    large = large,
    extraLarge = extraLarge,
)

internal val LocalThemeShapes = staticCompositionLocalOf<ThemeShapes> {
    error("No ThemeShapes provided")
}
