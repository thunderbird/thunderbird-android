package com.fsck.k9.view

import app.k9mail.core.ui.theme.api.Theme
import com.fsck.k9.K9
import net.thunderbird.core.ui.theme.manager.ThemeManager

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
