package com.fsck.k9.message.html

class TextToHtml private constructor(private val text: CharSequence, private val html: StringBuilder) {
    fun appendAsHtmlFragment() {
        val modifications = HTML_MODIFIERS
                .flatMap { it.findModifications(text) }
                .sortedBy { it.startIndex }

        var currentIndex = 0
        modifications.forEach { modification ->
            appendHtmlEncoded(currentIndex, modification.startIndex)

            when (modification) {
                is HtmlModification.Wrap -> {
                    modification.appendPrefix(this)
                    appendHtmlEncoded(modification.startIndex, modification.endIndex)
                    modification.appendSuffix(this)
                }
                is HtmlModification.Replace -> {
                    modification.replace(this)
                }
            }

            currentIndex = modification.endIndex
        }

        appendHtmlEncoded(currentIndex, text.length)
    }

    private fun appendHtmlEncoded(startIndex: Int, endIndex: Int) {
        for (i in startIndex until endIndex) {
            appendHtmlEncoded(text[i])
        }
    }

    internal fun appendHtml(text: String) {
        html.append(text)
    }

    internal fun appendHtmlEncoded(ch: Char) {
        when (ch) {
            '&' -> html.append("&amp;")
            '<' -> html.append("&lt;")
            '>' -> html.append("&gt;")
            '\r' -> Unit
            '\n' -> html.append(TextToHtml.HTML_NEWLINE)
            else -> html.append(ch)
        }
    }

    internal fun appendHtmlAttributeEncoded(attributeValue: CharSequence) {
        for (ch in attributeValue) {
            when (ch) {
                '&' -> html.append("&amp;")
                '<' -> html.append("&lt;")
                '"' -> html.append("&quot;")
                else -> html.append(ch)
            }
        }
    }

    companion object {
        private val HTML_MODIFIERS = listOf(DividerReplacer, UriLinkifier)
        private const val HTML_NEWLINE = "<br>"
        private const val TEXT_TO_HTML_EXTRA_BUFFER_LENGTH = 512

        @JvmStatic
        fun appendAsHtmlFragment(html: StringBuilder, text: CharSequence) {
            TextToHtml(text, html).appendAsHtmlFragment()
        }

        @JvmStatic
        fun toHtmlFragment(text: CharSequence): String {
            val html = StringBuilder(text.length + TEXT_TO_HTML_EXTRA_BUFFER_LENGTH)
            TextToHtml(text, html).appendAsHtmlFragment()
            return html.toString()
        }
    }

    internal interface HtmlModifier {
        fun findModifications(text: CharSequence): List<HtmlModification>
    }
}
