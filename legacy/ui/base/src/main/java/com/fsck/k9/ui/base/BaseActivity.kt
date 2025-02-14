package com.fsck.k9.ui.base

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.lifecycle.asLiveData
import com.fsck.k9.controller.push.PushController
import java.util.Locale
import net.thunderbird.core.ui.theme.api.Theme
import net.thunderbird.core.ui.theme.manager.ThemeManager
import org.koin.android.ext.android.inject

abstract class BaseActivity(
    private val themeType: ThemeType,
) : AppCompatActivity() {
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

        enableEdgeToEdge()

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = themeManager.appTheme == Theme.LIGHT
        }

        super.onCreate(savedInstanceState)

        setLayoutDirection()
        listenForAppLanguageChanges()
    }

    // On Android 12+ the layout direction doesn't seem to be updated when recreating the activity. This is a problem
    // when switching from an LTR to an RTL language (or the other way around) using the language picker in the app.
    private fun setLayoutDirection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, windowsInsets ->
            val insets = windowsInsets.getInsets(systemBars() or displayCutout())
            v.setPadding(insets.left, insets.top, insets.right, 0)

            windowsInsets
        }
    }

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        val newView = super.onCreateView(parent, name, context, attrs)
        if (newView != null) initializeInsets(newView)
        return newView
    }

    private fun initializeInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowsInsets ->
            val insets = windowsInsets.getInsets(displayCutout() or navigationBars())
            v.setPadding(insets.left, 0, insets.right, insets.bottom)

            windowsInsets
        }
    }

    protected fun recreateCompat() {
        ActivityCompat.recreate(this)
    }
}

enum class ThemeType {
    DEFAULT,
    DIALOG,
}
