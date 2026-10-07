package com.fsck.k9.preferences

import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.update

/**
 * Configures the unified inbox based on the number of accounts:
 * - If there is exactly 1 account → unified inbox is disabled.
 * - If there are exactly 2 accounts → unified inbox is enabled.
 * - For all other cases → no change is made.
 */
class UnifiedInboxConfigurator(
    private val accountManager: LegacyAccountManager,
    private val generalSettingsManager: GeneralSettingsManager,
) {
    fun configureUnifiedInbox() {
        when (accountManager.findAll().size) {
            1 -> updateUnifiedInbox(false)
            2 -> updateUnifiedInbox(true)
            else -> Unit
        }
    }
    private fun updateUnifiedInbox(isShowUnifiedInbox: Boolean) {
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    inboxSettings = settings.display.inboxSettings.copy(
                        isShowUnifiedInbox = isShowUnifiedInbox,
                    ),
                ),
            )
        }
    }
}
