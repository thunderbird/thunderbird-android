package net.thunderbird.components.ui.bolt.theme

import androidx.compose.runtime.Composable

@Composable
internal fun selectThemeImages(
    themeConfig: ThemeConfig,
    darkTheme: Boolean,
) = when {
    darkTheme -> themeConfig.images.dark
    else -> themeConfig.images.light
}
