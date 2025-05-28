package com.fsck.k9.preferences.upgrader

import com.fsck.k9.preferences.SettingsUpgrader

/**
 * Upgrades account settings by adding an avatar monogram based on the user's name or email.
 */
class AccountSettingsUpgraderTo104 : SettingsUpgrader {
    override fun upgrade(settings: MutableMap<String, Any?>) {
        val name = settings[NAME_KEY] as? String
        val email = settings[EMAIL_KEY] as? String
        settings[AVATAR_MONOGRAM_KEY] = getAvatarMonogram(name, email)
    }

    private fun getAvatarMonogram(name: String?, email: String?): String {
        return if (name != null && name.isNotEmpty()) {
            composeAvatarMonogram(name)
        } else if (email != null && email.isNotEmpty()) {
            composeAvatarMonogram(email)
        } else {
            AVATAR_MONOGRAM_DEFAULT
        }
    }

    private fun composeAvatarMonogram(name: String): String {
        return name.replace(" ", "").take(2).uppercase()
    }

    private companion object {

        const val NAME_KEY = "name"
        const val EMAIL_KEY = "email"
        const val AVATAR_MONOGRAM_KEY = "avatarMonogram"

        const val AVATAR_MONOGRAM_DEFAULT = "XX"
    }
}
