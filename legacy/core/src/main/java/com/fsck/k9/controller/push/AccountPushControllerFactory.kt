package com.fsck.k9.controller.push

import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.MessagingController
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.data.repository.PushFolderTrackingRepository
import net.thunderbird.feature.mail.folder.api.data.repository.PushFoldersQueryRepository

internal class AccountPushControllerFactory(
    private val accountManager: LegacyAccountDtoManager,
    private val backendManager: BackendManager,
    private val messagingController: MessagingController,
    private val pushFolderTrackingRepository: PushFolderTrackingRepository,
    private val pushFoldersQueryRepository: PushFoldersQueryRepository,
    private val logger: Logger,
) {
    fun create(accountId: AccountId): AccountPushController {
        return AccountPushController(
            backendManager,
            pushFoldersQueryRepository,
            backendPusherCallback = AccountBackendPusherCallback(
                accountManager = accountManager,
                messagingController = messagingController,
                pushFolderTrackingRepository = pushFolderTrackingRepository,
                accountId = accountId,
                logger = logger,
            ),
            accountId = accountId,
            logger = logger,
        )
    }
}
