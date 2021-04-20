package com.fsck.k9.ui.base

import android.content.res.Resources
import com.fsck.k9.K9
import com.fsck.k9.ui.base.extensions.currentLocale
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/**
 * Manages app language changes.
 *
 * - Sets the default locale when the app language is changed.
 * - Notifies listeners when the app language has changed.
 */
class AppLanguageManager(
    private val coroutineScope: CoroutineScope = GlobalScope + Dispatchers.Main
) {
    private var currentOverrideLocale: Locale? = null
    private val _overrideLocale = MutableSharedFlow<Locale?>(replay = 1)
    val overrideLocale: Flow<Locale?> = _overrideLocale

    fun init() {
        setLocale(K9.k9Language)
    }

    fun getOverrideLocale(): Locale? = currentOverrideLocale

    fun getAppLanguage(): String {
        return K9.k9Language
    }

    fun setAppLanguage(appLanguage: String) {
        if (appLanguage == K9.k9Language) {
            return
        }

        K9.k9Language = appLanguage

        setLocale(appLanguage)
    }

    private fun setLocale(appLanguage: String) {
        val overrideLocale = getOverrideLocaleForLanguage(appLanguage)
        currentOverrideLocale = overrideLocale

        val locale = overrideLocale ?: systemLocale
        Locale.setDefault(locale)

        coroutineScope.launch {
            _overrideLocale.emit(overrideLocale)
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
