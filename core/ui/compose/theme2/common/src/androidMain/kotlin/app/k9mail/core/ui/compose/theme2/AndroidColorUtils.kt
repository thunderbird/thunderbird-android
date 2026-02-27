package app.k9mail.core.ui.compose.theme2

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.MaterialColors

/**
 * Returns a harmonized color that is derived from the given color and the target color.
 *
 * This function uses Material Colors to harmonize the two colors.
 *
 * @param target The target color to harmonize with.
 * @return A new color that is harmonized with the target color.
 */
fun Color.toHarmonizedColor(target: Color) = Color(MaterialColors.harmonize(toArgb(), target.toArgb()))

/**
 * Returns a [ColorRoles] object that contains the accent colors derived from the given color.
 *
 * This function uses Material Colors to retrieve the accent colors based on the provided color.
 *
 * @param context The context to use for retrieving the color roles.
 * @return A [ColorRoles] object containing the accent colors.
 */
fun Color.toColorRoles(context: Context): ColorRoles {
    val colorRoles = MaterialColors.getColorRoles(context, this.toArgb())
    return ColorRoles(
        accent = Color(colorRoles.accent),
        onAccent = Color(colorRoles.onAccent),
        accentContainer = Color(colorRoles.accentContainer),
        onAccentContainer = Color(colorRoles.onAccentContainer),
    )
}
