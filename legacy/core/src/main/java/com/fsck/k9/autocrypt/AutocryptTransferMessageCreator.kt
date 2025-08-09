package com.fsck.k9.autocrypt

import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.internet.MimeBodyPart
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.fsck.k9.mail.internet.MimeMultipart
import com.fsck.k9.mail.internet.TextBody
import com.fsck.k9.mailstore.BinaryMemoryBody
import java.util.Date
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.preference.GeneralSettingsManager

class AutocryptTransferMessageCreator(
    private val stringProvider: AutocryptStringProvider,
    private val generalSettingsManager: GeneralSettingsManager,
) {
    fun createAutocryptTransferMessage(data: ByteArray, address: Address): Message {
        try {
            val subjectText = stringProvider.transferMessageSubject()
            val messageText = stringProvider.transferMessageBody()

            val textBodyPart = MimeBodyPart.create(TextBody(messageText))
            val dataBodyPart = MimeBodyPart.create(BinaryMemoryBody(data, "7bit"))
            dataBodyPart.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "application/autocrypt-setup")
            dataBodyPart.setHeader(
                MimeHeader.HEADER_CONTENT_DISPOSITION,
                "attachment; filename=\"autocrypt-setup-message\"",
            )

            val messageBody = MimeMultipart.newInstance()
            messageBody.addBodyPart(textBodyPart)
            messageBody.addBodyPart(dataBodyPart)

            val message = MimeMessage.create()
            MimeMessageHelper.setBody(message, messageBody)

            val nowDate = Date()

            message.setFlag(Flag.X_DOWNLOADED_FULL, true)
            message.subject = subjectText
            message.setHeader("Autocrypt-Setup-Message", "v1")
            message.internalDate = nowDate
            message.addSentDate(
                nowDate,
                generalSettingsManager.getSettings().privacy.isHideTimeZone,
            )
            message.setFrom(address)
            message.setHeader("To", address.toEncodedString())

            return message
        } catch (e: MessagingException) {
            throw AssertionError(e)
        }
    }
}
