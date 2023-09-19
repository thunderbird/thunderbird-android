package app.k9mail.feature.account.edit.domain.usecase

import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterFailure
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterResult
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SaveServerSettingsTest {

    @Test
    fun `should get account state and update incoming server settings`() = runTest {
        var recordedAccountUuid: String? = null
        var recordedIsIncoming: Boolean? = null
        var recordedServerSettings: ServerSettings? = null
        val testSubject = SaveServerSettings(
            getAccountState = { _ -> ACCOUNT_STATE },
            serverSettingsUpdater = { accountUuid, isIncoming, serverSettings ->
                recordedAccountUuid = accountUuid
                recordedIsIncoming = isIncoming
                recordedServerSettings = serverSettings

                AccountUpdaterResult.Success(accountUuid)
            },
        )

        testSubject.execute(ACCOUNT_UUID, isIncoming = true)

        assertThat(recordedAccountUuid).isEqualTo(ACCOUNT_UUID)
        assertThat(recordedIsIncoming).isEqualTo(true)
        assertThat(recordedServerSettings).isEqualTo(INCOMING_SERVER_SETTINGS)
    }

    @Test
    fun `should throw exception WHEN no incoming server settings present`() = runTest {
        val testSubject = SaveServerSettings(
            getAccountState = { _ -> ACCOUNT_STATE.copy(incomingServerSettings = null) },
            serverSettingsUpdater = { accountUuid, _, _ ->
                AccountUpdaterResult.Success(accountUuid)
            },
        )

        assertFailure {
            testSubject.execute(ACCOUNT_UUID, isIncoming = true)
        }.isInstanceOf<IllegalStateException>()
            .prop(IllegalStateException::message)
            .isEqualTo("Server settings not found")
    }

    @Test
    fun `should get account state and update outgoing server settings`() = runTest {
        var recordedAccountUuid: String? = null
        var recordedIsIncoming: Boolean? = null
        var recordedServerSettings: ServerSettings? = null
        val testSubject = SaveServerSettings(
            getAccountState = { _ -> ACCOUNT_STATE },
            serverSettingsUpdater = { accountUuid, isIncoming, serverSettings ->
                recordedAccountUuid = accountUuid
                recordedIsIncoming = isIncoming
                recordedServerSettings = serverSettings

                AccountUpdaterResult.Success(accountUuid)
            },
        )

        testSubject.execute(ACCOUNT_UUID, isIncoming = false)

        assertThat(recordedAccountUuid).isEqualTo(ACCOUNT_UUID)
        assertThat(recordedIsIncoming).isEqualTo(false)
        assertThat(recordedServerSettings).isEqualTo(OUTGOING_SERVER_SETTINGS)
    }

    @Test
    fun `should throw exception WHEN no outgoing server settings present`() = runTest {
        val testSubject = SaveServerSettings(
            getAccountState = { _ -> ACCOUNT_STATE.copy(outgoingServerSettings = null) },
            serverSettingsUpdater = { accountUuid, _, _ ->
                AccountUpdaterResult.Success(accountUuid)
            },
        )

        assertFailure {
            testSubject.execute(ACCOUNT_UUID, isIncoming = false)
        }.isInstanceOf<IllegalStateException>()
            .prop(IllegalStateException::message)
            .isEqualTo("Server settings not found")
    }

    @Test
    fun `should throw exception WHEN update failed`() = runTest {
        val testSubject = SaveServerSettings(
            getAccountState = { _ -> ACCOUNT_STATE },
            serverSettingsUpdater = { _, _, _ ->
                AccountUpdaterResult.Failure(
                    AccountUpdaterFailure.AccountNotFound(ACCOUNT_UUID),
                )
            },
        )

        assertFailure {
            testSubject.execute(ACCOUNT_UUID, isIncoming = true)
        }.isInstanceOf<IllegalStateException>()
            .prop(IllegalStateException::message)
            .isEqualTo("Server settings update failed")
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

        val AUTHORIZATION_STATE = AuthorizationState("authorization state")

        val OPTIONS = AccountOptions(
            accountName = "accountName",
            displayName = "displayName",
            emailSignature = null,
            checkFrequencyInMinutes = 15,
            messageDisplayCount = 25,
            showNotification = true,
        )

        val ACCOUNT_STATE = AccountState(
            uuid = ACCOUNT_UUID,
            emailAddress = EMAIL_ADDRESS,
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
            authorizationState = AUTHORIZATION_STATE,
            options = OPTIONS,
        )
    }
}
