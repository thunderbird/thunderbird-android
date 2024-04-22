package app.k9mail.feature.account.common.ui.fake

import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AccountSyncOptions
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.SpecialFolderSettings
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings

@Suppress("TooManyFunctions")
class FakeAccountStateRepository : AccountDomainContract.AccountStateRepository {

    override fun getState(): AccountState = AccountState(
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

    override fun setState(accountState: AccountState) = Unit

    override fun setEmailAddress(emailAddress: String) = Unit

    override fun setIncomingServerSettings(serverSettings: ServerSettings) = Unit

    override fun setOutgoingServerSettings(serverSettings: ServerSettings) = Unit

    override fun setAuthorizationState(authorizationState: AuthorizationState) = Unit

    override fun setSpecialFolderSettings(specialFolderSettings: SpecialFolderSettings) = Unit

    override fun setDisplayOptions(displayOptions: AccountDisplayOptions) = Unit

    override fun setSyncOptions(syncOptions: AccountSyncOptions) = Unit

    override fun clear() = Unit
}
