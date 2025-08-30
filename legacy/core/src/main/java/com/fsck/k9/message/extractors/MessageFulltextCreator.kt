package com.fsck.k9.message.extractors

import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MessageExtractor
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.message.html.HtmlConverter

class MessageFulltextCreator private constructor(private val textPartFinder: TextPartFinder) {
    fun createFulltext(message: Message): String? {
        val textPart = textPartFinder.findFirstTextPart(message)
        if (textPart == null || hasEmptyBody(textPart)) {
            return null
        }

        val text = MessageExtractor.getTextFromPart(textPart, MAX_CHARACTERS_CHECKED_FOR_FTS)
        val mimeType = textPart.mimeType
        return if (!MimeUtility.isSameMimeType(mimeType, "text/html")) {
            text
        } else {
            HtmlConverter.htmlToText(text)
        }
    }

    private fun hasEmptyBody(textPart: Part) = textPart.getBody() == null

    companion object {
        private const val MAX_CHARACTERS_CHECKED_FOR_FTS = 200 * 1024L

        fun newInstance() = MessageFulltextCreator(TextPartFinder())
    }
}
