package com.fsck.k9.ui

import androidx.annotation.StyleRes
import com.fsck.k9.K9

class ThemeManager {
    val appTheme: Theme
        get() = when (K9.k9Theme) {
            K9.Theme.LIGHT -> Theme.LIGHT
            K9.Theme.DARK -> Theme.DARK
            K9.Theme.USE_GLOBAL -> error("App theme must be either LIGHT or DARK")
        }

    val messageViewTheme: Theme
        get() = resolveTheme(K9.k9MessageViewThemeSetting)

    val messageComposeTheme: Theme
        get() = resolveTheme(K9.k9ComposerThemeSetting)

    @get:StyleRes
    val appThemeResourceId: Int
        get() = getThemeResourceId(appTheme)

    @get:StyleRes
    val appActionBarThemeResourceId: Int
        get() = when (appTheme) {
            Theme.LIGHT -> R.style.Theme_K9_Light_ActionBar
            Theme.DARK -> R.style.Theme_K9_Dark_ActionBar
        }

    @get:StyleRes
    val messageViewThemeResourceId: Int
        get() = getThemeResourceId(messageViewTheme)

    @get:StyleRes
    val messageComposeThemeResourceId: Int
        get() = getThemeResourceId(messageComposeTheme)

    @get:StyleRes
    val dialogThemeResourceId: Int
        get() = when (appTheme) {
            Theme.LIGHT -> R.style.Theme_K9_Dialog_Light
            Theme.DARK -> R.style.Theme_K9_Dialog_Dark
        }

    @get:StyleRes
    val translucentDialogThemeResourceId: Int
        get() = when (appTheme) {
            Theme.LIGHT -> R.style.Theme_K9_Dialog_Translucent_Light
            Theme.DARK -> R.style.Theme_K9_Dialog_Translucent_Dark
        }

    fun toggleMessageViewTheme() {
        if (messageViewTheme === Theme.DARK) {
            K9.k9MessageViewThemeSetting = K9.Theme.LIGHT
        } else {
            K9.k9MessageViewThemeSetting = K9.Theme.DARK
        }

        K9.saveSettingsAsync()
    }

    private fun getThemeResourceId(theme: Theme): Int = when (theme) {
        Theme.LIGHT -> R.style.Theme_K9_Light
        Theme.DARK -> R.style.Theme_K9_Dark
    }

    private fun resolveTheme(theme: K9.Theme): Theme = when (theme) {
        K9.Theme.LIGHT -> Theme.LIGHT
        K9.Theme.DARK -> Theme.DARK
        K9.Theme.USE_GLOBAL -> appTheme
    }
}

enum class Theme {
    LIGHT,
    DARK
}
