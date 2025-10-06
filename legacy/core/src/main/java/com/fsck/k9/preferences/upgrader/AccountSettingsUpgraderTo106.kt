package com.fsck.k9.preferences.upgrader

import com.fsck.k9.preferences.SettingsUpgrader
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler.Companion.INCOMING_SERVER_SETTINGS_KEY
import net.thunderbird.feature.account.storage.legacy.serializer.ServerSettingsDtoSerializer
import net.thunderbird.feature.mail.folder.api.FOLDER_DEFAULT_PATH_DELIMITER

class AccountSettingsUpgraderTo106(
    private val serverSettingsDtoSerializer: ServerSettingsDtoSerializer,
) : SettingsUpgrader {
    override fun upgrade(settings: MutableMap<String, Any?>) {
        val incomingSettings = (settings[INCOMING_SERVER_SETTINGS_KEY] as? String)
            ?.let(serverSettingsDtoSerializer::deserialize)
        val pathPrefix = incomingSettings?.extra["pathPrefix"]
        settings[LegacyAccountStorageHandler.FOLDER_PATH_DELIMITER_KEY] = pathPrefix
            .orEmpty()
            .ifBlank { FOLDER_DEFAULT_PATH_DELIMITER }
    }
}
