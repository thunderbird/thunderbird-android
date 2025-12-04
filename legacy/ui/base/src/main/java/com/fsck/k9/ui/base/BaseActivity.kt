package com.fsck.k9.ui.base

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.updatePadding
import androidx.lifecycle.asLiveData
import com.fsck.k9.controller.push.PushController
import java.util.Locale
import kotlin.math.max
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

    /**
     * Sets the activity's content view to the specified layout resource and configures
     * the AppBarLayout and Toolbar for edge-to-edge display.
     *
     * It expects the layout to contain the following views with the specified IDs:
     * - A ViewGroup with ID `R.id.coordinator_layout` as the root layout.
     * - An AppBarLayout with ID `R.id.app_bar_layout` for the top app bar.
     * - A Toolbar with ID `R.id.toolbar` to be set as the support action bar.
     * - A ViewGroup with ID `R.id.content_container` to hold the main content of the activity or fragment.
     *
     * @param layoutResId The resource ID of the layout to be set as the content view.
     * @throws IllegalStateException if the required AppBarLayout or Toolbar is not found.
     */
    protected fun setLayout(@LayoutRes layoutResId: Int) {
        setContentView(layoutResId)

        val coordinatorLayout = findViewById<ViewGroup>(R.id.coordinator_layout)
            ?: error("TB layouts must provide a ViewGroup with id='coordinator_layout'.")
        val appBarLayout = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.app_bar_layout)
            ?: error("TB layouts must provide an AppBarLayout with id='app_bar_layout'.")
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
            ?: error("TB layouts must provide a Toolbar with id='toolbar'.")
        val contentContainer = findViewById<ViewGroup>(R.id.content_container)
            ?: error("TB layouts must provide a FrameLayout with id='content_container'.")

        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(coordinatorLayout) { _, windowInsets ->
            val insets = windowInsets.getInsets(
                systemBars() or displayCutout(),
            )
            val imeInsets = windowInsets.getInsets(ime())

            appBarLayout.updatePadding(
                top = insets.top,
                left = insets.left,
                right = insets.right,
            )

            contentContainer.updatePadding(
                left = insets.left,
                right = insets.right,
                bottom = max(insets.bottom, imeInsets.bottom),
            )

            WindowInsetsCompat.CONSUMED
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
