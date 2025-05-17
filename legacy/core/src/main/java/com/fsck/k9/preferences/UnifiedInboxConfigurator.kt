package com.fsck.k9.preferences

import com.fsck.k9.K9
import net.thunderbird.core.android.account.AccountManager

/**
 * Configures the unified inbox after an account has been added.
 */
class UnifiedInboxConfigurator(
    private val accountManager: AccountManager,
) {
    fun configureUnifiedInbox() {
        if (accountManager.getAccounts().size == 2) {
            K9.isShowUnifiedInbox = true
            K9.saveSettingsAsync()
        }
    }
}
