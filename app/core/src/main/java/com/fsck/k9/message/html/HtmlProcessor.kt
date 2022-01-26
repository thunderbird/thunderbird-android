package com.fsck.k9.message.html

import org.jsoup.nodes.Document

class HtmlProcessor internal constructor(private val htmlHeadProvider: HtmlHeadProvider) {
    private val htmlSanitizer = HtmlSanitizer()

    fun processForDisplay(html: String?): String {
        return htmlSanitizer.sanitize(html)
            .addCustomHeadContents()
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
}
