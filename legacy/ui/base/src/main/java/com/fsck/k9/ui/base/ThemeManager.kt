package com.fsck.k9.ui.base

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import app.k9mail.legacy.preferences.AppTheme
import app.k9mail.legacy.preferences.GeneralSettings
import app.k9mail.legacy.preferences.GeneralSettingsManager
import app.k9mail.legacy.preferences.SubTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus

class ThemeManager(
    private val context: Context,
    private val themeProvider: ThemeProvider,
    private val generalSettingsManager: GeneralSettingsManager,
    private val appCoroutineScope: CoroutineScope,
) {

    private val generalSettings: GeneralSettings
        get() = generalSettingsManager.getSettings()

    val appTheme: Theme
        get() = when (generalSettings.appTheme) {
            AppTheme.LIGHT -> Theme.LIGHT
            AppTheme.DARK -> Theme.DARK
            AppTheme.FOLLOW_SYSTEM -> if (Build.VERSION.SDK_INT < 28) Theme.LIGHT else getSystemTheme()
        }

    val messageViewTheme: Theme
        get() = resolveTheme(generalSettings.messageViewTheme)

    val messageComposeTheme: Theme
        get() = resolveTheme(generalSettings.messageComposeTheme)

    @get:StyleRes
    val appThemeResourceId: Int = themeProvider.appThemeResourceId

    @get:StyleRes
    val messageViewThemeResourceId: Int
        get() = getSubThemeResourceId(generalSettings.messageViewTheme)

    @get:StyleRes
    val messageComposeThemeResourceId: Int
        get() = getSubThemeResourceId(generalSettings.messageComposeTheme)

    @get:StyleRes
    val dialogThemeResourceId: Int = themeProvider.dialogThemeResourceId

    @get:StyleRes
    val translucentDialogThemeResourceId: Int = themeProvider.translucentDialogThemeResourceId

    fun init() {
        generalSettingsManager.getSettingsFlow()
            .map { it.appTheme }
            .distinctUntilChanged()
            .onEach {
                updateAppTheme(it)
            }
            .launchIn(appCoroutineScope + Dispatchers.Main.immediate)
    }

    private fun updateAppTheme(appTheme: AppTheme) {
        val defaultNightMode = when (appTheme) {
            AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppTheme.FOLLOW_SYSTEM -> {
                if (Build.VERSION.SDK_INT < 28) {
                    AppCompatDelegate.MODE_NIGHT_NO
                } else {
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            }
        }
        AppCompatDelegate.setDefaultNightMode(defaultNightMode)
    }

    fun toggleMessageViewTheme() {
        if (messageViewTheme === Theme.DARK) {
            generalSettingsManager.setMessageViewTheme(SubTheme.LIGHT)
        } else {
            generalSettingsManager.setMessageViewTheme(SubTheme.DARK)
        }
    }

    private fun getSubThemeResourceId(subTheme: SubTheme): Int = when (subTheme) {
        SubTheme.LIGHT -> themeProvider.appLightThemeResourceId
        SubTheme.DARK -> themeProvider.appDarkThemeResourceId
        SubTheme.USE_GLOBAL -> themeProvider.appThemeResourceId
    }

    private fun resolveTheme(theme: SubTheme): Theme = when (theme) {
        SubTheme.LIGHT -> Theme.LIGHT
        SubTheme.DARK -> Theme.DARK
        SubTheme.USE_GLOBAL -> appTheme
    }

    private fun getSystemTheme(): Theme {
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> Theme.LIGHT
            Configuration.UI_MODE_NIGHT_YES -> Theme.DARK
            else -> Theme.LIGHT
        }
    }
}

enum class Theme {
    LIGHT,
    DARK,
}
