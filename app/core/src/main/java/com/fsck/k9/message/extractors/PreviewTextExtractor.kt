package com.fsck.k9.message.extractors

import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MessageExtractor
import com.fsck.k9.mail.internet.MimeUtility.isSameMimeType
import com.fsck.k9.message.html.HtmlConverter.htmlToText

internal class PreviewTextExtractor {
    @Throws(PreviewExtractionException::class)
    fun extractPreview(textPart: Part): String {
        val text = MessageExtractor.getTextFromPart(textPart, MAX_CHARACTERS_CHECKED_FOR_PREVIEW.toLong())
            ?: throw PreviewExtractionException("Couldn't get text from part")

        val plainText = convertFromHtmlIfNecessary(textPart, text)
        return stripTextForPreview(plainText)
    }

    private fun convertFromHtmlIfNecessary(textPart: Part, text: String): String {
        return if (isSameMimeType(textPart.mimeType, "text/html")) {
            htmlToText(text)
        } else {
            text
        }
    }

    private fun stripTextForPreview(text: String): String {
        // Remove (correctly delimited by '-- \n') signatures
        var text = text.replace("(?ms)^-- [\\r\\n]+.*".toRegex(), "")
        // try to remove lines of dashes in the preview
        text = text.replace("(?m)^----.*?$".toRegex(), "")
        // remove quoted text from the preview
        text = text.replace("(?m)^[#>].*$".toRegex(), "")
        // Remove a common quote header from the preview
        text = text.replace("(?m)^On .*wrote.?$".toRegex(), "")
        // Remove a more generic quote header from the preview
        text = text.replace("(?m)^.*\\w+:$".toRegex(), "")
        // Remove horizontal rules.
        text = text.replace("\\s*([-=_]{30,}+)\\s*".toRegex(), " ")

        // URLs in the preview should just be shown as "..." - They're not
        // clickable and they usually overwhelm the preview
        text = text.replace("https?://\\S+".toRegex(), "...")
        // Don't show newlines in the preview
        text = text.replace("(\\r|\\n)+".toRegex(), " ")
        // Collapse whitespace in the preview
        text = text.replace("\\s+".toRegex(), " ")
        // Remove any whitespace at the beginning and end of the string.
        text = text.trim { it <= ' ' }

        return if (text.length > MAX_PREVIEW_LENGTH) {
            text.substring(0, MAX_PREVIEW_LENGTH - 1) + "…"
        } else {
            text
        }
    }

    companion object {
        private const val MAX_PREVIEW_LENGTH = 512
        private const val MAX_CHARACTERS_CHECKED_FOR_PREVIEW = 8192
    }
}
