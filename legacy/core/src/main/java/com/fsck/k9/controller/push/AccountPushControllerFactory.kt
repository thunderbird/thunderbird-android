package com.fsck.k9.controller.push

import app.k9mail.legacy.mailstore.FolderRepository
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.MessagingController
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId

internal class AccountPushControllerFactory(
    private val accountManager: LegacyAccountDtoManager,
    private val backendManager: BackendManager,
    private val messagingController: MessagingController,
    private val folderRepository: FolderRepository,
    private val logger: Logger,
) {
    fun create(accountId: AccountId): AccountPushController {
        return AccountPushController(
            backendManager,
            folderRepository,
            backendPusherCallback = AccountBackendPusherCallback(
                accountManager = accountManager,
                messagingController = messagingController,
                folderRepository = folderRepository,
                accountId = accountId,
                logger = logger,
            ),
            accountId = accountId,
            logger = logger,
        )
    }
}
