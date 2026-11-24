package app.k9mail.feature.account.edit.domain.usecase

import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AccountSyncOptions
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.Test

class GetAccountStateTest {

    @Test
    fun `should get account state from repository`() = runTest {
        val testSubject = GetAccountState(
            accountStateRepository = InMemoryAccountStateRepository(state = ACCOUNT_STATE),
        )

        val result = testSubject.execute(ACCOUNT_ID)

        assertThat(result).isEqualTo(ACCOUNT_STATE)
    }

    @Test
    fun `should throw exception WHEN account state repository contains state for different account uuid`() = runTest {
        val testSubject = GetAccountState(
            accountStateRepository = InMemoryAccountStateRepository(
                state = ACCOUNT_STATE.copy(id = AccountIdFactory.create()),
            ),
        )

        assertFailure {
            testSubject.execute(ACCOUNT_ID)
        }.isInstanceOf<IllegalStateException>()
            .hasMessage("Account state for $ACCOUNT_ID not found")
    }

    private companion object {
        val ACCOUNT_ID = AccountIdFactory.create()
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

        val AUTHORIZATION_STATE = AuthorizationState("authorization state")

        val OPTIONS = AccountOptions(
            accountName = "accountName",
            displayName = "displayName",
            emailSignature = null,
            checkFrequencyInMinutes = 15,
            messageDisplayCount = 25,
            showNotification = true,
        )

        val DISPLAY_OPTIONS = AccountDisplayOptions(
            accountName = "accountName",
            displayName = "displayName",
            emailSignature = null,
        )

        val SYNC_OPTIONS = AccountSyncOptions(
            checkFrequencyInMinutes = 15,
            messageDisplayCount = 25,
            showNotification = true,
        )

        val ACCOUNT_STATE = AccountState(
            id = ACCOUNT_ID,
            emailAddress = EMAIL_ADDRESS,
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
            authorizationState = AUTHORIZATION_STATE,
            displayOptions = DISPLAY_OPTIONS,
            syncOptions = SYNC_OPTIONS,
        )
    }
}
