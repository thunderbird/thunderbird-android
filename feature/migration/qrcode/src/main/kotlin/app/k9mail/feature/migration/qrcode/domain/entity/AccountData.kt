package app.k9mail.feature.migration.qrcode.domain.entity

import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.common.mail.EmailAddress
import net.thunderbird.core.common.net.Hostname
import net.thunderbird.core.common.net.Port

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
