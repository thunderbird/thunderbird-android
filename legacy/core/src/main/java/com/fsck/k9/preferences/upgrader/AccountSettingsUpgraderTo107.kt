package com.fsck.k9.preferences.upgrader

import com.fsck.k9.preferences.SettingsUpgrader

/**
 * Upgrade account settings
 *
 * - update the automatic check interval default from 60 to 15 minutes without
 *   overwriting user-customized values (e.g., manual, 30 min, etc.).
 */
class AccountSettingsUpgraderTo107 : SettingsUpgrader {
    override fun upgrade(settings: MutableMap<String, Any?>) {
        val current = settings[AUTOMATIC_CHECK_INTERVAL_MINUTES_KEY] as? Int

        when (current) {
            null -> settings[AUTOMATIC_CHECK_INTERVAL_MINUTES_KEY] = NEW_CHECK_INTERVAL_MINUTES
            OLD_DEFAULT_CHECK_INTERVAL_MINUTES -> {
                settings[AUTOMATIC_CHECK_INTERVAL_MINUTES_KEY] = NEW_CHECK_INTERVAL_MINUTES
            }
            else -> {
                // Keep user-selected value as-is
            }
        }
    }

    private companion object {
        const val AUTOMATIC_CHECK_INTERVAL_MINUTES_KEY = "automaticCheckIntervalMinutes"
        const val OLD_DEFAULT_CHECK_INTERVAL_MINUTES = 60
        const val NEW_CHECK_INTERVAL_MINUTES = 15
    }
}
