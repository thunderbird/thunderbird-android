package net.thunderbird.components.ui.bolt.theme

import androidx.compose.runtime.Composable

/** Uses platform-provided colors when available, otherwise returns [defaultColorScheme]. */
@Composable
internal expect fun platformColorScheme(
    defaultColorScheme: ThemeColorScheme,
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ThemeColorScheme
