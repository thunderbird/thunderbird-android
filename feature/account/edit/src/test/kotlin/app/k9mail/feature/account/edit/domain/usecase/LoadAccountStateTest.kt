package app.k9mail.feature.account.edit.domain.usecase

import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LoadAccountStateTest {

    @Test
    fun `should load account state WHEN account in state repository has different UUID`() = runTest {
        val testSubject = LoadAccountState(
            accountLoader = { accountUuid ->
                Account(
                    uuid = accountUuid,
                    emailAddress = EMAIL_ADDRESS,
                    incomingServerSettings = INCOMING_SERVER_SETTINGS,
                    outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
                    authorizationState = AUTHORIZATION_STATE,
                    options = OPTIONS,
                )
            },
            accountStateRepository = InMemoryAccountStateRepository(),
        )

        val result = testSubject.execute(ACCOUNT_UUID)

        assertThat(result).isEqualTo(
            AccountState(
                uuid = ACCOUNT_UUID,
                emailAddress = EMAIL_ADDRESS,
                incomingServerSettings = INCOMING_SERVER_SETTINGS,
                outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
                authorizationState = AuthorizationState(AUTHORIZATION_STATE),
                options = OPTIONS,
            ),
        )
    }

    @Test
    fun `should return account state WHEN account in state repository has same UUID`() = runTest {
        val testSubject = LoadAccountState(
            accountLoader = { accountUuid ->
                Account(
                    uuid = accountUuid,
                    emailAddress = EMAIL_ADDRESS,
                    incomingServerSettings = INCOMING_SERVER_SETTINGS,
                    outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
                    authorizationState = AUTHORIZATION_STATE,
                    options = OPTIONS,
                )
            },
            accountStateRepository = InMemoryAccountStateRepository().apply {
                setState(
                    AccountState(
                        uuid = ACCOUNT_UUID,
                        emailAddress = EMAIL_ADDRESS,
                        incomingServerSettings = INCOMING_SERVER_SETTINGS,
                        outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
                        authorizationState = AuthorizationState(AUTHORIZATION_STATE),
                        options = OPTIONS,
                    ),
                )
            },
        )

        val result = testSubject.execute(ACCOUNT_UUID)

        assertThat(result).isEqualTo(
            AccountState(
                uuid = ACCOUNT_UUID,
                emailAddress = EMAIL_ADDRESS,
                incomingServerSettings = INCOMING_SERVER_SETTINGS,
                outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
                authorizationState = AuthorizationState(AUTHORIZATION_STATE),
                options = OPTIONS,
            ),
        )
    }

    @Test
    fun `should return empty account state WHEN account loader returns null`() = runTest {
        val testSubject = LoadAccountState(
            accountLoader = { null },
            accountStateRepository = InMemoryAccountStateRepository(),
        )

        val result = testSubject.execute(ACCOUNT_UUID)

        assertThat(result).isEqualTo(
            AccountState(
                uuid = ACCOUNT_UUID,
                emailAddress = null,
                incomingServerSettings = null,
                outgoingServerSettings = null,
                authorizationState = null,
                options = null,
            ),
        )
    }

    private companion object {
        const val ACCOUNT_UUID = "accountUuid"
        const val EMAIL_ADDRESS = "test@example.com"
        val INCOMING_SERVER_SETTINGS = ServerSettings(
            type = "imap",
            host = "imap.example.com",
            port = 993,
            connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )
        val OUTGOING_SERVER_SETTINGS = ServerSettings(
            type = "smtp",
            host = "smtp.example.com",
            port = 465,
            connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )

        const val AUTHORIZATION_STATE = "authorization state"

        val OPTIONS = AccountOptions(
            accountName = "accountName",
            displayName = "displayName",
            emailSignature = null,
            checkFrequencyInMinutes = 15,
            messageDisplayCount = 25,
            showNotification = true,
        )
    }
}
