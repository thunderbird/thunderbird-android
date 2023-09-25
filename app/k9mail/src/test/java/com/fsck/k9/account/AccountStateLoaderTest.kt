package com.fsck.k9.account

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountStateLoaderTest {

    @Test
    fun `loadAccountState() should return null when accountManager returns null`() = runTest {
        val accountManager = FakeAccountManager()
        val accountLoader = AccountStateLoader(accountManager)

        val result = accountLoader.loadAccountState("accountUuid")

        assertThat(result).isNull()
    }

    @Test
    fun `loadAccountState() should return account when present in accountManager`() = runTest {
        val accounts = mapOf(
            "accountUuid" to Account(uuid = "accountUuid").apply {
                identities = mutableListOf(Identity())
                email = "emailAddress"
                incomingServerSettings = INCOMING_SERVER_SETTINGS
                outgoingServerSettings = OUTGOING_SERVER_SETTINGS
                oAuthState = "oAuthState"
            },
        )
        val accountManager = FakeAccountManager(accounts = accounts)
        val accountLoader = AccountStateLoader(accountManager)

        val result = accountLoader.loadAccountState("accountUuid")

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
