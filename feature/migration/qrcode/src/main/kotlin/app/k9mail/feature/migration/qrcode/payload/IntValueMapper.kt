@file:Suppress("MagicNumber")

package app.k9mail.feature.migration.qrcode.payload

import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.AuthenticationType
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.ConnectionSecurity
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.IncomingServerProtocol
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.OutgoingServerProtocol

internal fun Int.toIncomingServerProtocol(): IncomingServerProtocol {
    return when (this) {
        0 -> IncomingServerProtocol.Imap
        1 -> IncomingServerProtocol.Pop3
        else -> throw IllegalArgumentException("Unsupported value: $this")
    }
}

internal fun Int.toOutgoingServerProtocol(): OutgoingServerProtocol {
    return when (this) {
        0 -> OutgoingServerProtocol.Smtp
        else -> throw IllegalArgumentException("Unsupported value: $this")
    }
}

internal fun Int.toConnectionSecurity(): ConnectionSecurity {
    return when (this) {
        0 -> ConnectionSecurity.Plain
        1 -> ConnectionSecurity.AlwaysStartTls // TryStartTls, but we treat it like AlwaysStartTls
        2 -> ConnectionSecurity.AlwaysStartTls
        3 -> ConnectionSecurity.Tls
        else -> throw IllegalArgumentException("Unsupported value: $this")
    }
}

@Suppress("ThrowsCount")
internal fun Int.toAuthenticationType(): AuthenticationType {
    return when (this) {
        0 -> AuthenticationType.None
        1 -> AuthenticationType.PasswordCleartext
        2 -> AuthenticationType.PasswordEncrypted
        3 -> throw IllegalArgumentException("Unsupported authentication method: Gssapi")
        4 -> throw IllegalArgumentException("Unsupported authentication method: Ntlm")
        5 -> AuthenticationType.TlsCertificate
        6 -> AuthenticationType.OAuth2
        else -> throw IllegalArgumentException("Unsupported value: $this")
    }
}
