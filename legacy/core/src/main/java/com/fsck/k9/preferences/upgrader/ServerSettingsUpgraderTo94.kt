package com.fsck.k9.preferences.upgrader

import com.fsck.k9.preferences.ServerSettingsDescriptions.Companion.AUTHENTICATION_TYPE
import com.fsck.k9.preferences.ServerSettingsDescriptions.Companion.CONNECTION_SECURITY
import com.fsck.k9.preferences.SettingsUpgrader

/**
 * Removes legacy authentication values.
 *
 * Replaces the authentication value "AUTOMATIC" with "PLAIN" when TLS is used, "CRAM_MD5" otherwise.
 * Replaces the authentication value "LOGIN" with "PLAIN".
 */
class ServerSettingsUpgraderTo94 : SettingsUpgrader {
    override fun upgrade(settings: MutableMap<String, Any?>) {
        val connectionSecurity = settings[CONNECTION_SECURITY] as? String
        val isSecure = connectionSecurity == "STARTTLS_REQUIRED" || connectionSecurity == "SSL_TLS_REQUIRED"
        val authenticationType = settings[AUTHENTICATION_TYPE] as? String

        settings[AUTHENTICATION_TYPE] = when (authenticationType) {
            "AUTOMATIC" -> if (isSecure) "PLAIN" else "CRAM_MD5"
            "LOGIN" -> "PLAIN"
            else -> authenticationType
        }
    }
}
