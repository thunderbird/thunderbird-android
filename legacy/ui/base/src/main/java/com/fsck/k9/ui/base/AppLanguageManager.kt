package com.fsck.k9.ui.base

import android.content.res.Resources
import com.fsck.k9.ui.base.extensions.currentLocale
import com.fsck.k9.ui.base.locale.SystemLocaleChangeListener
import com.fsck.k9.ui.base.locale.SystemLocaleManager
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettingsPreferenceManager
import net.thunderbird.core.preference.update

/**
 * Manages app language changes.
 *
 * - Sets the default locale when the app language is changed.
 * - Notifies listeners when the app language has changed.
 */
class AppLanguageManager(
    private val systemLocaleManager: SystemLocaleManager,
    private val coroutineScope: CoroutineScope = MainScope(),
    private val displayCoreSettingsPreferenceManager: DisplayCoreSettingsPreferenceManager,
) {
    private var currentOverrideLocale: Locale? = null
    private val _overrideLocale = MutableSharedFlow<Locale?>(replay = 1)
    private val _appLocale = MutableSharedFlow<Locale>(replay = 1)
    val overrideLocale: Flow<Locale?> = _overrideLocale
    val appLocale: Flow<Locale> = _appLocale

    private val systemLocaleListener = SystemLocaleChangeListener {
        coroutineScope.launch {
            _appLocale.emit(systemLocale)
        }
    }

    fun init() {
        setLocale(getAppLanguage())
    }

    fun getOverrideLocale(): Locale? = currentOverrideLocale

    fun getAppLanguage(): String {
        return displayCoreSettingsPreferenceManager.getConfig().appLanguage
    }

    fun setAppLanguage(appLanguage: String) {
        if (appLanguage == getAppLanguage()) {
            return
        }
        displayCoreSettingsPreferenceManager.update { displayCoreSettings ->
            displayCoreSettings.copy(appLanguage = appLanguage)
        }

        setLocale(appLanguage)
    }

    fun applyOverrideLocale() {
        currentOverrideLocale?.let { overrideLocale ->
            Locale.setDefault(overrideLocale)
        }
    }

    private fun setLocale(appLanguage: String) {
        val overrideLocale = getOverrideLocaleForLanguage(appLanguage)
        currentOverrideLocale = overrideLocale

        val locale = overrideLocale ?: systemLocale
        Locale.setDefault(locale)

        if (overrideLocale == null) {
            systemLocaleManager.addListener(systemLocaleListener)
        } else {
            systemLocaleManager.removeListener(systemLocaleListener)
        }

        coroutineScope.launch {
            _overrideLocale.emit(overrideLocale)
            _appLocale.emit(locale)
        }
    }

    private fun getOverrideLocaleForLanguage(appLanguage: String): Locale? {
        return if (appLanguage.isEmpty()) {
            null
        } else if (appLanguage.length == 5 && appLanguage[2] == '_') {
            // language is in the form: en_US
            val language = appLanguage.substring(0, 2)
            val country = appLanguage.substring(3)
            Locale(language, country)
        } else {
            Locale(appLanguage)
        }
    }

    private val systemLocale get() = Resources.getSystem().configuration.currentLocale
}
