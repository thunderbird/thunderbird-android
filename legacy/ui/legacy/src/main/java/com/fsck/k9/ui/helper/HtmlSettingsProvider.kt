package com.fsck.k9.ui.helper

import com.fsck.k9.message.html.HtmlSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.ui.theme.api.Theme
import net.thunderbird.core.ui.theme.manager.ThemeManager

class HtmlSettingsProvider(
    private val themeManager: ThemeManager,
    private val generalSettingsManager: GeneralSettingsManager,
) {
    fun createForMessageView() = HtmlSettings(
        useDarkMode = themeManager.messageViewTheme == Theme.DARK,
        useFixedWidthFont = generalSettingsManager.getConfig().display.isUseMessageViewFixedWidthFont,
    )

    fun createForMessageCompose() = HtmlSettings(
        useDarkMode = themeManager.messageComposeTheme == Theme.DARK,
        useFixedWidthFont = false,
    )
}
