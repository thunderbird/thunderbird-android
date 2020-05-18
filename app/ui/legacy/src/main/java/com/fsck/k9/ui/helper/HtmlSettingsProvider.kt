package com.fsck.k9.ui.helper

import com.fsck.k9.K9
import com.fsck.k9.message.html.HtmlSettings
import com.fsck.k9.ui.Theme
import com.fsck.k9.ui.ThemeManager

class HtmlSettingsProvider(private val themeManager: ThemeManager) {
    fun createForMessageView() = HtmlSettings(
            useDarkMode = themeManager.messageViewTheme == Theme.DARK,
            useFixedWidthFont = K9.isUseMessageViewFixedWidthFont
    )

    fun createForMessageCompose() = HtmlSettings(
            useDarkMode = themeManager.messageComposeTheme == Theme.DARK,
            useFixedWidthFont = false
    )
}
