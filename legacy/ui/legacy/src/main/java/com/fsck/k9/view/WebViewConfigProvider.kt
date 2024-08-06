package com.fsck.k9.view

import app.k9mail.legacy.ui.theme.Theme
import app.k9mail.legacy.ui.theme.ThemeManager
import com.fsck.k9.K9

class WebViewConfigProvider(private val themeManager: ThemeManager) {
    fun createForMessageView() = createWebViewConfig(themeManager.messageViewTheme)

    fun createForMessageCompose() = createWebViewConfig(themeManager.messageComposeTheme)

    private fun createWebViewConfig(theme: Theme): WebViewConfig {
        return WebViewConfig(
            useDarkMode = theme == Theme.DARK,
            autoFitWidth = K9.isAutoFitWidth,
            textZoom = K9.fontSizes.messageViewContentAsPercent,
        )
    }
}
