package com.fsck.k9.message.html

import com.fsck.k9.K9

class DisplayHtmlFactory {
    fun create(): DisplayHtml {
        return DisplayHtml(createHtmlSettings())
    }

    private fun createHtmlSettings(): HtmlSettings {
        return HtmlSettings(
                useDarkMode = K9.k9MessageViewTheme == K9.Theme.DARK,
                useFixedWidthFont = K9.isUseMessageViewFixedWidthFont
        )
    }
}
