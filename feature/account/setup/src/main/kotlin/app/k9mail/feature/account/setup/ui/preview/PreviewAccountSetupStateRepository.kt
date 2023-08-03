package app.k9mail.feature.account.setup.ui.preview

import app.k9mail.feature.account.oauth.domain.entity.AuthorizationState
import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.entity.AccountOptions
import app.k9mail.feature.account.setup.domain.entity.AccountSetupState
import app.k9mail.feature.account.setup.domain.entity.MailConnectionSecurity
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings

class PreviewAccountSetupStateRepository : DomainContract.AccountSetupStateRepository {
    override fun getState(): AccountSetupState = AccountSetupState(
        emailAddress = "test@example.com",
        incomingServerSettings = ServerSettings(
            type = "imap",
            host = "imap.example.com",
            port = 993,
            connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "test",
            password = "password",
            clientCertificateAlias = null,
        ),
        outgoingServerSettings = ServerSettings(
            type = "smtp",
            host = "smtp.example.com",
            port = 465,
            connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "test",
            password = "password",
            clientCertificateAlias = null,
        ),
    )

    override fun save(accountSetupState: AccountSetupState) = Unit

    override fun saveEmailAddress(emailAddress: String) = Unit

    override fun saveIncomingServerSettings(serverSettings: ServerSettings) = Unit

    override fun saveOutgoingServerSettings(serverSettings: ServerSettings) = Unit

    override fun saveAuthorizationState(authorizationState: AuthorizationState) = Unit

    override fun saveOptions(options: AccountOptions) = Unit

    override fun clear() = Unit
}
