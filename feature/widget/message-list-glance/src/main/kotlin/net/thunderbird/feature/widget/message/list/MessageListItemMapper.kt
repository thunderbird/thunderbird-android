package net.thunderbird.feature.widget.message.list

import app.k9mail.legacy.account.LegacyAccount
import app.k9mail.legacy.mailstore.MessageDetailsAccessor
import app.k9mail.legacy.mailstore.MessageMapper
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.ui.helper.DisplayAddressHelper
import java.util.Calendar
import java.util.Locale

internal class MessageListItemMapper(
    private val messageHelper: MessageHelper,
    private val account: LegacyAccount,
) : MessageMapper<MessageListItem> {
    private val calendar: Calendar = Calendar.getInstance()

    override fun map(message: MessageDetailsAccessor): MessageListItem {
        val fromAddresses = message.fromAddresses
        val toAddresses = message.toAddresses
        val previewResult = message.preview
        val previewText = if (previewResult.isPreviewTextAvailable) previewResult.previewText else ""
        val uniqueId = createUniqueId(account, message.id)
        val showRecipients = DisplayAddressHelper.shouldShowRecipients(account, message.folderId)
        val displayAddress = if (showRecipients) toAddresses.firstOrNull() else fromAddresses.firstOrNull()
        val displayName = if (showRecipients) {
            messageHelper.getRecipientDisplayNames(toAddresses.toTypedArray()).toString()
        } else {
            messageHelper.getSenderDisplayName(displayAddress).toString()
        }

        return MessageListItem(
            displayName = displayName,
            displayDate = formatDate(message.messageDate),
            subject = message.subject.orEmpty(),
            preview = previewText,
            isRead = message.isRead,
            hasAttachments = message.hasAttachments,
            threadCount = message.threadCount,
            accountColor = account.chipColor,
            messageReference = MessageReference(account.uuid, message.folderId, message.messageServerId),
            uniqueId = uniqueId,
            sortSubject = message.subject,
            sortMessageDate = message.messageDate,
            sortInternalDate = message.internalDate,
            sortIsStarred = message.isStarred,
            sortDatabaseId = message.id,
        )
    }

    @Suppress("ImplicitDefaultLocale")
    private fun formatDate(date: Long): String {
        calendar.timeInMillis = date
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())

        return String.format("%d %s", dayOfMonth, month)
    }

    private fun createUniqueId(account: LegacyAccount, messageId: Long): Long {
        return ((account.accountNumber + 1).toLong() shl ACCOUNT_NUMBER_BIT_SHIFT) + messageId
    }

    private companion object {
        const val ACCOUNT_NUMBER_BIT_SHIFT = 52
    }
}
