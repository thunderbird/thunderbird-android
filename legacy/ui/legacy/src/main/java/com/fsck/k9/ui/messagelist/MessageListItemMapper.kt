package com.fsck.k9.ui.messagelist

import app.k9mail.legacy.mailstore.MessageDetailsAccessor
import app.k9mail.legacy.mailstore.MessageMapper
import app.k9mail.legacy.message.extractors.PreviewResult.PreviewType
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.ui.helper.DisplayAddressHelper
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.preference.GeneralSettingsManager

class MessageListItemMapper(
    private val messageHelper: MessageHelper,
    private val account: LegacyAccount,
    private val generalSettingsManager: GeneralSettingsManager,
) : MessageMapper<MessageListItem> {

    override fun map(message: MessageDetailsAccessor): MessageListItem {
        val fromAddresses = message.fromAddresses
        val toAddresses = message.toAddresses
        val previewResult = message.preview
        val isMessageEncrypted = previewResult.previewType == PreviewType.ENCRYPTED
        val previewText = if (previewResult.isPreviewTextAvailable) previewResult.previewText else ""
        val uniqueId = createUniqueId(account, message.id)
        val showRecipients = DisplayAddressHelper.shouldShowRecipients(account, message.folderId)
        val displayAddress = if (showRecipients) toAddresses.firstOrNull() else fromAddresses.firstOrNull()
        val displayName = if (showRecipients) {
            messageHelper.getRecipientDisplayNames(
                toAddresses.toTypedArray(),
                generalSettingsManager.getSettings().isShowCorrespondentNames,
            )
        } else {
            messageHelper.getSenderDisplayName(displayAddress)
        }

        return MessageListItem(
            account,
            message.subject,
            message.threadCount,
            message.messageDate,
            message.internalDate,
            displayName,
            displayAddress,
            previewText,
            isMessageEncrypted,
            message.isRead,
            message.isStarred,
            message.isAnswered,
            message.isForwarded,
            message.hasAttachments,
            uniqueId,
            message.folderId,
            message.messageServerId,
            message.id,
            message.threadRoot,
        )
    }

    private fun createUniqueId(account: LegacyAccount, messageId: Long): Long {
        return ((account.accountNumber + 1).toLong() shl 52) + messageId
    }
}
