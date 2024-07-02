package com.fsck.k9.preferences

import com.fsck.k9.preferences.Settings.IntegerRangeSetting
import com.fsck.k9.preferences.Settings.SettingsDescription
import com.fsck.k9.preferences.Settings.SettingsUpgrader
import com.fsck.k9.preferences.Settings.StringSetting
import com.fsck.k9.preferences.upgrader.ServerSettingsUpgraderTo92
import com.fsck.k9.preferences.upgrader.ServerSettingsUpgraderTo94
import com.fsck.k9.preferences.upgrader.ServerSettingsUpgraderTo95
import java.util.TreeMap

/**
 * Contains information to validate imported server settings with a given content version, and to upgrade those server
 * settings to the latest content version.
 */
@Suppress("MagicNumber")
internal class ServerSettingsDescriptions {
    val settings: Map<String, TreeMap<Int, SettingsDescription<*>>> by lazy {
        mapOf(
            HOST to versions(
                1 to StringSetting(null),
            ),
            PORT to versions(
                1 to IntegerRangeSetting(1, 65535, -1),
            ),
            CONNECTION_SECURITY to versions(
                1 to StringEnumSetting(
                    defaultValue = "SSL_TLS_REQUIRED",
                    values = setOf(
                        "NONE",
                        "STARTTLS_OPTIONAL",
                        "STARTTLS_REQUIRED",
                        "SSL_TLS_OPTIONAL",
                        "SSL_TLS_REQUIRED",
                    ),
                ),
                92 to NoDefaultStringEnumSetting(
                    values = setOf(
                        "NONE",
                        "STARTTLS_REQUIRED",
                        "SSL_TLS_REQUIRED",
                    ),
                ),
            ),
            AUTHENTICATION_TYPE to versions(
                1 to NoDefaultStringEnumSetting(
                    values = setOf(
                        "PLAIN",
                        "CRAM_MD5",
                        "EXTERNAL",
                        "XOAUTH2",
                        "AUTOMATIC",
                        "LOGIN",
                    ),
                ),
                94 to NoDefaultStringEnumSetting(
                    values = setOf(
                        "PLAIN",
                        "CRAM_MD5",
                        "EXTERNAL",
                        "XOAUTH2",
                    ),
                ),
                95 to NoDefaultStringEnumSetting(
                    values = setOf(
                        "PLAIN",
                        "CRAM_MD5",
                        "EXTERNAL",
                        "XOAUTH2",
                        "NONE",
                    ),
                ),
            ),
            USERNAME to versions(
                1 to StringSetting(""),
            ),
            PASSWORD to versions(
                1 to StringSetting(null),
            ),
            CLIENT_CERTIFICATE_ALIAS to versions(
                1 to StringSetting(null),
            ),
        )
    }

    val upgraders: Map<Int, SettingsUpgrader> by lazy {
        mapOf(
            92 to ServerSettingsUpgraderTo92(),
            94 to ServerSettingsUpgraderTo94(),
            95 to ServerSettingsUpgraderTo95(),
        )
    }

    companion object {
        const val HOST = "host"
        const val PORT = "port"
        const val CONNECTION_SECURITY = "connectionSecurity"
        const val AUTHENTICATION_TYPE = "authenticationType"
        const val USERNAME = "username"
        const val PASSWORD = "password"
        const val CLIENT_CERTIFICATE_ALIAS = "clientCertificateAlias"
    }
}

private fun versions(vararg versions: Pair<Int, SettingsDescription<*>>): TreeMap<Int, SettingsDescription<*>> {
    return TreeMap(versions.toMap())
}
