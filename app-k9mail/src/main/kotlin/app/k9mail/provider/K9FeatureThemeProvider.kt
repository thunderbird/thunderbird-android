package app.k9mail.provider

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.theme2.k9mail.K9MailTheme2
import app.k9mail.feature.launcher.FeatureLauncherExternalContract

class K9FeatureThemeProvider : FeatureLauncherExternalContract.FeatureThemeProvider {
    @Composable
    override fun WithTheme(content: @Composable () -> Unit) {
        K9MailTheme2 {
            content()
        }
    }
}
