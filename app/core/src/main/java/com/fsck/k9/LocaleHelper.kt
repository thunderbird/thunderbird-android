package com.fsck.k9

import android.content.res.Resources
import java.util.Locale

class LocaleHelper {
    companion object {
        private lateinit var currentLanguage: String

        @JvmStatic
        fun initializeLocale(resources: Resources) {
            currentLanguage = K9.k9Language
            setLanguage(resources)
        }

        private fun setLanguage(resources: Resources) {
            val locale = actualLocale()

            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        }

        @JvmStatic
        fun actualLocale(): Locale {
            val locale = if (K9.k9Language == null || K9.k9Language.isEmpty()) {
                Resources.getSystem().configuration.locale
            } else if (K9.k9Language.length == 5 && K9.k9Language[2] == '_') {
                // language is in the form: en_US
                Locale(K9.k9Language.substring(0, 2), K9.k9Language.substring(3))
            } else {
                Locale(K9.k9Language)
            }
            return locale
        }

        fun languageHasChanged(): Boolean {
            return currentLanguage != K9.k9Language
        }
    }
}
