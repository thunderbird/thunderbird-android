package net.thunderbird.components.ui.bolt.theme

import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun platformColorScheme(
    defaultColorScheme: ThemeColorScheme,
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ThemeColorScheme {
    if (!dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return defaultColorScheme

    val context = LocalContext.current
    val dynamicColorScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    return dynamicColorScheme.toBoltColorScheme(defaultColorScheme)
}
