package com.fsck.k9.account

import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterFailure
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterResult
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.Before
import org.junit.Test
import net.thunderbird.core.android.account.LegacyAccountDto as K9Account

class AccountServerSettingsUpdaterTest {

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `updateServerSettings() SHOULD return account not found exception WHEN none present with uuid`() = runTest {
        val accountManager = FakeLegacyAccountDtoManager(accounts = mutableMapOf())
        val testSubject = AccountServerSettingsUpdater(accountManager)

        val result = testSubject.updateServerSettings(
            accountUuid = ACCOUNT_ID_RAW,
            isIncoming = true,
            serverSettings = INCOMING_SERVER_SETTINGS,
            authorizationState = AUTHORIZATION_STATE,
        )

        assertThat(result).isEqualTo(
            AccountUpdaterResult.Failure(
                error = AccountUpdaterFailure.AccountNotFound(ACCOUNT_ID_RAW),
            ),
        )
    }

    @Test
    fun `updateServerSettings() SHOULD return success with updated incoming settings WHEN is incoming`() = runTest {
        val accountManager = FakeLegacyAccountDtoManager(
            accounts = mutableMapOf(ACCOUNT_ID_RAW to createAccount(ACCOUNT_ID_RAW)),
        )
        val updatedIncomingServerSettings = INCOMING_SERVER_SETTINGS.copy(port = 123)
        val updatedAuthorizationState = AuthorizationState("new")
        val testSubject = AccountServerSettingsUpdater(accountManager)

        val result = testSubject.updateServerSettings(
            accountUuid = ACCOUNT_ID_RAW,
            isIncoming = true,
            serverSettings = updatedIncomingServerSettings,
            authorizationState = updatedAuthorizationState,
        )

        assertThat(result).isEqualTo(AccountUpdaterResult.Success(ACCOUNT_ID_RAW))

        val k9Account = accountManager.getAccount(ACCOUNT_ID_RAW)
        assertThat(k9Account).isNotNull().all {
            prop(K9Account::incomingServerSettings).isEqualTo(updatedIncomingServerSettings)
            prop(K9Account::outgoingServerSettings).isEqualTo(OUTGOING_SERVER_SETTINGS)
            prop(K9Account::oAuthState).isEqualTo(updatedAuthorizationState.value)
        }
    }

    @Test
    fun `updateServerSettings() SHOULD return success with updated outgoing settings WHEN is not incoming`() = runTest {
        val accountManager = FakeLegacyAccountDtoManager(
            accounts = mutableMapOf(ACCOUNT_ID_RAW to createAccount(ACCOUNT_ID_RAW)),
        )
        val updatedOutgoingServerSettings = OUTGOING_SERVER_SETTINGS.copy(port = 123)
        val updatedAuthorizationState = AuthorizationState("new")
        val testSubject = AccountServerSettingsUpdater(accountManager)

        val result = testSubject.updateServerSettings(
            accountUuid = ACCOUNT_ID_RAW,
            isIncoming = false,
            serverSettings = updatedOutgoingServerSettings,
            authorizationState = updatedAuthorizationState,
        )

        assertThat(result).isEqualTo(AccountUpdaterResult.Success(ACCOUNT_ID_RAW))

        val k9Account = accountManager.getAccount(ACCOUNT_ID_RAW)
        assertThat(k9Account).isNotNull().all {
            prop(K9Account::incomingServerSettings).isEqualTo(INCOMING_SERVER_SETTINGS)
            prop(K9Account::outgoingServerSettings).isEqualTo(updatedOutgoingServerSettings)
            prop(K9Account::oAuthState).isEqualTo(updatedAuthorizationState.value)
        }
    }

    @Test
    fun `updateServerSettings() SHOULD return unknown error when exception thrown`() = runTest {
        val accountManager = FakeLegacyAccountDtoManager(
            accounts = mutableMapOf(ACCOUNT_ID_RAW to createAccount(ACCOUNT_ID_RAW)),
            isFailureOnSave = true,
        )
        val testSubject = AccountServerSettingsUpdater(accountManager)

        val result = testSubject.updateServerSettings(
            accountUuid = ACCOUNT_ID_RAW,
            isIncoming = true,
            serverSettings = INCOMING_SERVER_SETTINGS,
            authorizationState = AUTHORIZATION_STATE,
        )

        assertThat(result).isInstanceOf<AccountUpdaterResult.Failure>()
            .prop(AccountUpdaterResult.Failure::error).isInstanceOf<AccountUpdaterFailure.UnknownError>()
            .prop(AccountUpdaterFailure.UnknownError::error).isInstanceOf<Exception>()
    }

    private companion object {
        val INCOMING_SERVER_SETTINGS = ServerSettings(
            type = "pop3",
            host = "pop.example.org",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "username",
            password = "password",
            clientCertificateAlias = null,
            extra = emptyMap(),
        )

        val OUTGOING_SERVER_SETTINGS = ServerSettings(
            type = "smtp",
            host = "smtp.example.org",
            port = 587,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "username",
            password = "password",
            clientCertificateAlias = null,
            extra = emptyMap(),
        )

        val AUTHORIZATION_STATE = AuthorizationState("auth state")

        fun createAccount(accountUuid: String): K9Account {
            return K9Account(
                uuid = accountUuid,
            ).apply {
                incomingServerSettings = INCOMING_SERVER_SETTINGS
                outgoingServerSettings = OUTGOING_SERVER_SETTINGS
                oAuthState = AUTHORIZATION_STATE.value
            }
        }
    }
}
