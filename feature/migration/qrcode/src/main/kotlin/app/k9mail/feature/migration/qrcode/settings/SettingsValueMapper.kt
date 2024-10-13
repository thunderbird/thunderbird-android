package app.k9mail.feature.migration.qrcode.settings

import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.AuthenticationType
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.ConnectionSecurity
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.IncomingServerProtocol
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.OutgoingServerProtocol

internal fun IncomingServerProtocol.mapToSettingsString(): String {
    return when (this) {
        IncomingServerProtocol.Imap -> "IMAP"
        IncomingServerProtocol.Pop3 -> "POP3"
    }
}

internal fun OutgoingServerProtocol.mapToSettingsString(): String {
    return when (this) {
        OutgoingServerProtocol.Smtp -> "SMTP"
    }
}

internal fun ConnectionSecurity.mapToSettingsString(): String {
    return when (this) {
        ConnectionSecurity.Plain -> "NONE"
        ConnectionSecurity.AlwaysStartTls -> "STARTTLS_REQUIRED"
        ConnectionSecurity.Tls -> "SSL_TLS_REQUIRED"
    }
}

internal fun AuthenticationType.mapToSettingsString(): String {
    return when (this) {
        AuthenticationType.None -> "NONE"
        AuthenticationType.PasswordCleartext -> "PLAIN"
        AuthenticationType.PasswordEncrypted -> "CRAM_MD5"
        AuthenticationType.TlsCertificate -> "EXTERNAL"
        AuthenticationType.OAuth2 -> "XOAUTH2"
    }
}
