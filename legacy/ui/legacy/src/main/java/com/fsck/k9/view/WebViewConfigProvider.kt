package com.fsck.k9.view

import com.fsck.k9.K9
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.ui.theme.api.Theme
import net.thunderbird.core.ui.theme.manager.ThemeManager

class WebViewConfigProvider(
    private val themeManager: ThemeManager,
    private val generalSettingsManager: GeneralSettingsManager,
) {
    fun createForMessageView() = createWebViewConfig(themeManager.messageViewTheme)

    fun createForMessageCompose() = createWebViewConfig(themeManager.messageComposeTheme)

    private fun createWebViewConfig(theme: Theme): WebViewConfig {
        return WebViewConfig(
            useDarkMode = theme == Theme.DARK,
            autoFitWidth = generalSettingsManager.getConfig().display.visualSettings.isAutoFitWidth,
            textZoom = K9.fontSizes.messageViewContentAsPercent,
        )
    }
}
