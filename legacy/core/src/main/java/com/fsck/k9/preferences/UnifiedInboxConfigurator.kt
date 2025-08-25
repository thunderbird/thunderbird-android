package com.fsck.k9.preferences

import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.update

/**
 * Configures the unified inbox after an account has been added.
 */
class UnifiedInboxConfigurator(
    private val accountManager: AccountManager,
    private val generalSettingsManager: GeneralSettingsManager,
) {
    fun configureUnifiedInbox() {
        if (accountManager.getAccounts().size == 2) {
            generalSettingsManager.update { settings ->
                settings.copy(
                    display = settings.display.copy(
                        inboxSettings = settings.display.inboxSettings.copy(
                            isShowUnifiedInbox = true,
                        ),
                    ),
                )
            }
        }
    }
}
