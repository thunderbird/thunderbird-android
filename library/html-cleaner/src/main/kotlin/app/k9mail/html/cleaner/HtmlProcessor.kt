package app.k9mail.html.cleaner

import org.jsoup.nodes.Document

class HtmlProcessor(
    private val customClasses: Set<String>,
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
        if (customClasses.isNotEmpty()) {
            body().apply {
                customClasses.forEach(::addClass)
            }
        }
    }
}
