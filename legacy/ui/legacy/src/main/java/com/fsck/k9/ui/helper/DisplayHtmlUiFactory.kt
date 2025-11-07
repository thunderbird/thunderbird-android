package com.fsck.k9.ui.helper

import com.fsck.k9.message.html.DisplayHtml
import net.thunderbird.feature.mail.message.composer.html.MessageComposerHtmlSettingsProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.html.MessageReaderHtmlSettingsProvider

class DisplayHtmlUiFactory(
    private val cssClassNameProvider: CssClassNameProvider,
    private val cssStyleProviders: List<CssStyleProvider.Factory>,
    private val messageReaderHtmlSettingsProvider: MessageReaderHtmlSettingsProvider,
    private val messageComposerHtmlSettingsProvider: MessageComposerHtmlSettingsProvider,
) {
    fun createForMessageView(): DisplayHtml = DisplayHtml(
        htmlSettings = messageReaderHtmlSettingsProvider.create(),
        cssClassNameProvider = cssClassNameProvider,
        cssStyleProviders = cssStyleProviders,
    )

    fun createForMessageCompose(): DisplayHtml = DisplayHtml(
        htmlSettings = messageComposerHtmlSettingsProvider.create(),
        cssClassNameProvider = cssClassNameProvider,
        cssStyleProviders = cssStyleProviders,
    )
}
