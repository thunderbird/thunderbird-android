package com.fsck.k9.ui.messagelist

import app.k9mail.legacy.mailstore.MessageListRepository
import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.android.account.LegacyAccountManager

class MessageListLiveDataFactory(
    private val messageListLoader: MessageListLoader,
    private val accountManager: LegacyAccountManager,
    private val messageListRepository: MessageListRepository,
) {
    fun create(coroutineScope: CoroutineScope, config: MessageListConfig): MessageListLiveData {
        return MessageListLiveData(messageListLoader, accountManager, messageListRepository, coroutineScope, config)
    }
}
