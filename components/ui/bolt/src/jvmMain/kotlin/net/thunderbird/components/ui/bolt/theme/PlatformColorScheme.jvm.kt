package net.thunderbird.components.ui.bolt.theme

import androidx.compose.runtime.Composable

@Composable
internal actual fun platformColorScheme(
    defaultColorScheme: ThemeColorScheme,
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ThemeColorScheme = defaultColorScheme
