package app.k9mail.provider

import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.theme.k9mail.K9MailBoltTheme
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider

internal class K9FeatureThemeProvider : FeatureThemeProvider {
    @Composable
    override fun WithTheme(content: @Composable () -> Unit) {
        K9MailBoltTheme {
            content()
        }
    }

    @Composable
    override fun WithTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
        K9MailBoltTheme(darkTheme = darkTheme) {
            content()
        }
    }
}
