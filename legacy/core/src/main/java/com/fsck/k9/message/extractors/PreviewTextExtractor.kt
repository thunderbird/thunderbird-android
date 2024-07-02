package com.fsck.k9.message.extractors

import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MessageExtractor
import com.fsck.k9.mail.internet.MimeUtility.isSameMimeType
import com.fsck.k9.message.html.EmailSection
import com.fsck.k9.message.html.EmailSectionExtractor
import com.fsck.k9.message.html.HtmlConverter

internal class PreviewTextExtractor {
    @Throws(PreviewExtractionException::class)
    fun extractPreview(textPart: Part): String {
        val text = MessageExtractor.getTextFromPart(textPart, MAX_CHARACTERS_CHECKED_FOR_PREVIEW)
            ?: throw PreviewExtractionException("Couldn't get text from part")

        val plainText = convertFromHtmlIfNecessary(textPart, text)
        return stripTextForPreview(plainText)
    }

    private fun convertFromHtmlIfNecessary(textPart: Part, text: String): String {
        return if (isSameMimeType(textPart.mimeType, "text/html")) {
            HtmlConverter.htmlToText(text)
        } else {
            text
        }
    }

    private fun stripTextForPreview(text: String): String {
        var intermediateText = text

        intermediateText = normalizeLineBreaks(intermediateText)
        intermediateText = stripSignature(intermediateText)
        intermediateText = extractUnquotedText(intermediateText)

        // try to remove lines of dashes in the preview
        intermediateText = intermediateText.replace("(?m)^----.*?$".toRegex(), "")
        // Remove horizontal rules.
        intermediateText = intermediateText.replace("\\s*([-=_]{30,}+)\\s*".toRegex(), " ")

        // URLs in the preview should just be shown as "..." - They're not
        // clickable and they usually overwhelm the preview
        intermediateText = intermediateText.replace("https?://\\S+".toRegex(), "...")
        // Don't show newlines in the preview
        intermediateText = intermediateText.replace('\n', ' ')
        // Collapse whitespace in the preview
        intermediateText = intermediateText.replace("\\s+".toRegex(), " ")
        // Remove any whitespace at the beginning and end of the string.
        intermediateText = intermediateText.trim()

        return if (intermediateText.length > MAX_PREVIEW_LENGTH) {
            intermediateText.substring(0, MAX_PREVIEW_LENGTH - 1) + "…"
        } else {
            intermediateText
        }
    }

    private fun normalizeLineBreaks(text: String) = text.replace(REGEX_CRLF, "\n")

    private fun stripSignature(text: String): String {
        return if (text.startsWith("-- \n")) {
            ""
        } else {
            text.substringBefore("\n-- \n")
        }
    }

    private fun extractUnquotedText(text: String): String {
        if (text.isEmpty()) return ""
        val emailSections = EmailSectionExtractor.extract(text)
        if (emailSections.isEmpty()) return ""

        val firstEmailSection = emailSections.first()
        val replySections = if (firstEmailSection.quoteDepth == 0) {
            val replyEmailSections = emailSections.drop(1).filter { it.quoteDepth == 0 && it.isNotBlank() }
            if (firstEmailSection.isQuoteHeaderOnly()) {
                replyEmailSections
            } else {
                val firstSectionTextWithoutQuoteHeader = stripQuoteHeader(firstEmailSection)
                listOf(firstSectionTextWithoutQuoteHeader) + replyEmailSections
            }
        } else {
            emailSections.filter { it.quoteDepth == 0 && it.isNotBlank() }
        }

        return replySections.joinToString(separator = " […] ")
    }

    private fun stripQuoteHeader(emailSection: EmailSection): String {
        val quoteHeaderIndex = emailSection.quoteHeaderIndex
        if (quoteHeaderIndex == -1) return emailSection.toString()
        return emailSection.substring(startIndex = 0, endIndex = quoteHeaderIndex)
    }

    private fun EmailSection.isQuoteHeaderOnly(): Boolean {
        return quoteHeaderIndex == 0
    }

    private val EmailSection.quoteHeaderIndex: Int
        get() {
            var quoteHeaderIndex = lastIndex
            if (quoteHeaderIndex == -1) return -1

            while (quoteHeaderIndex > 0 && this[quoteHeaderIndex] == '\n') {
                quoteHeaderIndex--
            }
            if (this[quoteHeaderIndex] != ':') return -1

            var newlineCount = 0
            while (quoteHeaderIndex > 0) {
                when {
                    this[quoteHeaderIndex] == '\n' -> newlineCount++
                    newlineCount > 1 -> return quoteHeaderIndex + 1
                    else -> newlineCount = 0
                }
                quoteHeaderIndex--
            }

            return 0
        }

    companion object {
        private const val MAX_PREVIEW_LENGTH = 512
        private const val MAX_CHARACTERS_CHECKED_FOR_PREVIEW = 8192L

        private val REGEX_CRLF = "(\\r\\n|\\r)".toRegex()
    }
}
