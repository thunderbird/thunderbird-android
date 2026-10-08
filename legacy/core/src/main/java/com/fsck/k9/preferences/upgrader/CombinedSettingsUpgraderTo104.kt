package com.fsck.k9.preferences.upgrader

import com.fsck.k9.preferences.CombinedSettingsUpgrader
import com.fsck.k9.preferences.ValidatedSettings

/**
 * Upgrades account settings by adding an avatar monogram based on the user's name or email.
 */
class CombinedSettingsUpgraderTo104 : CombinedSettingsUpgrader {
    override fun upgrade(account: ValidatedSettings.Account): ValidatedSettings.Account {
        val currentMonogram = account.settings[AVATAR_MONOGRAM_KEY] as? String
        if (currentMonogram == null || currentMonogram == AVATAR_MONOGRAM_DEFAULT) {
            val name = account.name
            val email = account.identities.firstOrNull()?.email
            val monogram = getAvatarMonogram(name, email)

            if (monogram != currentMonogram) {
                val newSettings = account.settings.toMutableMap().apply {
                    this[AVATAR_MONOGRAM_KEY] = monogram
                }

                return account.copy(settings = newSettings)
            }
        }

        return account
    }

    private fun getAvatarMonogram(name: String?, email: String?): String {
        return if (!name.isNullOrEmpty()) {
            composeAvatarMonogram(name)
        } else if (!email.isNullOrEmpty()) {
            composeAvatarMonogram(email)
        } else {
            AVATAR_MONOGRAM_DEFAULT
        }
    }

    private fun composeAvatarMonogram(name: String): String {
        return name.replace(" ", "").take(2).uppercase()
    }

    private companion object {
        const val AVATAR_MONOGRAM_KEY = "avatarMonogram"

        const val AVATAR_MONOGRAM_DEFAULT = "XX"
    }
}
