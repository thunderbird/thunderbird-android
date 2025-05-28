package net.thunderbird.core.ui.theme.api

import androidx.compose.runtime.Composable

/**
 * Provides the compose theme for a feature.
 */
interface FeatureThemeProvider {
    @Composable
    fun WithTheme(
        content: @Composable () -> Unit,
    )

    @Composable
    fun WithTheme(
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    )
}
