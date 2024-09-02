package com.fsck.k9.preferences.upgrader

import com.fsck.k9.preferences.ServerSettingsDescriptions.Companion.CONNECTION_SECURITY
import com.fsck.k9.preferences.SettingsUpgrader

class ServerSettingsUpgraderTo92 : SettingsUpgrader {
    override fun upgrade(settings: MutableMap<String, Any?>) {
        val oldConnectionSecurity = settings[CONNECTION_SECURITY] as? String

        settings[CONNECTION_SECURITY] = when (oldConnectionSecurity) {
            "NONE" -> "NONE"
            "STARTTLS_OPTIONAL" -> "STARTTLS_REQUIRED"
            "STARTTLS_REQUIRED" -> "STARTTLS_REQUIRED"
            "SSL_TLS_OPTIONAL" -> "SSL_TLS_REQUIRED"
            "SSL_TLS_REQUIRED" -> "SSL_TLS_REQUIRED"
            else -> "SSL_TLS_REQUIRED"
        }
    }
}
