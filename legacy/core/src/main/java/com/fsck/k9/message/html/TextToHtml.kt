package com.fsck.k9.message.html

import java.util.ArrayDeque

class TextToHtml private constructor(
    private val text: CharSequence,
    private val html: StringBuilder,
    private val retainOriginalWhitespace: Boolean,
) {
    fun appendAsHtmlFragment() {
        appendHtmlPrefix()

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
                        "outer HtmlModification ${modificationStack.peek()}",
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

        appendHtmlSuffix()
    }

    private fun appendHtmlPrefix() {
        html.append("""<span dir="auto">""")
    }

    private fun appendHtmlSuffix() {
        html.append("</span>")
    }

    private fun appendHtmlEncoded(startIndex: Int, endIndex: Int) {
        if (retainOriginalWhitespace) {
            appendHtmlEncodedWithOriginalWhitespace(startIndex, endIndex)
        } else {
            appendHtmlEncodedWithNonBreakingSpaces(startIndex, endIndex)
        }
    }

    private fun appendHtmlEncodedWithOriginalWhitespace(startIndex: Int, endIndex: Int) {
        for (i in startIndex until endIndex) {
            appendHtmlEncoded(text[i])
        }
    }

    private fun appendHtmlEncodedWithNonBreakingSpaces(startIndex: Int, endIndex: Int) {
        var adjustedStartIndex = startIndex
        if (startIndex < endIndex && text[startIndex] == SPACE) {
            html.append(NON_BREAKING_SPACE)
            adjustedStartIndex++
        }

        var spaces = 0
        for (i in adjustedStartIndex until endIndex) {
            if (text[i] == SPACE) {
                spaces++
            } else {
                appendSpaces(spaces)
                spaces = 0
                appendHtmlEncoded(text[i])
            }
        }

        appendSpaces(spaces)
    }

    private fun appendSpaces(count: Int) {
        if (count <= 0) return

        repeat(count - 1) {
            html.append(NON_BREAKING_SPACE)
        }
        html.append(SPACE)
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

        private const val SPACE = ' '
        private const val NON_BREAKING_SPACE = '\u00A0'

        private const val HTML_NEWLINE = "<br>"
        private const val TEXT_TO_HTML_EXTRA_BUFFER_LENGTH = 512

        @JvmStatic
        fun appendAsHtmlFragment(html: StringBuilder, text: CharSequence, retainOriginalWhitespace: Boolean) {
            TextToHtml(text, html, retainOriginalWhitespace).appendAsHtmlFragment()
        }

        @JvmStatic
        fun toHtmlFragment(text: CharSequence, retainOriginalWhitespace: Boolean): String {
            val html = StringBuilder(text.length + TEXT_TO_HTML_EXTRA_BUFFER_LENGTH)
            TextToHtml(text, html, retainOriginalWhitespace).appendAsHtmlFragment()
            return html.toString()
        }
    }

    internal interface HtmlModifier {
        fun findModifications(text: CharSequence): List<HtmlModification>
    }
}
