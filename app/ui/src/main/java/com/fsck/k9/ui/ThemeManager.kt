package com.fsck.k9.ui

import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import com.fsck.k9.K9
import com.fsck.k9.K9.AppTheme
import com.fsck.k9.K9.SubTheme

class ThemeManager {
    val appTheme: Theme
        get() = when (K9.appTheme) {
            AppTheme.LIGHT -> Theme.LIGHT
            AppTheme.DARK -> Theme.DARK
        }

    val messageViewTheme: Theme
        get() = resolveTheme(K9.messageViewTheme)

    val messageComposeTheme: Theme
        get() = resolveTheme(K9.messageComposeTheme)

    @get:StyleRes
    val appThemeResourceId: Int = R.style.Theme_K9_DayNight

    @get:StyleRes
    val appActionBarThemeResourceId: Int = R.style.Theme_K9_DayNight_ActionBar

    @get:StyleRes
    val messageViewThemeResourceId: Int
        get() = getSubThemeResourceId(K9.messageViewTheme)

    @get:StyleRes
    val messageComposeThemeResourceId: Int
        get() = getSubThemeResourceId(K9.messageComposeTheme)

    @get:StyleRes
    val dialogThemeResourceId: Int = R.style.Theme_K9_Dialog_DayNight

    @get:StyleRes
    val translucentDialogThemeResourceId: Int = R.style.Theme_K9_Dialog_Translucent_DayNight


    fun init() {
        updateAppTheme()
    }

    fun updateAppTheme() {
        val defaultNightMode = when (appTheme) {
            Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            Theme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(defaultNightMode)
    }

    fun toggleMessageViewTheme() {
        if (messageViewTheme === Theme.DARK) {
            K9.messageViewTheme = SubTheme.LIGHT
        } else {
            K9.messageViewTheme = SubTheme.DARK
        }

        K9.saveSettingsAsync()
    }

    private fun getSubThemeResourceId(subTheme: SubTheme): Int = when (subTheme) {
        SubTheme.LIGHT -> R.style.Theme_K9_Light
        SubTheme.DARK -> R.style.Theme_K9_Dark
        SubTheme.USE_GLOBAL -> R.style.Theme_K9_DayNight
    }

    private fun resolveTheme(theme: SubTheme): Theme = when (theme) {
        SubTheme.LIGHT -> Theme.LIGHT
        SubTheme.DARK -> Theme.DARK
        SubTheme.USE_GLOBAL -> appTheme
    }
}

enum class Theme {
    LIGHT,
    DARK
}
