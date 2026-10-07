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
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.legacy.logging.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import org.junit.Before
import org.junit.Test

class AccountServerSettingsUpdaterTest {

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `updateServerSettings() SHOULD return account not found exception WHEN none present with uuid`() = runTest {
        val accountManager = FakeLegacyAccountManager(accounts = mutableMapOf())
        val testSubject = AccountServerSettingsUpdater(accountManager)

        val result = testSubject.updateServerSettings(
            accountId = ACCOUNT_ID,
            isIncoming = true,
            serverSettings = INCOMING_SERVER_SETTINGS,
            authorizationState = AUTHORIZATION_STATE,
        )

        assertThat(result).isEqualTo(
            AccountUpdaterResult.Failure(
                error = AccountUpdaterFailure.AccountNotFound(ACCOUNT_ID),
            ),
        )
    }

    @Test
    fun `updateServerSettings() SHOULD return success with updated incoming settings WHEN is incoming`() = runTest {
        val accountManager = FakeLegacyAccountManager(
            accounts = mutableMapOf(ACCOUNT_ID to createAccount(ACCOUNT_ID)),
        )
        val updatedIncomingServerSettings = INCOMING_SERVER_SETTINGS.copy(port = 123)
        val updatedAuthorizationState = AuthorizationState("new")
        val testSubject = AccountServerSettingsUpdater(accountManager)

        val result = testSubject.updateServerSettings(
            accountId = ACCOUNT_ID,
            isIncoming = true,
            serverSettings = updatedIncomingServerSettings,
            authorizationState = updatedAuthorizationState,
        )

        assertThat(result).isEqualTo(AccountUpdaterResult.Success(ACCOUNT_ID))

        val legacyAccount = accountManager.findById(ACCOUNT_ID)
        assertThat(legacyAccount).isNotNull().all {
            prop(LegacyAccount::incomingServerSettings).isEqualTo(updatedIncomingServerSettings)
            prop(LegacyAccount::outgoingServerSettings).isEqualTo(OUTGOING_SERVER_SETTINGS)
            prop(LegacyAccount::oAuthState).isEqualTo(updatedAuthorizationState.value)
        }
    }

    @Test
    fun `updateServerSettings() SHOULD return success with updated outgoing settings WHEN is not incoming`() = runTest {
        val accountManager = FakeLegacyAccountManager(
            accounts = mutableMapOf(ACCOUNT_ID to createAccount(ACCOUNT_ID)),
        )
        val updatedOutgoingServerSettings = OUTGOING_SERVER_SETTINGS.copy(port = 123)
        val updatedAuthorizationState = AuthorizationState("new")
        val testSubject = AccountServerSettingsUpdater(accountManager)

        val result = testSubject.updateServerSettings(
            accountId = ACCOUNT_ID,
            isIncoming = false,
            serverSettings = updatedOutgoingServerSettings,
            authorizationState = updatedAuthorizationState,
        )

        assertThat(result).isEqualTo(AccountUpdaterResult.Success(ACCOUNT_ID))

        val k9Account = accountManager.findById(ACCOUNT_ID)
        assertThat(k9Account).isNotNull().all {
            prop(LegacyAccount::incomingServerSettings).isEqualTo(INCOMING_SERVER_SETTINGS)
            prop(LegacyAccount::outgoingServerSettings).isEqualTo(updatedOutgoingServerSettings)
            prop(LegacyAccount::oAuthState).isEqualTo(updatedAuthorizationState.value)
        }
    }

    @Test
    fun `updateServerSettings() SHOULD return unknown error when exception thrown`() = runTest {
        val accountManager = FakeLegacyAccountManager(
            accounts = mutableMapOf(ACCOUNT_ID to createAccount(ACCOUNT_ID)),
            isFailureOnSave = true,
        )
        val testSubject = AccountServerSettingsUpdater(accountManager)

        val result = testSubject.updateServerSettings(
            accountId = ACCOUNT_ID,
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

        fun createAccount(accountId: AccountId): LegacyAccount {
            return LegacyAccount(
                id = accountId,
                name = "Account",
                email = "user@example.com",
                profile = ProfileDto(
                    id = accountId,
                    name = "Account",
                    color = -1,
                    avatar = AvatarDto(
                        id = accountId,
                        avatarType = AvatarTypeDto.MONOGRAM,
                        avatarMonogram = "AC",
                        avatarImageUri = null,
                        avatarIconName = null,
                    ),
                ),
                incomingServerSettings = INCOMING_SERVER_SETTINGS,
                outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
                oAuthState = AUTHORIZATION_STATE.value,
                identities = listOf(Identity(email = "user@example.com")),
            )
        }
    }
}
