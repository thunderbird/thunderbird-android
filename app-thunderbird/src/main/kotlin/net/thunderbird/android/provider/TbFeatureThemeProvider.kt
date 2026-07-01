package net.thunderbird.android.provider

import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider

internal class TbFeatureThemeProvider : FeatureThemeProvider {
    @Composable
    override fun WithTheme(content: @Composable () -> Unit) {
        ThunderbirdBoltTheme {
            content()
        }
    }

    @Composable
    override fun WithTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
        ThunderbirdBoltTheme(darkTheme = darkTheme) {
            content()
        }
    }
}
