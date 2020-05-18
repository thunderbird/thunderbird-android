package com.fsck.k9.ui.base

import android.app.Activity
import android.content.res.Resources
import android.text.TextUtils
import com.fsck.k9.K9
import java.util.Locale
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * This class implements functionality common to most activities used in K-9 Mail.
 *
 * @see K9Activity
 */
class K9ActivityCommon(
    private val activity: Activity,
    private val themeType: ThemeType
) {
    private lateinit var currentLanguage: String
    private lateinit var currentTheme: Theme

    val themeManager = Companion.themeManager

    /**
     * Call this before calling `super.onCreate(Bundle)`.
     */
    fun preOnCreate() {
        K9.k9Language.let { language ->
            currentLanguage = language
            setLanguage(language)
        }

        currentTheme = themeManager.appTheme
        val theme = when (themeType) {
            ThemeType.DEFAULT -> themeManager.appThemeResourceId
            ThemeType.ACTION_BAR -> themeManager.appActionBarThemeResourceId
            ThemeType.DIALOG -> themeManager.translucentDialogThemeResourceId
        }
        activity.setTheme(theme)
    }

    fun preOnResume() {
        if (currentLanguage != K9.k9Language) {
            activity.recreate()
        }
    }

    private fun setLanguage(language: String) {
        val locale = if (TextUtils.isEmpty(language)) {
            Resources.getSystem().configuration.locale
        } else if (language.length == 5 && language[2] == '_') {
            // language is in the form: en_US
            Locale(language.substring(0, 2), language.substring(3))
        } else {
            Locale(language)
        }

        val resources = activity.resources
        val config = resources.configuration
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    companion object : KoinComponent {
        private val themeManager: ThemeManager by inject()
    }
}

enum class ThemeType {
    DEFAULT,
    ACTION_BAR,
    DIALOG
}
