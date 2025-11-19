package net.thunderbird.feature.mail.message.reader.impl.html

import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsPreferenceManager
import net.thunderbird.core.ui.theme.api.Theme
import net.thunderbird.core.ui.theme.api.ThemeManager
import net.thunderbird.feature.mail.message.reader.api.html.MessageReaderHtmlSettingsProvider

internal class DefaultMessageReaderHtmlSettingsProvider(
    private val themeManager: ThemeManager,
    private val visualSettingsPreferenceManager: DisplayVisualSettingsPreferenceManager,
) : MessageReaderHtmlSettingsProvider {
    override fun create(): HtmlSettings = HtmlSettings(
        useDarkMode = themeManager.messageViewTheme == Theme.DARK,
        useFixedWidthFont = visualSettingsPreferenceManager.getConfig().isUseMessageViewFixedWidthFont,
    )
}
