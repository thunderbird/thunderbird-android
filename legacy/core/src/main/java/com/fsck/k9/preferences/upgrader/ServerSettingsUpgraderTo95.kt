package com.fsck.k9.preferences.upgrader

import com.fsck.k9.preferences.ServerSettingsDescriptions.AUTHENTICATION_TYPE
import com.fsck.k9.preferences.ServerSettingsDescriptions.PASSWORD
import com.fsck.k9.preferences.ServerSettingsDescriptions.USERNAME
import com.fsck.k9.preferences.SettingsUpgrader

/**
 * Updates server settings to use an authentication type value of "NONE" when appropriate.
 */
class ServerSettingsUpgraderTo95 : SettingsUpgrader {
    override fun upgrade(settings: MutableMap<String, Any?>) {
        val username = settings[USERNAME] as? String

        if (username.isNullOrEmpty()) {
            settings[AUTHENTICATION_TYPE] = "NONE"
            settings[USERNAME] = ""
            settings[PASSWORD] = null
        }
    }
}
