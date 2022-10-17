package com.fsck.k9.widget.list

import android.net.Uri

internal data class MessageListItem(
    val displayName: String,
    val displayDate: String,
    val subject: String,
    val preview: String,
    val isRead: Boolean,
    val hasAttachments: Boolean,
    val uri: Uri,
    val accountColor: Int,
    val uniqueId: Long,

    val sortSubject: String?,
    val sortMessageDate: Long,
    val sortInternalDate: Long,
    val sortIsStarred: Boolean,
    val sortDatabaseId: Long,
)
