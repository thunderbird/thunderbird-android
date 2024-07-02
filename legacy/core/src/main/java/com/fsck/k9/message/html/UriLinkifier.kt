package com.fsck.k9.message.html

internal object UriLinkifier : TextToHtml.HtmlModifier {
    override fun findModifications(text: CharSequence): List<HtmlModification> {
        return UriMatcher.findUris(text).map {
            LinkifyUri(it.startIndex, it.endIndex, it.uri)
        }
    }

    class LinkifyUri(
        startIndex: Int,
        endIndex: Int,
        val uri: CharSequence,
    ) : HtmlModification.Wrap(startIndex, endIndex) {

        override fun appendPrefix(textToHtml: TextToHtml) {
            textToHtml.appendHtml("<a href=\"")
            textToHtml.appendHtmlAttributeEncoded(uri)
            textToHtml.appendHtml("\">")
        }

        override fun appendSuffix(textToHtml: TextToHtml) {
            textToHtml.appendHtml("</a>")
        }

        override fun toString(): String {
            return "LinkifyUri{startIndex=$startIndex, endIndex=$endIndex, uri='$uri'}"
        }
    }
}
