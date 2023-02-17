package com.fsck.k9.ui.helper

import com.fsck.k9.K9
import com.fsck.k9.message.html.HtmlSettings
import com.fsck.k9.ui.base.Theme
import com.fsck.k9.ui.base.ThemeManager

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
