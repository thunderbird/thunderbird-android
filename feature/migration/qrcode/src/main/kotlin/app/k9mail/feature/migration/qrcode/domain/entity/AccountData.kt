package app.k9mail.feature.migration.qrcode.domain.entity

import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.net.Hostname
import app.k9mail.core.common.net.Port
import app.k9mail.legacy.account.DeletePolicy

internal data class AccountData(
    val sequenceNumber: Int,
    val sequenceEnd: Int,
    val accounts: List<Account>,
) {
    data class Account(
        val accountName: String,
        val deletePolicy: DeletePolicy,
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

    enum class IncomingServerProtocol {
        Imap,
        Pop3,
    }

    enum class OutgoingServerProtocol {
        Smtp,
    }

    enum class ConnectionSecurity {
        Plain,
        AlwaysStartTls,
        Tls,
    }

    enum class AuthenticationType {
        None,
        PasswordCleartext,
        PasswordEncrypted,
        TlsCertificate,
        OAuth2,
    }
}
