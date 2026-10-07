package com.fsck.k9.controller.push

import com.fsck.k9.backend.api.BackendPusherCallback
import com.fsck.k9.controller.MessagingController
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.data.repository.PushFolderTrackingRepository

private const val TAG = "AccountBackendPusherCallback"

class AccountBackendPusherCallback(
    private val messagingController: MessagingController,
    private val pushFolderTrackingRepository: PushFolderTrackingRepository,
    private val accountId: AccountId,
    private val logger: Logger,
) : BackendPusherCallback {
    override fun onPushEvent(folderServerId: String) {
        messagingController.synchronizeMailboxBlocking(accountId, folderServerId)
    }

    override fun onPushError(exception: Exception) {
        messagingController.handleException(accountId, exception)
    }

    override suspend fun onPushNotSupported() {
        logger.verbose(TAG) { "Push not supported for account $accountId. Disabling push." }
        pushFolderTrackingRepository.disable(accountId)
    }
}
