package app.k9mail.core.ui.compose.theme2

import androidx.compose.runtime.Composable

@Composable
internal fun selectThemeImages(
    themeConfig: ThemeConfig,
    darkTheme: Boolean,
) = when {
    darkTheme -> themeConfig.images.dark
    else -> themeConfig.images.light
}
