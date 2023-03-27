package app.k9mail.core.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

private val k9LightColorPalette = lightColors(
    primary = MaterialColor.gray_800,
    primaryVariant = MaterialColor.gray_700,
    secondary = MaterialColor.pink_500,
    secondaryVariant = MaterialColor.pink_300,
)

private val k9DarkColorPalette = darkColors(
    primary = MaterialColor.gray_100,
    primaryVariant = MaterialColor.gray_50,
    secondary = MaterialColor.pink_300,
    secondaryVariant = MaterialColor.pink_500,
)

@Composable
fun K9Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val images = Images(logo = R.drawable.core_ui_theme_logo_orange)

    MainTheme(
        lightColorPalette = k9LightColorPalette,
        darkColorPalette = k9DarkColorPalette,
        lightImages = images,
        darkImages = images,
        darkTheme = darkTheme,
        content = content,
    )
}
