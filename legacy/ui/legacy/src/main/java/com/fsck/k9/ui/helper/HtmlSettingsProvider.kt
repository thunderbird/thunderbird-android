package com.fsck.k9.ui.helper

import app.k9mail.core.ui.theme.api.Theme
import com.fsck.k9.K9
import com.fsck.k9.message.html.HtmlSettings
import net.thunderbird.core.ui.theme.manager.ThemeManager

class HtmlSettingsProvider(private val themeManager: ThemeManager) {
    fun createForMessageView() = HtmlSettings(
        useDarkMode = themeManager.messageViewTheme == Theme.DARK,
        useFixedWidthFont = K9.isUseMessageViewFixedWidthFont,
    )

    fun createForMessageCompose() = HtmlSettings(
        useDarkMode = themeManager.messageComposeTheme == Theme.DARK,
        useFixedWidthFont = false,
    )
}
