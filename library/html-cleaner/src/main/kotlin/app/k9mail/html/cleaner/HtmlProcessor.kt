package app.k9mail.html.cleaner

import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.feature.mail.message.reader.api.MessageReaderFeatureFlags
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import org.jsoup.nodes.Document

class HtmlProcessor(
    private val featureFlagProvider: FeatureFlagProvider,
    private val cssClassNameProvider: CssClassNameProvider,
    private val htmlHeadProvider: HtmlHeadProvider,
) {
    private val htmlSanitizer = HtmlSanitizer()

    fun processForDisplay(html: String): String {
        return htmlSanitizer.sanitize(html)
            .addCustomHeadContents()
            .addCustomClasses()
            .toCompactString()
    }

    private fun Document.addCustomHeadContents() = apply {
        head().append(htmlHeadProvider.headHtml)
    }

    private fun Document.toCompactString(): String {
        outputSettings()
            .prettyPrint(false)
            .indentAmount(0)

        return html()
    }

    private fun Document.addCustomClasses() = apply {
        if (featureFlagProvider.provide(MessageReaderFeatureFlags.UseNewMessageViewerCssStyles).isEnabled()) {
            body()
                .addClass(cssClassNameProvider.rootClassName)
                .addClass(cssClassNameProvider.mainContentClassName)
        }
    }
}
