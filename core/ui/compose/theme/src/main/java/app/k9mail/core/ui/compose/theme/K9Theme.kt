package app.k9mail.core.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val k9LightColorPalette = lightColors(
    primary = material_gray_800,
    primaryVariant = material_gray_700,
    secondary = material_pink_500,
    secondaryVariant = material_pink_300,
)

private val k9DarkColorPalette = darkColors(
    primary = material_gray_100,
    primaryVariant = material_gray_50,
    secondary = material_pink_300,
    secondaryVariant = material_pink_500,
)

@Composable
fun K9Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val images = Images(logo = R.drawable.logo_orange)

    MainTheme(
        lightColorPalette = k9LightColorPalette,
        darkColorPalette = k9DarkColorPalette,
        lightImages = images,
        darkImages = images,
        darkTheme = darkTheme,
        content = content,
    )
}
