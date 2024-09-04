package com.fsck.k9.preferences

import com.fsck.k9.preferences.Settings.IntegerRangeSetting
import com.fsck.k9.preferences.Settings.StringSetting
import com.fsck.k9.preferences.upgrader.ServerSettingsUpgraderTo92
import com.fsck.k9.preferences.upgrader.ServerSettingsUpgraderTo94
import com.fsck.k9.preferences.upgrader.ServerSettingsUpgraderTo95

/**
 * Contains information to validate imported server settings with a given content version, and to upgrade those server
 * settings to the latest content version.
 */
@Suppress("MagicNumber")
internal object ServerSettingsDescriptions {
    const val HOST = "host"
    const val PORT = "port"
    const val CONNECTION_SECURITY = "connectionSecurity"
    const val AUTHENTICATION_TYPE = "authenticationType"
    const val USERNAME = "username"
    const val PASSWORD = "password"
    const val CLIENT_CERTIFICATE_ALIAS = "clientCertificateAlias"

    val SETTINGS: SettingsDescriptions = mapOf(
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

    val UPGRADERS: Map<Int, SettingsUpgrader> = mapOf(
        92 to ServerSettingsUpgraderTo92(),
        94 to ServerSettingsUpgraderTo94(),
        95 to ServerSettingsUpgraderTo95(),
    )
}
