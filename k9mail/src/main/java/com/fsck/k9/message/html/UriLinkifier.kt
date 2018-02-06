package com.fsck.k9.message.html


@Deprecated("Helper to be able to transition to the new text to HTML conversion in smaller steps")
object UriLinkifier {
    @JvmStatic
    fun linkifyText(text: String, html: StringBuffer) {
        val uriMatches = UriMatcher.findUris(text)

        var currentIndex = 0
        uriMatches.forEach { uriMatch ->
            append(html, text, currentIndex, uriMatch.startIndex)

            html.append("<a href=\"")
            html.append(uriMatch.uri)
            html.append("\">")
            html.append(uriMatch.uri)
            html.append("</a>")

            currentIndex = uriMatch.endIndex
        }

        append(html, text, currentIndex, text.length)
    }

    private fun append(html: StringBuffer, text: String, startIndex: Int, endIndex: Int) {
        for (i in startIndex until endIndex) {
            html.append(text[i])
        }
    }
}
