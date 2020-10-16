package com.fsck.k9.message.html

import java.util.ArrayDeque

class TextToHtml private constructor(private val text: CharSequence, private val html: StringBuilder) {
    fun appendAsHtmlFragment() {
        val modifications = HTML_MODIFIERS
            .flatMap { it.findModifications(text) }
            .sortedBy { it.startIndex }

        val modificationStack = ArrayDeque<HtmlModification.Wrap>()
        var currentIndex = 0
        modifications.forEach { modification ->
            while (modification.startIndex >= modificationStack.peek()?.endIndex ?: Int.MAX_VALUE) {
                val outerModification = modificationStack.pop()
                appendHtmlEncoded(currentIndex, outerModification.endIndex)
                outerModification.appendSuffix(this)
                currentIndex = outerModification.endIndex
            }

            appendHtmlEncoded(currentIndex, modification.startIndex)

            if (modification.endIndex > modificationStack.peek()?.endIndex ?: Int.MAX_VALUE) {
                error(
                    "HtmlModification $modification must be fully contained within " +
                        "outer HtmlModification ${modificationStack.peek()}"
                )
            }

            when (modification) {
                is HtmlModification.Wrap -> {
                    modification.appendPrefix(this)
                    modificationStack.push(modification)
                    currentIndex = modification.startIndex
                }
                is HtmlModification.Replace -> {
                    modification.replace(this)
                    currentIndex = modification.endIndex
                }
            }
        }

        while (modificationStack.isNotEmpty()) {
            val outerModification = modificationStack.pop()
            appendHtmlEncoded(currentIndex, outerModification.endIndex)
            outerModification.appendSuffix(this)
            currentIndex = outerModification.endIndex
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
            '\n' -> html.append(HTML_NEWLINE)
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
        private val HTML_MODIFIERS = listOf(DividerReplacer, UriLinkifier, SignatureWrapper)
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
