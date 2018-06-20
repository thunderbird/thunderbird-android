package com.fsck.k9.autocrypt


import java.util.Date

import android.content.res.Resources

import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.internet.MimeBodyPart
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.fsck.k9.mail.internet.MimeMultipart
import com.fsck.k9.mail.internet.TextBody
import com.fsck.k9.mailstore.BinaryMemoryBody


class AutocryptTransferMessageCreator(val resources: Resources) {
    fun createAutocryptTransferMessage(data: ByteArray, address: Address): Message {
        try {
            val subjectText = resources.getString(R.string.ac_transfer_msg_subject)
            val messageText = resources.getString(R.string.ac_transfer_msg_body)

            val textBodyPart = MimeBodyPart(TextBody(messageText))
            val dataBodyPart = MimeBodyPart(BinaryMemoryBody(data, "7bit"))
            dataBodyPart.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "application/autocrypt-setup")
            dataBodyPart.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, "attachment; filename=\"autocrypt-setup-message\"")

            val messageBody = MimeMultipart.newInstance()
            messageBody.addBodyPart(textBodyPart)
            messageBody.addBodyPart(dataBodyPart)

            val message = MimeMessage()
            MimeMessageHelper.setBody(message, messageBody)

            val nowDate = Date()

            message.setFlag(Flag.X_DOWNLOADED_FULL, true)
            message.subject = subjectText
            message.setHeader("Autocrypt-Setup-Message", "v1")
            message.internalDate = nowDate
            message.addSentDate(nowDate, K9.hideTimeZone())
            message.setFrom(address)
            message.setRecipients(RecipientType.TO, arrayOf(address))

            return message
        } catch (e: MessagingException) {
            throw AssertionError(e)
        }

    }
}
