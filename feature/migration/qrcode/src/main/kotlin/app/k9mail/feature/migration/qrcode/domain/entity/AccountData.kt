package app.k9mail.feature.migration.qrcode.domain.entity

import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.net.Hostname
import app.k9mail.core.common.net.Port

internal data class AccountData(
    val sequenceNumber: Int,
    val sequenceEnd: Int,
    val accounts: List<Account>,
) {
    data class Account(
        val accountName: String,
        val incomingServer: IncomingServer,
        val outgoingServerGroups: List<OutgoingServerGroup>,
    )

    data class IncomingServer(
        val protocol: IncomingServerProtocol,
        val hostname: Hostname,
        val port: Port,
        val connectionSecurity: ConnectionSecurity,
        val authenticationType: AuthenticationType,
        val username: String,
        val password: String?,
    )

    data class OutgoingServer(
        val protocol: OutgoingServerProtocol,
        val hostname: Hostname,
        val port: Port,
        val connectionSecurity: ConnectionSecurity,
        val authenticationType: AuthenticationType,
        val username: String,
        val password: String?,
    )

    data class OutgoingServerGroup(
        val outgoingServer: OutgoingServer,
        val identities: List<Identity>,
    )

    data class Identity(
        val emailAddress: EmailAddress,
        val displayName: String,
    )

    @Suppress("MagicNumber")
    enum class IncomingServerProtocol(val intValue: Int) {
        Imap(0),
        Pop3(1),
        ;

        companion object {
            fun fromInt(value: Int): IncomingServerProtocol {
                return requireNotNull(entries.find { it.intValue == value }) { "Unsupported value: $value" }
            }
        }
    }

    @Suppress("MagicNumber")
    enum class OutgoingServerProtocol(val intValue: Int) {
        Smtp(0),
        ;

        companion object {
            fun fromInt(value: Int): OutgoingServerProtocol {
                return requireNotNull(entries.find { it.intValue == value }) { "Unsupported value: $value" }
            }
        }
    }

    @Suppress("MagicNumber")
    enum class ConnectionSecurity(val intValue: Int) {
        Plain(0),
        TryStartTls(1),
        AlwaysStartTls(2),
        Tls(3),
        ;

        companion object {
            fun fromInt(value: Int): ConnectionSecurity {
                return requireNotNull(entries.find { it.intValue == value }) { "Unsupported value: $value" }
            }
        }
    }

    @Suppress("MagicNumber")
    enum class AuthenticationType(val intValue: Int) {
        None(0),
        PasswordCleartext(1),
        PasswordEncrypted(2),
        Gssapi(3),
        Ntlm(4),
        TlsCertificate(5),
        OAuth2(6),
        ;

        companion object {
            fun fromInt(value: Int): AuthenticationType {
                return requireNotNull(entries.find { it.intValue == value }) { "Unsupported value: $value" }
            }
        }
    }
}
