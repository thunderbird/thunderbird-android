package net.thunderbird.feature.mail.message.composer.html

import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.core.common.mail.html.HtmlSettingsProvider
import net.thunderbird.core.ui.theme.api.Theme
import net.thunderbird.core.ui.theme.api.ThemeManager

interface MessageComposerHtmlSettingsProvider : HtmlSettingsProvider

internal fun MessageComposerHtmlSettingsProvider(themeManager: ThemeManager): MessageComposerHtmlSettingsProvider =
    object : MessageComposerHtmlSettingsProvider {
        override fun create(): HtmlSettings = HtmlSettings(
            useDarkMode = themeManager.messageComposeTheme == Theme.DARK,
            useFixedWidthFont = false,
        )
    }
