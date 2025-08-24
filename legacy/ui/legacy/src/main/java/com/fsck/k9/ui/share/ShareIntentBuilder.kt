package com.fsck.k9.ui.share

import android.content.Intent
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MessageExtractor
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.message.extractors.TextPartFinder
import com.fsck.k9.message.html.HtmlConverter
import com.fsck.k9.message.quote.QuoteDateFormatter

/**
 * Create a Share intent containing important headers and the body text of a message
 */
class ShareIntentBuilder(
    private val resourceProvider: CoreResourceProvider,
    private val textPartFinder: TextPartFinder,
    private val quoteDateFormatter: QuoteDateFormatter,
) {
    // TODO: Pass MessageViewInfo and extract text from there
    // TODO: Use display HTML for EXTRA_HTML_TEXT and convert it to plain text for EXTRA_TEXT
    fun createShareIntent(message: LocalMessage): Intent {
        val shareText = createShareText(message)

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
    }

    private fun createShareText(message: LocalMessage): String {
        val bodyText = extractBodyText(message)
        val sentDate = quoteDateFormatter.format(message.sentDate)

        return buildString {
            message.subject?.let { subject ->
                append(resourceProvider.messageHeaderSubject())
                append(' ')
                append(subject)
                append('\n')
            }

            if (sentDate.isNotEmpty()) {
                append(resourceProvider.messageHeaderDate())
                append(' ')
                append(sentDate)
                append('\n')
            }

            message.from.displayString()?.let { fromAddresses ->
                append(resourceProvider.messageHeaderFrom())
                append(' ')
                append(fromAddresses)
                append('\n')
            }

            message.getRecipients(RecipientType.TO).displayString()?.let { toAddresses ->
                append(resourceProvider.messageHeaderTo())
                append(' ')
                append(toAddresses)
                append('\n')
            }

            message.getRecipients(RecipientType.CC).displayString()?.let { ccAddresses ->
                append(resourceProvider.messageHeaderCc())
                append(' ')
                append(ccAddresses)
                append('\n')
            }

            append('\n')
            append(bodyText)
        }
    }

    private fun extractBodyText(message: LocalMessage): String {
        val part = textPartFinder.findFirstTextPart(message)
        if (part == null || part.body == null) return ""

        val text = MessageExtractor.getTextFromPart(part) ?: return ""
        return convertFromHtmlIfNecessary(part, text)
    }

    private fun convertFromHtmlIfNecessary(textPart: Part, text: String): String {
        return if (MimeUtility.isSameMimeType(textPart.mimeType, "text/html")) {
            HtmlConverter.htmlToText(text)
        } else {
            text
        }
    }

    private fun Array<Address>.displayString() = Address.toString(this)?.let { if (it.isEmpty()) null else it }
}
