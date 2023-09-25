package app.k9mail.core.ui.compose.theme.material2

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.theme.material2.color.MaterialColor
import app.k9mail.core.ui.compose.theme.material2.color.darkColors
import app.k9mail.core.ui.compose.theme.material2.color.lightColors

private val thunderbirdLightColorPalette = lightColors(
    primary = MaterialColor.blue_800,
    primaryVariant = MaterialColor.light_blue_700,
    secondary = MaterialColor.pink_500,
    secondaryVariant = MaterialColor.pink_300,
)

private val thunderbirdDarkColorPalette = darkColors(
    primary = MaterialColor.blue_200,
    primaryVariant = MaterialColor.blue_400,
    secondary = MaterialColor.pink_300,
    secondaryVariant = MaterialColor.pink_500,
)

@Composable
fun ThunderbirdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val images = Images(logo = R.drawable.core_ui_theme_material2_thunderbird_logo)

    MainTheme(
        lightColorPalette = thunderbirdLightColorPalette,
        darkColorPalette = thunderbirdDarkColorPalette,
        lightImages = images,
        darkImages = images,
        darkTheme = darkTheme,
        content = content,
    )
}
