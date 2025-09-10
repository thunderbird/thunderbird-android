package com.fsck.k9.ui.messagelist.item

import androidx.compose.runtime.Composable
import com.fsck.k9.ui.messagelist.MessageListItem
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.core.ui.compose.designsystem.organism.message.UnreadMessageItem

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
) {
    UnreadMessageItem(
        sender = "${item.displayName}",
        subject = item.subject ?: "n/a",
        preview = item.previewText,
        receivedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        avatar = {},
        onClick = onClick,
        onLongClick = onLongClick,
        onLeadingClick = onAvatarClick,
        onFavouriteChange = onFavouriteClick,
        favourite = item.isStarred,
        selected = isSelected,
    )
}
