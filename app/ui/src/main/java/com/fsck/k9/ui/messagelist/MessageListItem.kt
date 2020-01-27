package com.fsck.k9.ui.messagelist

import com.fsck.k9.Account
import com.fsck.k9.mail.Address

data class MessageListItem(
    val position: Int,
    val account: Account,
    val subject: String?,
    val threadCount: Int,
    val messageDate: Long,
    val displayName: CharSequence,
    val counterPartyAddress: Address?,
    val fromMe: Boolean,
    val toMe: Boolean,
    val ccMe: Boolean,
    val previewText: String,
    val isMessageEncrypted: Boolean,
    val isRead: Boolean,
    val isStarred: Boolean,
    val isAnswered: Boolean,
    val isForwarded: Boolean,
    val hasAttachments: Boolean,
    val uniqueId: Long,
    val folderServerId: String,
    val messageUid: String,
    val databaseId: Long,
    val senderAddress: String?,
    val threadRoot: Long
)
