package app.k9mail.html.cleaner

import org.jsoup.nodes.Document

class HtmlProcessor(private val htmlHeadProvider: HtmlHeadProvider) {
    private val htmlSanitizer = HtmlSanitizer()

    fun processForDisplay(html: String): String {
        return htmlSanitizer.sanitize(html)
            .addCustomHeadContents()
            .wrapContent()
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

    private fun Document.wrapContent() = apply {
        val elements = body().children().clone()
        body().remove()

        val wrapper = createElement("div")
            .classNames(setOf("message", "message-content"))
            .appendChild(
                createElement("div")
                    .classNames(setOf("clear"))
                    .appendChildren(elements),
            )

        body().appendChild(wrapper)
    }
}
