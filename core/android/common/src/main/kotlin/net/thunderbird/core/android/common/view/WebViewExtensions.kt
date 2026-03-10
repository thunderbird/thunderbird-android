package net.thunderbird.core.android.common.view

import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

fun WebView.showInDarkMode() = setupThemeMode(darkTheme = true)
fun WebView.showInLightMode() = setupThemeMode(darkTheme = false)

private fun WebView.setupThemeMode(darkTheme: Boolean) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(
            this.settings,
            darkTheme,
        )
    }
}
