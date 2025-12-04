package com.fsck.k9.message.html

import app.k9mail.html.cleaner.HtmlHeadProvider
import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssStyleProvider
import org.intellij.lang.annotations.Language

class DisplayHtml(
    private val htmlSettings: HtmlSettings,
    private val cssClassNameProvider: CssClassNameProvider,
    private val cssStyleProviders: List<CssStyleProvider.Factory>,
) : HtmlHeadProvider {
    override val headHtml: String
        @Language("HTML")
        get() = """
            |<meta name="viewport" content="width=device-width"/>
            |${cssStyleProviders.joinToString("\n") { it.create(htmlSettings).style }}
        """.trimMargin()

    fun wrapStatusMessage(status: CharSequence): String {
        return wrapMessageContent("<div style=\"text-align:center; color: grey;\">$status</div>")
    }

    @Language("HTML")
    fun wrapMessageContent(messageContent: CharSequence): String {
        // TODO(#10074): This should be consolidated in a single place. There are many places that
        //               build an HTML document to almost the same purpose (Viewer, Composer, etc).
        //               Current files that build the HTML document: this, HtmlProcessor.kt, and
        //               HtmlQuoteCreator.java (maybe).
        // Include a meta tag so the WebView will not use a fixed viewport width of 980 px
        return """
            |<html dir="auto">
            |  <head>
            |    $headHtml
            |  </head>
            |  <body class="${cssClassNameProvider.rootClassName} ${cssClassNameProvider.mainContentClassName}">
            |    $messageContent
            |  </body>
            |</html>
        """.trimMargin()
    }
}
