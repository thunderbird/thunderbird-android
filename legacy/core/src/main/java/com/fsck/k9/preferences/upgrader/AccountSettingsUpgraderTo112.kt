package com.fsck.k9.preferences.upgrader

import com.fsck.k9.preferences.SettingsUpgrader
import net.thunderbird.core.android.account.MessageFormat

/**
 * Upgrade account settings:
 * - Migrate legacy `messageFormatAuto = true` flag to `messageFormat = "AUTO"`.
 * - Remove `messageFormatAuto` setting.
 */
class AccountSettingsUpgraderTo112 : SettingsUpgrader {
    override fun upgrade(settings: MutableMap<String, Any?>) {
        val isMessageFormatAuto = settings.remove(MESSAGE_FORMAT_AUTO_KEY) as? Boolean ?: false
        if (isMessageFormatAuto) {
            settings[MESSAGE_FORMAT_KEY] = MessageFormat.AUTO.name
        }
    }

    private companion object {
        const val MESSAGE_FORMAT_KEY = "messageFormat"
        const val MESSAGE_FORMAT_AUTO_KEY = "messageFormatAuto"
    }
}
