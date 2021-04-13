package com.fsck.k9.ui.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fsck.k9.LocaleHelper.Companion.initializeLocale
import com.fsck.k9.LocaleHelper.Companion.languageHasChanged
import org.koin.android.ext.android.inject

abstract class K9Activity(private val themeType: ThemeType) : AppCompatActivity() {
    constructor() : this(ThemeType.DEFAULT)

    protected val themeManager: ThemeManager by inject()

    private lateinit var currentTheme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        initializeLocale(resources)
        initializeTheme()
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        languageChangeCheck()
        super.onResume()
    }

    private fun initializeTheme() {
        currentTheme = themeManager.appTheme
        val theme = when (themeType) {
            ThemeType.DEFAULT -> themeManager.appThemeResourceId
            ThemeType.DIALOG -> themeManager.translucentDialogThemeResourceId
        }
        setTheme(theme)
    }

    private fun languageChangeCheck() {
        if (languageHasChanged()) {
            recreate()
        }
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
    DIALOG
}
