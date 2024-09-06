package app.k9mail.core.ui.theme.api

import androidx.compose.runtime.Composable

/**
 * Provides the compose theme for a feature.
 */
fun interface FeatureThemeProvider {
    @Composable
    fun WithTheme(
        content: @Composable () -> Unit,
    )
}
