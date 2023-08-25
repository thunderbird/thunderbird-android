package com.fsck.k9.mail.server

import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.AuthStateStorage

/**
 * Validate [ServerSettings] by trying to connect to the server and log in.
 */
fun interface ServerSettingsValidator {
    fun checkServerSettings(
        serverSettings: ServerSettings,
        authStateStorage: AuthStateStorage?,
    ): ServerSettingsValidationResult
}
