package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CreateAccountTest {

    @Test
    fun `should successfully create account`() = runTest {
        var recordedAccount: Account? = null
        val createAccount = CreateAccount(
            accountCreator = { account ->
                recordedAccount = account
                AccountCreatorResult.Success(accountUuid = "uuid")
            },
            uuidGenerator = { "uuid" },
        )

        val emailAddress = "user@example.com"
        val incomingServerSettings = ServerSettings(
            type = "imap",
            host = "imap.example.com",
            port = 993,
            connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )
        val outgoingServerSettings = ServerSettings(
            type = "smtp",
            host = "smtp.example.com",
            port = 465,
            connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )
        val authorizationState = "authorization state"
        val options = AccountOptions(
            accountName = "accountName",
            displayName = "displayName",
            emailSignature = null,
            checkFrequencyInMinutes = 15,
            messageDisplayCount = 25,
            showNotification = true,
        )

        val result = createAccount.execute(
            emailAddress,
            incomingServerSettings,
            outgoingServerSettings,
            authorizationState,
            options,
        )

        assertThat(result).isEqualTo("uuid")
        assertThat(recordedAccount).isEqualTo(
            Account(
                uuid = "uuid",
                emailAddress = emailAddress,
                incomingServerSettings = incomingServerSettings,
                outgoingServerSettings = outgoingServerSettings,
                authorizationState = authorizationState,
                options = options,
            ),
        )
    }
}
