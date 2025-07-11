package com.fsck.k9.preferences

import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.preference.GeneralSettingsManager

/**
 * Configures the unified inbox after an account has been added.
 */
class UnifiedInboxConfigurator(
    private val accountManager: AccountManager,
    private val generalSettingsManager: GeneralSettingsManager,
) {
    fun configureUnifiedInbox() {
        if (accountManager.getAccounts().size == 2) {
            generalSettingsManager.setIsShowUnifiedInbox(isShowUnifiedInbox = true)
        }
    }
}
