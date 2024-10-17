package app.k9mail.feature.navigation.drawer.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.compose.theme2.toHarmonizedColor
import com.google.android.material.color.MaterialColors

@Composable
internal fun calculateAccountColor(accountColor: Int): Color {
    return if (accountColor == 0) {
        MainTheme.colors.primary
    } else {
        Color(accountColor).toHarmonizedColor(MainTheme.colors.surface)
    }
}

data class ColorRoles(
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
) {
    companion object {
        @Composable
        fun from(color: Color): ColorRoles {
            val context = LocalContext.current
            val colorRoles = MaterialColors.getColorRoles(context, color.toArgb())
            return ColorRoles(
                accent = Color(colorRoles.accent),
                onAccent = Color(colorRoles.onAccent),
                accentContainer = Color(colorRoles.accentContainer),
                onAccentContainer = Color(colorRoles.onAccentContainer),
            )
        }
    }
}
