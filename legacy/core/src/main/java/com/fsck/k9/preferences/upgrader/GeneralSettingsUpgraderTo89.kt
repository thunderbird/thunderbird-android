package com.fsck.k9.preferences.upgrader

import com.fsck.k9.preferences.SettingsUpgrader

/**
 * Combine `messageViewReturnToList` and `messageViewShowNext` into `messageViewPostDeleteAction`.
 */
class GeneralSettingsUpgraderTo89 : SettingsUpgrader {
    override fun upgrade(settings: MutableMap<String, Any?>) {
        val messageViewReturnToList = settings["messageViewReturnToList"] as? Boolean
        val messageViewShowNext = settings["messageViewShowNext"] as? Boolean

        if (messageViewReturnToList == true) {
            settings["messageViewPostDeleteAction"] = "ReturnToMessageList"
        } else if (messageViewShowNext == true) {
            settings["messageViewPostDeleteAction"] = "ShowNextMessage"
        } else {
            settings["messageViewPostDeleteAction"] = "ShowPreviousMessage"
        }
    }
}
