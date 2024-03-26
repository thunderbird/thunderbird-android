package app.k9mail.feature.launcher

import androidx.compose.runtime.Composable

/**
 * Contract defining the external functionality of the feature launcher to be provided by the host application.
 */
interface FeatureLauncherExternalContract {

    /**
     * Provides the theme for the feature.
     */
    fun interface FeatureThemeProvider {
        @Composable
        fun WithTheme(
            content: @Composable () -> Unit,
        )
    }

    fun interface AccountSetupFinishedLauncher {
        fun launch(accountUuid: String?)
    }
}
