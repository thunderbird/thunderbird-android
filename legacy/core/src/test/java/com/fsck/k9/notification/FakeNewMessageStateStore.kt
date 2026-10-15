package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference
import net.thunderbird.feature.account.AccountId

internal class FakeNewMessageStateStore : NewMessageStateStore {
    val newMessageStates = mutableMapOf<MessageReference, Boolean>()
    val clearedAccountIds = mutableListOf<AccountId>()

    override fun setNewMessageState(accountId: AccountId, messageReference: MessageReference, newMessage: Boolean) {
        newMessageStates[messageReference] = newMessage
    }

    override fun clearNewMessageState(accountId: AccountId) {
        clearedAccountIds.add(accountId)
    }
}
