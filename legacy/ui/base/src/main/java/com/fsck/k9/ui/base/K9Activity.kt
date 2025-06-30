package com.fsck.k9.ui.base

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.lifecycle.asLiveData
import com.fsck.k9.controller.push.PushController
import java.util.Locale
import net.thunderbird.core.ui.theme.manager.ThemeManager
import org.koin.android.ext.android.inject

abstract class K9Activity(private val themeType: ThemeType) : AppCompatActivity() {
    constructor() : this(ThemeType.DEFAULT)

    private val pushController: PushController by inject()
    protected val themeManager: ThemeManager by inject()
    private val appLanguageManager: AppLanguageManager by inject()

    private var overrideLocaleOnLaunch: Locale? = null

    override fun attachBaseContext(baseContext: Context) {
        overrideLocaleOnLaunch = appLanguageManager.getOverrideLocale()

        val newBaseContext = overrideLocaleOnLaunch?.let { locale ->
            LocaleContextWrapper(baseContext, locale)
        } ?: baseContext

        super.attachBaseContext(newBaseContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initializeTheme()
        initializePushController()
        super.onCreate(savedInstanceState)

        setLayoutDirection()
        listenForAppLanguageChanges()
    }

    // On Android 12+ the layout direction doesn't seem to be updated when recreating the activity. This is a problem
    // when switching from an LTR to an RTL language (or the other way around) using the language picker in the app.
    private fun setLayoutDirection() {
        if (Build.VERSION.SDK_INT >= 31) {
            window.decorView.layoutDirection = resources.configuration.layoutDirection
        }
    }

    private fun listenForAppLanguageChanges() {
        appLanguageManager.overrideLocale.asLiveData().observe(this) { overrideLocale ->
            if (overrideLocale != overrideLocaleOnLaunch) {
                recreateCompat()
            }
        }
    }

    private fun initializeTheme() {
        val theme = when (themeType) {
            ThemeType.DEFAULT -> themeManager.appThemeResourceId
            ThemeType.DIALOG -> themeManager.translucentDialogThemeResourceId
        }
        setTheme(theme)
    }

    private fun initializePushController() {
        pushController.init()
    }

    protected fun setLayout(@LayoutRes layoutResId: Int) {
        setContentView(layoutResId)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
            ?: error("K9 layouts must provide a toolbar with id='toolbar'.")

        setSupportActionBar(toolbar)
    }

    protected fun recreateCompat() {
        ActivityCompat.recreate(this)
    }
}

enum class ThemeType {
    DEFAULT,
    DIALOG,
}
