package com.fsck.k9.controller

import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.account.AccountCapabilities
import net.thunderbird.feature.account.AccountId

class RealAccountCapabilities(
    private val messagingController: MessagingController,
    private val accountManager: LegacyAccountDtoManager,
): AccountCapabilities {
    override fun supportsUpload(accountId: AccountId): Boolean {
       val account = accountManager.getById(accountId) ?: return false
       return messagingController.supportsUpload(account)
    }
}
