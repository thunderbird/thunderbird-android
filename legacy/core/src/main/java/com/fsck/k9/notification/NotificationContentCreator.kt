package com.fsck.k9.notification

import android.text.SpannableStringBuilder
import app.k9mail.core.android.common.contact.ContactRepository
import app.k9mail.legacy.message.extractors.PreviewResult.PreviewType
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.LocalMessage
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.preference.GeneralSettingsManager

internal class NotificationContentCreator(
    private val resourceProvider: NotificationResourceProvider,
    private val contactRepository: ContactRepository,
    private val generalSettingsManager: GeneralSettingsManager,
) {
    fun createFromMessage(account: LegacyAccountDto, message: LocalMessage): NotificationContent {
        val sender = getMessageSender(account, message)

        return NotificationContent(
            messageReference = message.makeMessageReference(),
            sender = sender,
            subject = getMessageSubject(message),
            preview = getMessagePreview(message),
            summary = buildMessageSummary(sender.personal, getMessageSubject(message)),
        )
    }

    private fun getMessagePreview(message: LocalMessage): CharSequence {
        val snippet = getPreview(message)
        if (message.subject.isNullOrEmpty() && snippet != null) {
            return snippet
        }

        return SpannableStringBuilder().apply {
            val displaySubject = getMessageSubject(message)
            append(displaySubject)

            if (snippet != null) {
                append('\n')
                append(snippet)
            }
        }
    }

    private fun getPreview(message: LocalMessage): String? {
        val previewType = message.previewType ?: error("previewType == null")
        return when (previewType) {
            PreviewType.NONE, PreviewType.ERROR -> null
            PreviewType.TEXT -> message.preview
            PreviewType.ENCRYPTED -> resourceProvider.previewEncrypted()
        }
    }

    private fun buildMessageSummary(sender: String?, subject: String): CharSequence {
        return if (sender == null) {
            subject
        } else {
            SpannableStringBuilder().apply {
                append(sender)
                append(" ")
                append(subject)
            }
        }
    }

    private fun getMessageSubject(message: Message): String {
        val subject = message.subject.orEmpty()
        return subject.ifEmpty { resourceProvider.noSubject() }
    }

    @Suppress("ReturnCount")
    private fun getMessageSender(account: LegacyAccountDto, message: Message): Address {
        val localContactRepository =
            if (generalSettingsManager.getConfig().display.visualSettings.isShowContactName) contactRepository else null
        var isSelf = false

        val fromAddresses = message.from
        if (!fromAddresses.isNullOrEmpty()) {
            isSelf = account.isAnIdentity(fromAddresses)
            if (!isSelf) {
                val firstFrom = fromAddresses.first()
                val personal = MessageHelper.toFriendly(
                    firstFrom,
                    generalSettingsManager.getConfig().display.visualSettings.isShowCorrespondentNames,
                    generalSettingsManager.getConfig().display.visualSettings.isChangeContactNameColor,
                    localContactRepository,
                ).toString()
                return Address(firstFrom.address, personal)
            }
        }

        if (isSelf) {
            // show To: if the message was sent from me
            val recipients = message.getRecipients(Message.RecipientType.TO)
            if (!recipients.isNullOrEmpty()) {
                val firstRecipient = recipients.first()
                val recipientDisplayName = MessageHelper.toFriendly(
                    address = recipients.first(),
                    isShowCorrespondentNames = generalSettingsManager
                        .getConfig()
                        .display
                        .visualSettings
                        .isShowCorrespondentNames,
                    isChangeContactNameColor = generalSettingsManager
                        .getConfig()
                        .display
                        .visualSettings
                        .isChangeContactNameColor,
                    contactRepository = localContactRepository,
                ).toString()
                val personal = resourceProvider.recipientDisplayName(recipientDisplayName)
                return Address(firstRecipient.address, personal)
            }
        }

        return Address("no.sender@example.com", resourceProvider.noSender())
    }
}
