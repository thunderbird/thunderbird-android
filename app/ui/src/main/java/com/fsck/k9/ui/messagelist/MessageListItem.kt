package com.fsck.k9.ui.messagelist

import androidx.annotation.ColorInt
import com.fsck.k9.Account
import com.fsck.k9.mail.Address

data class MessageListItem(
        val id: Long,
        val messageUid: String,
        val folderServerId: String,
        val displayName: String,
        val subject: String,
        val date: Long,
        val sigil: String,
        val threadCount: Int,
        val threadId: Long,
        val read: Boolean,
        val answered: Boolean,
        val forwarded: Boolean,
        @ColorInt val chipColor: Int,
        val flagged: Boolean,
        val hasAttachments: Boolean,
        val preview: String,
        val counterPartyAddresses: Address?,
        val account: Account,
        val selectionIdentifier: Long
)