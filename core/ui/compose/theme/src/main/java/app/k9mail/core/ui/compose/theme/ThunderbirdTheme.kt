package app.k9mail.core.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

private val thunderbirdLightColorPalette = lightColors(
    primary = MaterialColor.blue_600,
    primaryVariant = MaterialColor.light_blue_500,
    secondary = MaterialColor.pink_500,
    secondaryVariant = MaterialColor.pink_300,
    background = MaterialColor.gray_200,
)

private val thunderbirdDarkColorPalette = darkColors(
    primary = MaterialColor.blue_100,
    primaryVariant = MaterialColor.blue_50,
    secondary = MaterialColor.pink_300,
    secondaryVariant = MaterialColor.pink_500,
    background = MaterialColor.gray_800,
)

@Composable
fun ThunderbirdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val images = Images(logo = R.drawable.core_ui_theme_logo_teal)

    MainTheme(
        lightColorPalette = thunderbirdLightColorPalette,
        darkColorPalette = thunderbirdDarkColorPalette,
        lightImages = images,
        darkImages = images,
        darkTheme = darkTheme,
        content = content,
    )
}
