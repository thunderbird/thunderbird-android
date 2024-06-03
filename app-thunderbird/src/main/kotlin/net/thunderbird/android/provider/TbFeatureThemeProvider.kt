package net.thunderbird.android.provider

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.theme2.thunderbird.ThunderbirdTheme2
import app.k9mail.feature.launcher.FeatureLauncherExternalContract

class TbFeatureThemeProvider : FeatureLauncherExternalContract.FeatureThemeProvider {
    @Composable
    override fun WithTheme(content: @Composable () -> Unit) {
        ThunderbirdTheme2 {
            content()
        }
    }
}
