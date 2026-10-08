package com.fsck.k9.account

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import org.junit.Test

class AccountStateLoaderTest {

    @Test
    fun `loadAccountState() SHOULD return null when accountManager returns null`() = runTest {
        val accountManager = FakeLegacyAccountManager()
        val testSubject = AccountStateLoader(accountManager)

        val result = testSubject.loadAccountState(ACCOUNT_ID)

        assertThat(result).isNull()
    }

    @Test
    fun `loadAccountState() SHOULD return account when present in accountManager`() = runTest {
        val account = LegacyAccount(
            id = ACCOUNT_ID,
            name = "name",
            email = "emailAddress",
            profile = ProfileDto(
                id = ACCOUNT_ID,
                name = "name",
                color = -1,
                avatar = AvatarDto(
                    id = ACCOUNT_ID,
                    avatarType = AvatarTypeDto.MONOGRAM,
                    avatarMonogram = "NA",
                    avatarImageUri = null,
                    avatarIconName = null,
                ),
            ),
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
            identities = listOf(Identity()),
            oAuthState = "oAuthState",
        )
        val accounts = mutableMapOf(ACCOUNT_ID to account)
        val accountManager = FakeLegacyAccountManager(accounts = accounts)
        val testSubject = AccountStateLoader(accountManager)

        val result = testSubject.loadAccountState(ACCOUNT_ID)

        assertThat(result).isEqualTo(
            AccountState(
                id = ACCOUNT_ID,
                emailAddress = "emailAddress",
                incomingServerSettings = INCOMING_SERVER_SETTINGS,
                outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
                authorizationState = AuthorizationState("oAuthState"),
            ),
        )
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
    }
}
