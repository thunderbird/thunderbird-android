package com.fsck.k9.controller.push

import app.k9mail.legacy.account.LegacyAccount
import app.k9mail.legacy.mailstore.FolderRepository
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.MessagingController

internal class AccountPushControllerFactory(
    private val backendManager: BackendManager,
    private val messagingController: MessagingController,
    private val folderRepository: FolderRepository,
) {
    fun create(account: LegacyAccount): AccountPushController {
        return AccountPushController(
            backendManager,
            messagingController,
            folderRepository,
            account = account,
        )
    }
}
