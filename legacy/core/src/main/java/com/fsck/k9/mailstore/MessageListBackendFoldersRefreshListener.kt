package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.MessageListRepository
import net.thunderbird.feature.account.AccountId

/**
 * Makes message lists reload after the folder list has been refreshed, because the displayed folder might be gone.
 */
class MessageListBackendFoldersRefreshListener(
    private val messageListRepository: MessageListRepository,
    private val accountId: AccountId,
) : BackendFoldersRefreshListener {
    override fun onBeforeFolderListRefresh() = Unit

    override suspend fun onAfterFolderListRefresh() {
        messageListRepository.notifyMessageListChanged(accountId)
    }
}
