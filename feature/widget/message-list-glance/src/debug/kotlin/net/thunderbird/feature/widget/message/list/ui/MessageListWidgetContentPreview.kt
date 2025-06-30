package net.thunderbird.feature.widget.message.list.ui

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import app.k9mail.legacy.message.controller.MessageReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.widget.message.list.MessageListItem

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 180)
@Composable
internal fun MessageListWidgetContentPreview() {
    MessageListWidgetContent(
        mails = generateMessageListItems(),
        onOpenApp = {},
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 180)
@Composable
internal fun MessageListWidgetContentEmptyPreview() {
    MessageListWidgetContent(
        mails = persistentListOf(),
        onOpenApp = {},
    )
}

private fun generateMessageListItems(): ImmutableList<MessageListItem> {
    return persistentListOf(
        generateMessageListItem(
            displayName = "Alice",
            displayDate = "1 Jan",
            subject = "Subject 1",
            preview = "Preview 1",
            color = Color.BLUE,
            isRead = false,
        ),
        generateMessageListItem(
            displayName = "Bob",
            displayDate = "2 Jan",
            subject = "Subject 2",
            preview = "Preview 2",
            color = Color.RED,
            isRead = true,
        ),
        generateMessageListItem(
            displayName = "Charlie",
            displayDate = "3 Jan",
            subject = "Subject 3",
            preview = "Preview 3",
            color = Color.RED,
            isRead = false,
        ),
    )
}

private fun generateMessageListItem(
    displayName: String,
    displayDate: String,
    subject: String,
    preview: String,
    color: Int,
    isRead: Boolean,
): MessageListItem {
    return MessageListItem(
        displayName = displayName,
        displayDate = displayDate,
        subject = subject,
        preview = preview,
        isRead = isRead,
        hasAttachments = false,
        threadCount = 0,
        accountColor = color,
        uniqueId = 0,
        messageReference = MessageReference("accountUuid", 123, "messageServerId"),
        sortSubject = subject,
        sortMessageDate = 0,
        sortInternalDate = 0,
        sortIsStarred = false,
        sortDatabaseId = 0,
    )
}
