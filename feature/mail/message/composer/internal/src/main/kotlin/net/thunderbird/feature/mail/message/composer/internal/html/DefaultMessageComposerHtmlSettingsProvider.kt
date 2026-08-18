package net.thunderbird.feature.mail.message.composer.internal.html

import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.core.ui.theme.api.Theme
import net.thunderbird.core.ui.theme.api.ThemeManager
import net.thunderbird.feature.mail.message.composer.html.MessageComposerHtmlSettingsProvider

internal class DefaultMessageComposerHtmlSettingsProvider(
    private val themeManager: ThemeManager,
) : MessageComposerHtmlSettingsProvider {
    override fun create(): HtmlSettings = HtmlSettings(
        useDarkMode = themeManager.messageComposeTheme == Theme.DARK,
        useFixedWidthFont = false,
    )
}
