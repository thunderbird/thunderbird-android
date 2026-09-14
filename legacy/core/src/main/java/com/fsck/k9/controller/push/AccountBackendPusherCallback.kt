package com.fsck.k9.controller.push

import com.fsck.k9.backend.api.BackendPusherCallback
import com.fsck.k9.controller.MessagingController
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.data.repository.PushFolderTrackingRepository

private const val TAG = "AccountBackendPusherCallback"

class AccountBackendPusherCallback(
    private val accountManager: LegacyAccountDtoManager,
    private val messagingController: MessagingController,
    private val pushFolderTrackingRepository: PushFolderTrackingRepository,
    private val accountId: AccountId,
    private val logger: Logger,
) : BackendPusherCallback {
    override fun onPushEvent(folderServerId: String) {
        val account = accountManager.getAccount(accountId.toString())
        messagingController.synchronizeMailboxBlocking(account, folderServerId)
    }

    override fun onPushError(exception: Exception) {
        val account = accountManager.getAccount(accountId.toString())
        messagingController.handleException(account, exception)
    }

    override suspend fun onPushNotSupported() {
        logger.verbose(TAG) { "Push not supported for account $accountId. Disabling push." }
        pushFolderTrackingRepository.disable(accountId)
    }
}
