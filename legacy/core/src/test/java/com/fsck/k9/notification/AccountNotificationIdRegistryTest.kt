package com.fsck.k9.notification

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import org.junit.Test

class AccountNotificationIdRegistryTest : RobolectricTest() {
    private val accountOne = createAccount(uuid = "00000000-0000-4000-0000-000000000001", accountNumber = 4)
    private val accountTwo = createAccount(uuid = "00000000-0000-4000-0000-000000000002", accountNumber = 5)
    private val accountManager = FakeLegacyAccountManager(listOf(accountOne, accountTwo))
    private val testSubject = DefaultAccountNotificationIdRegistry(accountManager)

    @Test
    fun `allocates stable distinct ids for indexed single message notifications`() {
        val firstIndexId = testSubject.getOrAllocate(accountOne.id, AccountNotificationKind.SingleMessage, 0)
        val secondIndexId = testSubject.getOrAllocate(accountOne.id, AccountNotificationKind.SingleMessage, 1)

        assertThat(testSubject.getOrAllocate(accountOne.id, AccountNotificationKind.SingleMessage, 0))
            .isEqualTo(firstIndexId)
        assertThat(firstIndexId).isNotEqualTo(secondIndexId)
    }

    @Test
    fun `allocates notification ids independently per account`() {
        val accountOneId = testSubject.getOrAllocate(accountOne.id, AccountNotificationKind.Sync)
        val accountTwoId = testSubject.getOrAllocate(accountTwo.id, AccountNotificationKind.Sync)

        assertThat(accountOneId).isEqualTo(NotificationIds.getFetchingMailNotificationId(accountOne.accountNumber))
        assertThat(accountTwoId).isEqualTo(NotificationIds.getFetchingMailNotificationId(accountTwo.accountNumber))
        assertThat(accountOneId).isNotEqualTo(accountTwoId)
    }

    @Test
    fun `uses current account number when it changes after initial allocation`() {
        // Arrange
        val initialId = testSubject.getOrAllocate(accountOne.id, AccountNotificationKind.Sync)
        accountManager.replace(accountOne.copy(accountNumber = 6))

        // Act
        val updatedId = testSubject.getOrAllocate(accountOne.id, AccountNotificationKind.Sync)

        // Assert
        assertThat(initialId).isEqualTo(NotificationIds.getFetchingMailNotificationId(4))
        assertThat(updatedId).isEqualTo(NotificationIds.getFetchingMailNotificationId(6))
    }

    @Test
    fun `returns all single message and summary notification ids for account`() {
        val ids = testSubject.getAllNewMailNotificationIds(accountOne.id)

        assertThat(ids).isEqualTo(
            NotificationIds.getAllMessageNotificationIds(accountOne.accountNumber),
        )
    }

    @Test
    fun `all new mail ids differ between accounts`() {
        val firstAccountIds = testSubject.getAllNewMailNotificationIds(accountOne.id)
        val secondAccountIds = testSubject.getAllNewMailNotificationIds(accountTwo.id)

        assertThat(firstAccountIds.intersect(secondAccountIds.toSet())).isEqualTo(emptySet())
    }

    @Test
    fun `single message notification requires an index`() {
        assertThat(
            runCatching {
                testSubject.getOrAllocate(accountOne.id, AccountNotificationKind.SingleMessage)
            }.isFailure,
        )
            .isTrue()
    }

    @Test
    fun `rejects indexes outside available single message slots`() {
        assertThat(
            runCatching {
                testSubject.getOrAllocate(
                    accountOne.id,
                    AccountNotificationKind.SingleMessage,
                    MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS,
                )
            }.isFailure,
        ).isTrue()
    }

    @Test
    fun `rejects indexes for notification kinds other than single message`() {
        assertThat(
            runCatching { testSubject.getOrAllocate(accountOne.id, AccountNotificationKind.Sync, 0) }.isFailure,
        )
            .isTrue()
    }

    private fun createAccount(uuid: String, accountNumber: Int): LegacyAccount {
        val id = AccountIdFactory.of(uuid)
        return LegacyAccount(
            id = id,
            name = "Account",
            email = "irrelevant@example.com",
            profile = ProfileDto(
                id = id,
                name = "Account",
                color = 0,
                avatar = AvatarDto(
                    id = id,
                    avatarType = AvatarTypeDto.MONOGRAM,
                    avatarMonogram = null,
                    avatarImageUri = null,
                    avatarIconName = null,
                ),
            ),
            incomingServerSettings = createServerSettings(Protocols.IMAP, 993),
            outgoingServerSettings = createServerSettings("smtp", 587),
            identities = emptyList(),
            accountNumber = accountNumber,
        )
    }

    private fun createServerSettings(type: String, port: Int) = ServerSettings(
        type = type,
        host = "example.com",
        port = port,
        connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
        authenticationType = AuthType.PLAIN,
        username = "irrelevant",
        password = "irrelevant",
        clientCertificateAlias = null,
    )

    private class FakeLegacyAccountManager(
        accounts: List<LegacyAccount>,
    ) : LegacyAccountManager {
        private val accounts = accounts.toMutableList()

        fun replace(account: LegacyAccount) {
            val index = accounts.indexOfFirst { it.id == account.id }
            accounts[index] = account
        }

        override fun observeAll(): Flow<List<LegacyAccount>> = flowOf(accounts)
        override fun observeById(accountId: AccountId): Flow<LegacyAccount?> =
            flowOf(findById(accountId))

        override fun findById(accountId: AccountId): LegacyAccount? =
            accounts.firstOrNull { it.id == accountId }

        override fun moveAccount(accountId: AccountId, newPosition: Int) = error("Not used in this test")
        override suspend fun update(account: LegacyAccount) = error("Not used in this test")
        override fun updateSync(account: LegacyAccount) = error("Not used in this test")
        override fun findAll(): List<LegacyAccount> = accounts
    }
}
