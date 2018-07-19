package com.fsck.k9.preferences

import java.util.Locale

object ServerTypeConverter {
    @JvmStatic
    fun toServerSettingsType(exportType: String): String = exportType.toLowerCase(Locale.ROOT)

    @JvmStatic
    fun fromServerSettingsType(serverSettingsType: String): String = when (serverSettingsType) {
        Protocols.IMAP -> "IMAP"
        Protocols.POP3 -> "POP3"
        Protocols.WEBDAV -> "WebDAV"
        Protocols.SMTP -> "SMTP"
        else -> throw AssertionError("Unsupported type: $serverSettingsType")
    }
}
