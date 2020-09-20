package com.fsck.k9.message.html

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class HtmlSanitizer {
    private val headCleaner = HeadCleaner()
    private val bodyCleaner = BodyCleaner()

    fun sanitize(html: String?): Document {
        val dirtyDocument = Jsoup.parse(html)
        val cleanedDocument = bodyCleaner.clean(dirtyDocument)
        headCleaner.clean(dirtyDocument, cleanedDocument)
        return cleanedDocument
    }
}
