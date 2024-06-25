package com.fsck.k9.preferences.upgrader

import com.fsck.k9.preferences.ServerSettingsDescriptions.Companion.AUTHENTICATION_TYPE
import com.fsck.k9.preferences.ServerSettingsDescriptions.Companion.PASSWORD
import com.fsck.k9.preferences.ServerSettingsDescriptions.Companion.USERNAME
import com.fsck.k9.preferences.Settings.SettingsUpgrader

/**
 * Updates server settings to use an authentication type value of "NONE" when appropriate.
 */
class ServerSettingsUpgraderTo95 : SettingsUpgrader {
    override fun upgrade(settings: MutableMap<String, Any?>): Set<String> {
        val username = settings[USERNAME] as? String

        if (username.isNullOrEmpty()) {
            settings[AUTHENTICATION_TYPE] = "NONE"
            settings[USERNAME] = ""
            settings[PASSWORD] = null
        }

        return emptySet()
    }
}
