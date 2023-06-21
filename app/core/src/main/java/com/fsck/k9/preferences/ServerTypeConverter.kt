package com.fsck.k9.preferences

import app.k9mail.core.common.mail.Protocols

object ServerTypeConverter {
    @JvmStatic
    fun toServerSettingsType(exportType: String): String = exportType.lowercase()

    @JvmStatic
    fun fromServerSettingsType(serverSettingsType: String): String = when (serverSettingsType) {
        Protocols.IMAP -> "IMAP"
        Protocols.POP3 -> "POP3"
        Protocols.SMTP -> "SMTP"
        else -> throw AssertionError("Unsupported type: $serverSettingsType")
    }
}
