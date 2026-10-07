package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto

private const val FOLDER_ID = 42L
private const val TIMESTAMP = 0L

class NotificationDataStoreTest : RobolectricTest() {
    private val accountId = AccountIdFactory.create()
    private val account = createFakeAccount(accountId)
    private val accountManager = FakeAccountManager(mutableMapOf(accountId to account))
    private val notificationIdRegistry = FakeAccountNotificationIdRegistry()
    private val notificationDataStore = NotificationDataStore(accountManager, notificationIdRegistry)

    @Test
    fun testAddNotificationContent() {
        val content = createNotificationContent("1")

        val result = notificationDataStore.addNotification(accountId, content, TIMESTAMP)

        assertNotNull(result)
        assertThat(result.shouldCancelNotification).isFalse()

        val holder = result.notificationHolder

        assertThat(holder).isNotNull()
        assertThat(holder.notificationId).isEqualTo(
            notificationIdRegistry.getOrAllocate(accountId, AccountNotificationKind.SingleMessage, 0),
        )
        assertThat(holder.content).isEqualTo(content)
    }

    @Test
    fun testAddNotificationContentWithReplacingNotification() {
        notificationDataStore.addNotification(accountId, createNotificationContent("1"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("2"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("3"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("4"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("5"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("6"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("7"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("8"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("9"), TIMESTAMP)

        val result = notificationDataStore.addNotification(accountId, createNotificationContent("10"), TIMESTAMP)

        assertNotNull(result)
        assertThat(result.shouldCancelNotification).isTrue()
        assertThat(result.cancelNotificationId).isEqualTo(
            notificationIdRegistry.getOrAllocate(accountId, AccountNotificationKind.SingleMessage, 0),
        )
    }

    @Test
    fun testRemoveNotificationForMessage() {
        val content = createNotificationContent("1")
        notificationDataStore.addNotification(accountId, content, TIMESTAMP)

        val result = notificationDataStore.removeNotifications(accountId) { listOf(content.messageReference) }

        assertNotNull(result) { removeResult ->
            assertThat(removeResult.cancelNotificationIds)
                .containsExactly(
                    notificationIdRegistry.getOrAllocate(accountId, AccountNotificationKind.SingleMessage, 0),
                )
            assertThat(removeResult.notificationHolders).isEmpty()
        }
    }

    @Test
    fun testRemoveNotificationForMessageWithRecreatingNotification() {
        notificationDataStore.addNotification(accountId, createNotificationContent("1"), TIMESTAMP)
        val content = createNotificationContent("2")
        notificationDataStore.addNotification(accountId, content, TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("3"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("4"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("5"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("6"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("7"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("8"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("9"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("10"), TIMESTAMP)
        val latestContent = createNotificationContent("11")
        notificationDataStore.addNotification(accountId, latestContent, TIMESTAMP)

        val result = notificationDataStore.removeNotifications(accountId) { listOf(latestContent.messageReference) }

        assertNotNull(result) { removeResult ->
            assertThat(removeResult.cancelNotificationIds)
                .containsExactly(
                    notificationIdRegistry.getOrAllocate(
                        accountId,
                        AccountNotificationKind.SingleMessage,
                        1,
                    ),
                )
            assertThat(removeResult.notificationHolders).hasSize(1)

            val holder = removeResult.notificationHolders.first()
            assertThat(holder.notificationId).isEqualTo(
                notificationIdRegistry.getOrAllocate(accountId, AccountNotificationKind.SingleMessage, 1),
            )
            assertThat(holder.content).isEqualTo(content)
        }
    }

    @Test
    fun `remove multiple notifications`() {
        repeat(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS + 1) { index ->
            notificationDataStore.addNotification(accountId, createNotificationContent(index.toString()), TIMESTAMP)
        }

        val result = notificationDataStore.removeNotifications(accountId) { it.dropLast(1) }

        assertNotNull(result) { removeResult ->
            assertThat(removeResult.notificationData.newMessagesCount).isEqualTo(1)
            assertThat(removeResult.cancelNotificationIds).hasSize(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS)
        }
    }

    @Test
    fun `remove all notifications`() {
        repeat(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS + 1) { index ->
            notificationDataStore.addNotification(accountId, createNotificationContent(index.toString()), TIMESTAMP)
        }

        val result = notificationDataStore.removeNotifications(accountId) { it }

        assertNotNull(result) { removeResult ->
            assertThat(removeResult.notificationData.newMessagesCount).isEqualTo(0)
            assertThat(removeResult.notificationHolders).hasSize(0)
            assertThat(removeResult.notificationStoreOperations).hasSize(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS + 1)
            for (notificationStoreOperation in removeResult.notificationStoreOperations) {
                assertThat(notificationStoreOperation).isInstanceOf<NotificationStoreOperation.Remove>()
            }
        }
    }

    @Test
    fun testRemoveDoesNotLeakNotificationIds() {
        for (i in 1..MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS + 1) {
            val content = createNotificationContent(i.toString())
            notificationDataStore.addNotification(accountId, content, TIMESTAMP)
            notificationDataStore.removeNotifications(accountId) { listOf(content.messageReference) }
        }
    }

    @Test
    fun testNewMessagesCount() {
        val contentOne = createNotificationContent("1")
        val resultOne = notificationDataStore.addNotification(accountId, contentOne, TIMESTAMP)
        assertNotNull(resultOne)
        assertThat(resultOne.notificationData.newMessagesCount).isEqualTo(1)

        val contentTwo = createNotificationContent("2")
        val resultTwo = notificationDataStore.addNotification(accountId, contentTwo, TIMESTAMP)
        assertNotNull(resultTwo)
        assertThat(resultTwo.notificationData.newMessagesCount).isEqualTo(2)
    }

    @Test
    fun testIsSingleMessageNotification() {
        val resultOne = notificationDataStore.addNotification(accountId, createNotificationContent("1"), TIMESTAMP)
        assertNotNull(resultOne)
        assertThat(resultOne.notificationData.isSingleMessageNotification).isTrue()

        val resultTwo = notificationDataStore.addNotification(accountId, createNotificationContent("2"), TIMESTAMP)
        assertNotNull(resultTwo)
        assertThat(resultTwo.notificationData.isSingleMessageNotification).isFalse()
    }

    @Test
    fun testGetHolderForLatestNotification() {
        val content = createNotificationContent("1")
        val addResult = notificationDataStore.addNotification(accountId, content, TIMESTAMP)

        assertNotNull(addResult)
        assertThat(addResult.notificationData.activeNotifications.first()).isEqualTo(addResult.notificationHolder)
    }

    @Test
    fun `adding notification for message with active notification should update notification`() {
        val content1 = createNotificationContent("1")
        val content2 = createNotificationContent("1")

        val resultOne = notificationDataStore.addNotification(accountId, content1, TIMESTAMP)
        val resultTwo = notificationDataStore.addNotification(accountId, content2, TIMESTAMP)

        assertNotNull(resultOne)
        assertNotNull(resultTwo)
        assertThat(resultTwo.notificationData.activeNotifications).hasSize(1)
        assertThat(resultTwo.notificationData.activeNotifications.first().content).isSameInstanceAs(content2)
        assertThat(resultTwo.notificationStoreOperations).isEmpty()
        with(resultTwo.notificationHolder) {
            assertThat(notificationId).isEqualTo(resultOne.notificationHolder.notificationId)
            assertThat(timestamp).isEqualTo(resultOne.notificationHolder.timestamp)
            assertThat(content).isSameInstanceAs(content2)
        }
        assertThat(resultTwo.shouldCancelNotification).isFalse()
    }

    @Test
    fun `adding notification for message with inactive notification should update notificationData`() {
        notificationDataStore.addNotification(accountId, createNotificationContent("1"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("2"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("3"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("4"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("5"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("6"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("7"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("8"), TIMESTAMP)
        notificationDataStore.addNotification(accountId, createNotificationContent("9"), TIMESTAMP)
        val latestNotificationContent = createNotificationContent("10")
        notificationDataStore.addNotification(accountId, latestNotificationContent, TIMESTAMP)
        val content = createNotificationContent("1")

        val resultOne = notificationDataStore.addNotification(accountId, content, TIMESTAMP)

        assertThat(resultOne).isNull()

        val resultTwo = notificationDataStore.removeNotifications(accountId) {
            listOf(latestNotificationContent.messageReference)
        }

        assertNotNull(resultTwo)
        val notificationHolder = resultTwo.notificationData.activeNotifications.first { notificationHolder ->
            notificationHolder.content.messageReference == content.messageReference
        }
        assertThat(notificationHolder.content).isSameInstanceAs(content)
    }

    private class FakeAccountManager(
        val accounts: MutableMap<AccountId, LegacyAccount> = mutableMapOf(),
    ) : LegacyAccountManager {
        override fun findAll(): List<LegacyAccount> = accounts.values.toList()
        override fun observeAll(): Flow<List<LegacyAccount>> = flowOf(accounts.values.toList())
        override fun findById(accountId: AccountId): LegacyAccount? = accounts[accountId]
        override fun observeById(accountId: AccountId): Flow<LegacyAccount?> = flowOf(accounts[accountId])
        override fun moveAccount(accountId: AccountId, newPosition: Int) = Unit
        override suspend fun update(account: LegacyAccount) { updateSync(account) }
        override fun updateSync(account: LegacyAccount) { accounts[account.id] = account }
    }

    private class FakeAccountNotificationIdRegistry : AccountNotificationIdRegistry {
        private val notificationIds = mutableMapOf<Triple<AccountId, AccountNotificationKind, Int?>, Int>()

        override fun getOrAllocate(accountId: AccountId, kind: AccountNotificationKind): Int =
            getOrAllocate(accountId, kind, null)

        override fun getOrAllocate(accountId: AccountId, kind: AccountNotificationKind, index: Int): Int =
            getOrAllocate(accountId, kind, index as Int?)

        override fun getAllNewMailNotificationIds(accountId: AccountId): List<Int> =
            buildList {
                repeat(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) { index ->
                    add(getOrAllocate(accountId, AccountNotificationKind.SingleMessage, index))
                }
                add(getOrAllocate(accountId, AccountNotificationKind.NewMailSummary))
            }

        private fun getOrAllocate(accountId: AccountId, kind: AccountNotificationKind, index: Int?): Int =
            notificationIds.getOrPut(Triple(accountId, kind, index)) {
                1000 + notificationIds.size
            }
    }

    private fun createMessageReference(uid: String): MessageReference {
        return MessageReference(accountId, FOLDER_ID, uid)
    }

    private fun createNotificationContent(uid: String): NotificationContent {
        val messageReference = createMessageReference(uid)
        return createNotificationContent(messageReference)
    }

    private fun createNotificationContent(messageReference: MessageReference): NotificationContent {
        return NotificationContent(
            messageReference = messageReference,
            sender = Address("irrelevant", "irrelevant"),
            subject = "irrelevant",
            preview = "irrelevant",
            summary = "irrelevant",
        )
    }

    private companion object {
        fun createFakeAccount(id: AccountId): LegacyAccount {
            return LegacyAccount(
                id = id,
                name = "Test Account",
                email = "user@example.com",
                profile = ProfileDto(
                    id = id,
                    name = "Test Account",
                    color = -1,
                    avatar = AvatarDto(
                        id = id,
                        avatarType = AvatarTypeDto.MONOGRAM,
                        avatarMonogram = "TA",
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
                identities = listOf(Identity(email = "user@example.com")),
            )
        }
    }
}
