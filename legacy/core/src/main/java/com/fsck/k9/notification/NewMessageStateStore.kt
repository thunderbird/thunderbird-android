package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference
import net.thunderbird.feature.account.AccountId

/**
 * Stores whether messages are new, i.e. whether they still have a new message notification.
 */
internal interface NewMessageStateStore {
    fun setNewMessageState(accountId: AccountId, messageReference: MessageReference, newMessage: Boolean)

    fun clearNewMessageState(accountId: AccountId)
}
