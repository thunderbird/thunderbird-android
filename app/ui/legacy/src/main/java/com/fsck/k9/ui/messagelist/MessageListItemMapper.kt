package com.fsck.k9.ui.messagelist

import com.fsck.k9.Account
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mailstore.MessageDetailsAccessor
import com.fsck.k9.mailstore.MessageMapper
import com.fsck.k9.message.extractors.PreviewResult.PreviewType
import com.fsck.k9.ui.helper.DisplayAddressHelper

class MessageListItemMapper(
    private val messageHelper: MessageHelper,
    private val account: Account,
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
            messageHelper.getRecipientDisplayNames(toAddresses.toTypedArray())
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

    private fun createUniqueId(account: Account, messageId: Long): Long {
        return ((account.accountNumber + 1).toLong() shl 52) + messageId
    }
}
