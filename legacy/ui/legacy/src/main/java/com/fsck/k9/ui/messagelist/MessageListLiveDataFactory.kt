package com.fsck.k9.ui.messagelist

import app.k9mail.legacy.mailstore.MessageListRepository
import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.mail.account.api.AccountManager

class MessageListLiveDataFactory(
    private val messageListLoader: MessageListLoader,
    private val accountManager: AccountManager<LegacyAccount>,
    private val messageListRepository: MessageListRepository,
) {
    fun create(coroutineScope: CoroutineScope, config: MessageListConfig): MessageListLiveData {
        return MessageListLiveData(messageListLoader, accountManager, messageListRepository, coroutineScope, config)
    }
}
