package app.k9mail.html.cleaner

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

internal class HtmlSanitizer {
    private val headCleaner = HeadCleaner()
    private val bodyCleaner = BodyCleaner()

    fun sanitize(html: String): Document {
        val dirtyDocument = Jsoup.parse(html)
        val cleanedDocument = bodyCleaner.clean(dirtyDocument)
        headCleaner.clean(dirtyDocument, cleanedDocument)
        return cleanedDocument
    }
}
