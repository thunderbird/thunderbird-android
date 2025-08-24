package net.thunderbird.core.ui.theme.manager

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.SubTheme
import net.thunderbird.core.preference.update
import net.thunderbird.core.ui.theme.api.Theme
import net.thunderbird.core.ui.theme.api.ThemeManager
import net.thunderbird.core.ui.theme.api.ThemeProvider

class ThemeManager(
    private val context: Context,
    private val themeProvider: ThemeProvider,
    private val generalSettingsManager: GeneralSettingsManager,
    private val appCoroutineScope: CoroutineScope,
) : ThemeManager {

    private val generalSettings: GeneralSettings
        get() = generalSettingsManager.getConfig()

    override val appTheme: Theme
        get() = when (generalSettings.display.coreSetting.appTheme) {
            AppTheme.LIGHT -> Theme.LIGHT
            AppTheme.DARK -> Theme.DARK
            AppTheme.FOLLOW_SYSTEM -> getSystemTheme()
        }

    override val messageViewTheme: Theme
        get() = resolveTheme(generalSettings.display.coreSetting.messageViewTheme)

    override val messageComposeTheme: Theme
        get() = resolveTheme(generalSettings.display.coreSetting.messageComposeTheme)

    @get:StyleRes
    override val appThemeResourceId: Int = themeProvider.appThemeResourceId

    @get:StyleRes
    override val messageViewThemeResourceId: Int
        get() = getSubThemeResourceId(generalSettings.display.coreSetting.messageViewTheme)

    @get:StyleRes
    override val messageComposeThemeResourceId: Int
        get() = getSubThemeResourceId(generalSettings.display.coreSetting.messageComposeTheme)

    @get:StyleRes
    override val dialogThemeResourceId: Int = themeProvider.dialogThemeResourceId

    @get:StyleRes
    override val translucentDialogThemeResourceId: Int = themeProvider.translucentDialogThemeResourceId

    fun init() {
        generalSettingsManager.getSettingsFlow()
            .map { it.display.coreSetting.appTheme }
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
            AppTheme.FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(defaultNightMode)
    }

    fun toggleMessageViewTheme() {
        if (messageViewTheme === Theme.DARK) {
            generalSettingsManager.update { settings ->
                settings.copy(
                    display = settings.display.copy(
                        coreSetting = settings.display.coreSetting.copy(
                            messageViewTheme = SubTheme.LIGHT
                        )
                    )
                )
            }
        } else {
            generalSettingsManager.update { settings ->
                settings.copy(
                    display = settings.display.copy(
                        coreSetting = settings.display.coreSetting.copy(
                            messageViewTheme = SubTheme.DARK
                        )
                    )
                )
            }
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
