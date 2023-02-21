package app.k9mail.core.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val thunderbirdLightColorPalette = lightColors(
    primary = material_blue_600,
    primaryVariant = material_light_blue_500,
    secondary = material_pink_500,
    secondaryVariant = material_pink_300,
)

private val thunderbirdDarkColorPalette = darkColors(
    primary = material_blue_100,
    primaryVariant = material_blue_50,
    secondary = material_pink_300,
    secondaryVariant = material_pink_500,
)

@Composable
fun ThunderbirdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val images = Images(logo = R.drawable.logo_teal)

    MainTheme(
        lightColorPalette = thunderbirdLightColorPalette,
        darkColorPalette = thunderbirdDarkColorPalette,
        lightImages = images,
        darkImages = images,
        darkTheme = darkTheme,
        content = content,
    )
}
