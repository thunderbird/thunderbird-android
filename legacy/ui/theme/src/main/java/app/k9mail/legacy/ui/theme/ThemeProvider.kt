package app.k9mail.legacy.ui.theme

import androidx.annotation.StyleRes

interface ThemeProvider {
    @get:StyleRes
    val appThemeResourceId: Int

    @get:StyleRes
    val appLightThemeResourceId: Int

    @get:StyleRes
    val appDarkThemeResourceId: Int

    @get:StyleRes
    val dialogThemeResourceId: Int

    @get:StyleRes
    val translucentDialogThemeResourceId: Int
}
