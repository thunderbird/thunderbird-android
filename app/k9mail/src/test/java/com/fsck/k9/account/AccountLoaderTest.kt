package com.fsck.k9.account

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.Identity
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.fsck.k9.Account as K9Account

class AccountLoaderTest {

    @Test
    fun `loadAccount() should return null when accountManager returns null`() = runTest {
        val accountManager = FakeAccountManager()
        val accountLoader = AccountLoader(accountManager)

        val result = accountLoader.loadAccount("accountUuid")

        assertThat(result).isNull()
    }

    @Test
    fun `loadAccount() should return account when present in accountManager`() = runTest {
        val accounts = mapOf(
            "accountUuid" to K9Account(
                uuid = "accountUuid",
            ).also {
                it.identities = mutableListOf(Identity())
                it.email = "emailAddress"
                it.incomingServerSettings = INCOMING_SERVER_SETTINGS
                it.outgoingServerSettings = OUTGOING_SERVER_SETTINGS
                it.oAuthState = "oAuthState"
            },
        )
        val accountManager = FakeAccountManager(accounts = accounts)
        val accountLoader = AccountLoader(accountManager)

        val result = accountLoader.loadAccount("accountUuid")

        assertThat(result).isEqualTo(
            AccountState(
                uuid = "accountUuid",
                emailAddress = "emailAddress",
                incomingServerSettings = INCOMING_SERVER_SETTINGS,
                outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
                authorizationState = AuthorizationState("oAuthState"),
            ),
        )
    }

    private companion object {
        val INCOMING_SERVER_SETTINGS = ServerSettings(
            type = "imap",
            host = "imap.example.org",
            port = 143,
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
    }
}
