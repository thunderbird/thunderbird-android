package net.thunderbird.app.common.account.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import org.junit.Test

class LegacyInMemoryAccountCacheTest {

    private val testSubject = LegacyInMemoryAccountCache()

    @Test
    fun `findAll should return empty list initially`() {
        val result = testSubject.findAll()

        assertThat(result).isEmpty()
    }

    @Test
    fun `findById should return null when account is not cached`() {
        val accountId = AccountIdFactory.create()

        val result = testSubject.findById(accountId)

        assertThat(result).isNull()
    }

    @Test
    fun `update should add new account and update observeAll`() = runTest {
        val account = createFakeAccount(name = "Account 1")

        testSubject.observeAll().test {
            assertThat(awaitItem()).isEmpty()

            testSubject.update(account)

            assertThat(awaitItem()).containsExactly(account)
            assertThat(testSubject.findAll()).containsExactly(account)
            assertThat(testSubject.findById(account.id)).isEqualTo(account)
        }
    }

    @Test
    fun `update should modify existing account when account with same ID is updated`() = runTest {
        val initialAccount = createFakeAccount(name = "Original Name")
        testSubject.update(initialAccount)

        val updatedAccount = initialAccount.copy(name = "Updated Name")
        testSubject.update(updatedAccount)

        assertThat(testSubject.findAll()).containsExactly(updatedAccount)
        assertThat(testSubject.findById(initialAccount.id)).isEqualTo(updatedAccount)
    }

    @Test
    fun `updateAll should replace all cached accounts and emit new list`() = runTest {
        val account1 = createFakeAccount(name = "Account 1")
        val account2 = createFakeAccount(name = "Account 2")
        val account3 = createFakeAccount(name = "Account 3")

        testSubject.update(account1)

        testSubject.updateAll(listOf(account2, account3))

        assertThat(testSubject.findAll()).containsExactly(account2, account3)
        assertThat(testSubject.findById(account1.id)).isNull()
        assertThat(testSubject.findById(account2.id)).isEqualTo(account2)
        assertThat(testSubject.findById(account3.id)).isEqualTo(account3)
    }

    @Test
    fun `delete should remove account from cache and emit updated list`() = runTest {
        val account1 = createFakeAccount(name = "Account 1")
        val account2 = createFakeAccount(name = "Account 2")

        testSubject.updateAll(listOf(account1, account2))

        testSubject.delete(account1.id)

        assertThat(testSubject.findAll()).containsExactly(account2)
        assertThat(testSubject.findById(account1.id)).isNull()
        assertThat(testSubject.findById(account2.id)).isEqualTo(account2)
    }

    @Test
    fun `deleteAll should clear all cached accounts and emit empty list`() = runTest {
        val account1 = createFakeAccount(name = "Account 1")
        val account2 = createFakeAccount(name = "Account 2")

        testSubject.updateAll(listOf(account1, account2))

        testSubject.deleteAll()

        assertThat(testSubject.findAll()).isEmpty()
        assertThat(testSubject.findById(account1.id)).isNull()
        assertThat(testSubject.findById(account2.id)).isNull()
    }

    private companion object Companion {
        fun createFakeAccount(
            id: AccountId = AccountIdFactory.create(),
            name: String = "Test",
        ): LegacyAccount {
            return LegacyAccount(
                id = id,
                name = name,
                email = "user@example.com",
                profile = ProfileDto(
                    id = id,
                    name = name,
                    color = -1,
                    avatar = AvatarDto(
                        id = id,
                        avatarType = AvatarTypeDto.MONOGRAM,
                        avatarMonogram = "NA",
                        avatarImageUri = null,
                        avatarIconName = null,
                    ),
                ),
                incomingServerSettings = ServerSettings(
                    type = "imap",
                    host = "host",
                    port = 993,
                    connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authenticationType = AuthType.PLAIN,
                    username = "user",
                    password = "pass",
                    clientCertificateAlias = null,
                ),
                outgoingServerSettings = ServerSettings(
                    type = "smtp",
                    host = "host",
                    port = 465,
                    connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authenticationType = AuthType.PLAIN,
                    username = "user",
                    password = "pass",
                    clientCertificateAlias = null,
                ),
                identities = emptyList(),
            )
        }
    }
}
