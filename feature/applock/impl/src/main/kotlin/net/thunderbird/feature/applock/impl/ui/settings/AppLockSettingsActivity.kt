package net.thunderbird.feature.applock.impl.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject

internal class AppLockSettingsActivity : FragmentActivity() {

    private val themeProvider: FeatureThemeProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            themeProvider.WithTheme {
                AppLockSettingsScreen(
                    onBack = { finish() },
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, AppLockSettingsActivity::class.java)
        }
    }
}
