package net.thunderbird.core.ui.theme.api

import androidx.annotation.StyleRes

interface ThemeManager {

    val appTheme: Theme
    val messageViewTheme: Theme
    val messageComposeTheme: Theme

    @get:StyleRes
    val appThemeResourceId: Int

    @get:StyleRes
    val messageViewThemeResourceId: Int

    @get:StyleRes
    val messageComposeThemeResourceId: Int

    @get:StyleRes
    val dialogThemeResourceId: Int

    @get:StyleRes
    val translucentDialogThemeResourceId: Int
}
