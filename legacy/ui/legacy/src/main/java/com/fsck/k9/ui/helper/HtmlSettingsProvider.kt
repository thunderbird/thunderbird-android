package com.fsck.k9.ui.helper

import app.k9mail.legacy.ui.theme.Theme
import app.k9mail.legacy.ui.theme.ThemeManager
import com.fsck.k9.K9
import com.fsck.k9.message.html.HtmlSettings

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
