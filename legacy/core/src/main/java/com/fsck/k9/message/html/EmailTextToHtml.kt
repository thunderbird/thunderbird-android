package com.fsck.k9.message.html

import app.k9mail.legacy.di.DI
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssVariableNameProvider
import org.intellij.lang.annotations.Language

class EmailTextToHtml private constructor(
    private val cssClassNameProvider: CssClassNameProvider,
    private val cssVariableNameProvider: CssVariableNameProvider,
    private val text: String,
) {
    private val html = StringBuilder(text.length + EXTRA_BUFFER_LENGTH)
    private var previousQuoteDepth = 0

    fun convert(): String {
        appendHtmlPrefix()

        val sections = EmailSectionExtractor.extract(text)
        sections.forEach { section ->
            appendBlockQuoteElement(section.quoteDepth)

            TextToHtml.appendAsHtmlFragment(html, section, retainOriginalWhitespace = true)
        }

        appendBlockQuoteElement(quoteDepth = 0)

        appendHtmlSuffix()

        return html.toString()
    }

    private fun appendHtmlPrefix() {
        html.append("<pre class=\"${cssClassNameProvider.plainTextMessagePreClassName}\">")
    }

    private fun appendHtmlSuffix() {
        html.append("</pre>")
    }

    private fun appendBlockQuoteElement(quoteDepth: Int) {
        if (previousQuoteDepth > quoteDepth) {
            repeat(previousQuoteDepth - quoteDepth) {
                html.append("</blockquote>")
            }
        } else if (quoteDepth > previousQuoteDepth) {
            for (depth in (previousQuoteDepth + 1)..quoteDepth) {
                val borderColorStyle =
                    "${cssVariableNameProvider.blockquoteDefaultBorderLeftColor}: ${quoteColor(depth)};"

                @Language("HTML")
                val blockQuoteStartTag =
                    "<blockquote class=\"gmail_quote\" style=\"margin-bottom: 1ex; $borderColorStyle\">"
                html.append(blockQuoteStartTag)
            }
        }
        previousQuoteDepth = quoteDepth
    }

    private fun quoteColor(depth: Int): String = when (depth) {
        1 -> "#729fcf"
        2 -> "#ad7fa8"
        3 -> "#8ae234"
        4 -> "#fcaf3e"
        5 -> "#e9b96e"
        else -> "#ccc"
    }

    companion object {
        private const val EXTRA_BUFFER_LENGTH = 2048
        const val K9MAIL_CSS_CLASS = "k9mail"

        @JvmStatic
        fun convert(text: String): String {
            val cssClassNameProvider = DI.get<CssClassNameProvider>()
            val cssVariableNameProvider = DI.get<CssVariableNameProvider>()
            return EmailTextToHtml(cssClassNameProvider, cssVariableNameProvider, text).convert()
        }
    }
}
