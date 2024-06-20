package app.k9mail.feature.widget.message.list

import com.fsck.k9.controller.MessageReference

internal data class MessageListItem(
    val displayName: String,
    val displayDate: String,
    val subject: String,
    val preview: String,
    val isRead: Boolean,
    val hasAttachments: Boolean,
    val threadCount: Int,
    val accountColor: Int,
    val messageReference: MessageReference,
    val uniqueId: Long,

    val sortSubject: String?,
    val sortMessageDate: Long,
    val sortInternalDate: Long,
    val sortIsStarred: Boolean,
    val sortDatabaseId: Long,
)
