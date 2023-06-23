package com.fsck.k9.mail.server

import com.fsck.k9.mail.ServerSettings

/**
 * Validate [ServerSettings] by trying to connect to the server and log in.
 */
interface ServerSettingsValidator {
    fun checkServerSettings(serverSettings: ServerSettings): ServerSettingsValidationResult
}
