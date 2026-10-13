package com.fsck.k9.account

import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterResult
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.legacy.logging.Log
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.core.android.account.LegacyAccountDto
import org.junit.Test

class AccountAuthorizationStateUpdaterTest {
    @Test
    fun `updating authorization state preserves account profile settings and sync metadata`() = runTest {
        val account = createAccount()
        val manager = FakeLegacyAccountDtoManager(accounts = mutableMapOf(ACCOUNT_ID to account))
        val testSubject = AccountAuthorizationStateUpdater(manager)

        val result = testSubject.updateAuthorizationState(
            accountId = ACCOUNT_ID,
            expectedAuthorizationState = OLD_STATE,
            newAuthorizationState = NEW_STATE,
        )

        assertThat(result).isEqualTo(AccountUpdaterResult.Success(ACCOUNT_ID))
        val savedAccount = manager.getById(ACCOUNT_ID)!!
        assertThat(savedAccount.oAuthState).isEqualTo(NEW_STATE.value)
        assertThat(savedAccount.name).isEqualTo("Existing account name")
        assertThat(savedAccount.incomingServerSettings).isEqualTo(GRAPH_SETTINGS)
        assertThat(savedAccount.outgoingServerSettings).isEqualTo(SMTP_SETTINGS)
        assertThat(savedAccount.inboxFolderId).isEqualTo(17L)
        assertThat(savedAccount.lastSyncTime).isEqualTo(1_234L)
    }

    @Test
    fun `stale authorization state is not overwritten`() = runTest {
        val account = createAccount()
        val manager = FakeLegacyAccountDtoManager(accounts = mutableMapOf(ACCOUNT_ID to account))
        val testSubject = AccountAuthorizationStateUpdater(manager)

        val result = testSubject.updateAuthorizationState(
            accountId = ACCOUNT_ID,
            expectedAuthorizationState = AuthorizationState("stale"),
            newAuthorizationState = NEW_STATE,
        )

        assertThat(result).isInstanceOf<AccountUpdaterResult.Failure>()
        assertThat(manager.getById(ACCOUNT_ID)!!.oAuthState).isEqualTo(OLD_STATE.value)
    }

    @Test
    fun `save failure restores the in-memory account authorization state`() = runTest {
        val account = createAccount()
        val manager = FakeLegacyAccountDtoManager(
            accounts = mutableMapOf(ACCOUNT_ID to account),
            isFailureOnSave = true,
        )
        val testSubject = AccountAuthorizationStateUpdater(manager)
        Log.logger = TestLogger()

        val result = testSubject.updateAuthorizationState(
            accountId = ACCOUNT_ID,
            expectedAuthorizationState = OLD_STATE,
            newAuthorizationState = NEW_STATE,
        )

        assertThat(result).isInstanceOf<AccountUpdaterResult.Failure>()
        assertThat(manager.getById(ACCOUNT_ID)!!.oAuthState).isEqualTo(OLD_STATE.value)
    }

    private fun createAccount() = LegacyAccountDto(ACCOUNT_ID).apply {
        name = "Existing account name"
        incomingServerSettings = GRAPH_SETTINGS
        outgoingServerSettings = SMTP_SETTINGS
        oAuthState = OLD_STATE.value
        inboxFolderId = 17L
        lastSyncTime = 1_234L
    }

    private companion object {
        val ACCOUNT_ID = AccountIdFactory.create()
        val OLD_STATE = AuthorizationState("serialized-current-state")
        val NEW_STATE = AuthorizationState("serialized-new-state")
        val GRAPH_SETTINGS = ServerSettings(
            type = Protocols.GRAPH,
            host = "graph.microsoft.com",
            port = 443,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.XOAUTH2,
            username = "user@example.com",
            password = null,
            clientCertificateAlias = null,
        )
        val SMTP_SETTINGS = ServerSettings(
            type = "smtp",
            host = "smtp.example.com",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user@example.com",
            password = "password",
            clientCertificateAlias = null,
        )
    }
}
