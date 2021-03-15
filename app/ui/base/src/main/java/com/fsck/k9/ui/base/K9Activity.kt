package com.fsck.k9.ui.base

import android.content.res.Resources
import android.os.Bundle
import android.text.TextUtils
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fsck.k9.K9
import java.util.Locale
import org.koin.android.ext.android.inject

abstract class K9Activity(private val themeType: ThemeType) : AppCompatActivity() {
    constructor() : this(ThemeType.DEFAULT)

    protected val themeManager: ThemeManager by inject()

    private lateinit var currentLanguage: String
    private lateinit var currentTheme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        initializeLanguage()
        initializeTheme()
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        languageChangeCheck()
        super.onResume()
    }

    private fun initializeLanguage() {
        K9.k9Language.let { language ->
            currentLanguage = language
            setLanguage(language)
        }
    }

    private fun initializeTheme() {
        currentTheme = themeManager.appTheme
        val theme = when (themeType) {
            ThemeType.DEFAULT -> themeManager.appThemeResourceId
            ThemeType.ACTION_BAR -> themeManager.appActionBarThemeResourceId
            ThemeType.DIALOG -> themeManager.translucentDialogThemeResourceId
        }
        setTheme(theme)
    }

    private fun languageChangeCheck() {
        if (currentLanguage != K9.k9Language) {
            recreate()
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

        val config = resources.configuration
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    protected fun setLayout(@LayoutRes layoutResId: Int) {
        setContentView(layoutResId)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
            ?: error("K9 layouts must provide a toolbar with id='toolbar'.")

        setSupportActionBar(toolbar)
    }
}

enum class ThemeType {
    DEFAULT,
    ACTION_BAR,
    DIALOG
}
