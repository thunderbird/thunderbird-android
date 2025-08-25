package app.k9mail.provider

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.theme2.k9mail.K9MailTheme2
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider

internal class K9FeatureThemeProvider : FeatureThemeProvider {
    @Composable
    override fun WithTheme(content: @Composable () -> Unit) {
        K9MailTheme2 {
            content()
        }
    }

    @Composable
    override fun WithTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
        K9MailTheme2(darkTheme = darkTheme) {
            content()
        }
    }
}
