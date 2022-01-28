package app.k9mail.html.cleaner

import org.jsoup.nodes.Document

class HtmlProcessor(private val htmlHeadProvider: HtmlHeadProvider) {
    private val htmlSanitizer = HtmlSanitizer()

    fun processForDisplay(html: String): String {
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
