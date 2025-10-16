package com.fsck.k9.ui.messagelist.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.fsck.k9.ui.messagelist.MessageListAppearance
import com.fsck.k9.ui.messagelist.MessageListItem
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.core.ui.compose.designsystem.organism.message.ActiveMessageItem
import net.thunderbird.core.ui.compose.designsystem.organism.message.ReadMessageItem
import net.thunderbird.core.ui.compose.designsystem.organism.message.UnreadMessageItem

@Suppress("LongParameterList")
@OptIn(ExperimentalTime::class)
@Composable
internal fun MessageItemContent(
    item: MessageListItem,
    isActive: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onFavouriteClick: (Boolean) -> Unit,
    appearance: MessageListAppearance,
) {
    val receivedAt = remember(item.messageDate) {
        Instant.fromEpochMilliseconds(item.messageDate)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }

    when {
        isActive -> ActiveMessageItem(
            sender = "${item.displayName}",
            subject = item.subject ?: "n/a",
            preview = item.previewText,
            receivedAt = receivedAt,
            avatar = {},
            onClick = onClick,
            onLongClick = onLongClick,
            onLeadingClick = onAvatarClick,
            onFavouriteChange = onFavouriteClick,
            favourite = item.isStarred,
            selected = isSelected,
            maxPreviewLines = appearance.previewLines,
            threadCount = item.threadCount,
            hasAttachments = item.hasAttachments,
            swapSenderWithSubject = !appearance.senderAboveSubject,
        )
        item.isRead -> ReadMessageItem(
            sender = "${item.displayName}",
            subject = item.subject ?: "n/a",
            preview = item.previewText,
            receivedAt = receivedAt,
            avatar = {},
            onClick = onClick,
            onLongClick = onLongClick,
            onLeadingClick = onAvatarClick,
            onFavouriteChange = onFavouriteClick,
            favourite = item.isStarred,
            selected = isSelected,
            maxPreviewLines = appearance.previewLines,
            threadCount = item.threadCount,
            hasAttachments = item.hasAttachments,
            swapSenderWithSubject = !appearance.senderAboveSubject,
        )
        else -> UnreadMessageItem(
            sender = "${item.displayName}",
            subject = item.subject ?: "n/a",
            preview = item.previewText,
            receivedAt = receivedAt,
            avatar = {},
            onClick = onClick,
            onLongClick = onLongClick,
            onLeadingClick = onAvatarClick,
            onFavouriteChange = onFavouriteClick,
            favourite = item.isStarred,
            selected = isSelected,
            maxPreviewLines = appearance.previewLines,
            threadCount = item.threadCount,
            hasAttachments = item.hasAttachments,
            swapSenderWithSubject = !appearance.senderAboveSubject,
        )
    }
}
