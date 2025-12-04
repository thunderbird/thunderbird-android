package com.fsck.k9.message.html

import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssStyleProvider

class DisplayHtmlFactory(
    private val cssClassNameProvider: CssClassNameProvider,
    private val cssStyleProviders: List<CssStyleProvider.Factory>,
) {
    fun create(settings: HtmlSettings): DisplayHtml = DisplayHtml(
        htmlSettings = settings,
        cssClassNameProvider = cssClassNameProvider,
        cssStyleProviders = cssStyleProviders,
    )
}
