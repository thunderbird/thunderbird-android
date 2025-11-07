package com.fsck.k9.message.html

import app.k9mail.html.cleaner.HtmlProcessor
import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider

class HtmlProcessorFactory(
    private val featureFlagProvider: FeatureFlagProvider,
    private val cssClassNameProvider: CssClassNameProvider,
    private val displayHtmlFactory: DisplayHtmlFactory,
) {
    fun create(settings: HtmlSettings): HtmlProcessor {
        val displayHtml = displayHtmlFactory.create(settings)
        return HtmlProcessor(featureFlagProvider, cssClassNameProvider, displayHtml)
    }
}
