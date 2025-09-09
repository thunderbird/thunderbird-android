package com.fsck.k9.ui.messagelist

import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.mail.Address
import net.thunderbird.core.android.account.LegacyAccount

data class MessageListItem(
    val account: LegacyAccount,
    val subject: String?,
    val threadCount: Int,
    val messageDate: Long,
    val internalDate: Long,
    val displayName: CharSequence,
    val displayAddress: Address?,
    val previewText: String,
    val isMessageEncrypted: Boolean,
    val isRead: Boolean,
    val isStarred: Boolean,
    val isAnswered: Boolean,
    val isForwarded: Boolean,
    val hasAttachments: Boolean,
    val uniqueId: Long,
    val folderId: Long,
    val messageUid: String,
    val databaseId: Long,
    val threadRoot: Long,
) {
    val messageReference: MessageReference
        get() = MessageReference(account.uuid, folderId, messageUid)
}
