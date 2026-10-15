package com.fsck.k9.backends

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapClientInfo
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import org.junit.Test

class AccountImapStoreConfigTest {
    private val account = createAccount(AccountIdFactory.create())
    private val accountManager = FakeLegacyAccountManager(listOf(account))

    private val testSubject = AccountImapStoreConfig(
        accountManager = accountManager,
        accountId = account.id,
        clientInfo = ImapClientInfo(appName = "App", appVersion = "1.0"),
    )

    @Test
    fun `should use settings changed after the config was created`() {
        // Arrange
        assertThat(testSubject.isSubscribedFoldersOnly()).isFalse()
        assertThat(testSubject.isExpungeImmediately()).isFalse()

        // Act
        accountManager.updateSync(account.id) {
            it.copy(isSubscribedFoldersOnly = true, expungePolicy = Expunge.EXPUNGE_IMMEDIATELY)
        }

        // Assert
        assertThat(testSubject.isSubscribedFoldersOnly()).isTrue()
        assertThat(testSubject.isExpungeImmediately()).isTrue()
    }

    @Test
    fun `should return false for a removed account`() {
        val testSubject = AccountImapStoreConfig(
            accountManager = FakeLegacyAccountManager(),
            accountId = account.id,
            clientInfo = ImapClientInfo(appName = "App", appVersion = "1.0"),
        )

        assertThat(testSubject.isSubscribedFoldersOnly()).isFalse()
        assertThat(testSubject.isExpungeImmediately()).isFalse()
    }

    private fun createAccount(accountId: AccountId): LegacyAccount {
        val serverSettings = ServerSettings(
            type = "imap",
            host = "imap.example.org",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "username",
            password = "password",
            clientCertificateAlias = null,
        )
        return LegacyAccount(
            id = accountId,
            name = "Account",
            email = "user@example.com",
            profile = ProfileDto(
                id = accountId,
                name = "Account",
                color = -1,
                avatar = AvatarDto(
                    accountId = accountId,
                    avatarType = AvatarTypeDto.MONOGRAM,
                    avatarMonogram = "AC",
                    avatarImageUri = null,
                    avatarIconName = null,
                ),
            ),
            incomingServerSettings = serverSettings,
            outgoingServerSettings = serverSettings.copy(type = "smtp", port = 587),
            identities = listOf(Identity(email = "user@example.com")),
            isSubscribedFoldersOnly = false,
            expungePolicy = Expunge.EXPUNGE_MANUALLY,
        )
    }
}
