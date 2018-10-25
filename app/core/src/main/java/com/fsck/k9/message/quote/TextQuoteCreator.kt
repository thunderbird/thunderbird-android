package com.fsck.k9.message.quote


import com.fsck.k9.Account.QuoteStyle
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.message.quote.QuoteHelper.Companion.QUOTE_BUFFER_LENGTH


class TextQuoteCreator(private val quoteHelper: QuoteHelper, private val resourceProvider: CoreResourceProvider) {
    private val prefixInsertionRegex = Regex("(?m)^")


    fun quoteOriginalTextMessage(
            originalMessage: Message,
            messageBody: String?,
            quoteStyle: QuoteStyle,
            prefix: String
    ): String {
        val body = messageBody ?: ""
        return when (quoteStyle) {
            QuoteStyle.PREFIX -> prefixQuoteText(body, originalMessage, prefix)
            QuoteStyle.HEADER -> headerQuoteText(body, originalMessage)
        }
    }

    private fun prefixQuoteText(body: String, originalMessage: Message, prefix: String): String {
        val sentDate = quoteHelper.getSentDateText(originalMessage)
        val sender = Address.toString(originalMessage.from)

        return StringBuilder(body.length + QUOTE_BUFFER_LENGTH).apply {
            val replyHeader = if (sentDate.isEmpty()) {
                resourceProvider.replyHeader(sender)
            } else {
                resourceProvider.replyHeader(sender, sentDate)
            }
            append(replyHeader)
            append(CRLF)

            val escapedPrefix = Regex.escapeReplacement(prefix)
            val prefixedText = body.replace(prefixInsertionRegex, escapedPrefix)

            append(prefixedText)
        }.toString()
    }

    private fun headerQuoteText(body: String, originalMessage: Message): String {
        val sentDate = quoteHelper.getSentDateText(originalMessage)

        return StringBuilder(body.length + QUOTE_BUFFER_LENGTH).apply {
            append(CRLF)
            append(resourceProvider.messageHeaderSeparator())
            append(CRLF)

            originalMessage.from.displayString()?.let { fromAddresses ->
                append(resourceProvider.messageHeaderFrom())
                append(" ")
                append(fromAddresses)
                append(CRLF)
            }

            if (sentDate.isNotEmpty()) {
                append(resourceProvider.messageHeaderDate())
                append(" ")
                append(sentDate)
                append(CRLF)
            }

            originalMessage.getRecipients(RecipientType.TO).displayString()?.let { toAddresses ->
                append(resourceProvider.messageHeaderTo())
                append(" ")
                append(toAddresses)
                append(CRLF)
            }

            originalMessage.getRecipients(RecipientType.CC).displayString()?.let { ccAddresses ->
                append(resourceProvider.messageHeaderCc())
                append(" ")
                append(ccAddresses)
                append(CRLF)
            }

            originalMessage.subject?.let { subject ->
                append(resourceProvider.messageHeaderSubject())
                append(" ")
                append(subject)
                append(CRLF)
            }

            append(CRLF)
            append(body)
        }.toString()
    }

    private fun Array<Address>.displayString() = Address.toString(this)?.let { if (it.isEmpty()) null else it }


    companion object {
        private const val CRLF = "\r\n"
    }
}
