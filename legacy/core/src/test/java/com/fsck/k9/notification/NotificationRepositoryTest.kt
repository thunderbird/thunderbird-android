package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.fsck.k9.FakeLegacyAccount
import com.fsck.k9.FakePlatformConfigProvider
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.NotificationMessage
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing
import org.mockito.kotlin.verify

private const val FOLDER_ID = 42L
private const val OTHER_EMAIL = "other@example.com"

class NotificationRepositoryTest {
    private val account = FakeLegacyAccount.ACCOUNT
    private val accountId = account.id
    private val accountManager = FakeLegacyAccountManager(listOf(account))
    private val notificationMessageProvider = FakeNotificationMessageProvider()
    private val notificationStoreProvider = FakeNotificationStoreProvider()
    private val notificationStore = notificationStoreProvider.getNotificationStore(accountId)
    private val newMessageStateStore = FakeNewMessageStateStore()
    private val notificationContentCreator = mock<NotificationContentCreator>()

    private val testSubject = NotificationRepository(
        notificationStoreProvider = notificationStoreProvider,
        notificationMessageProvider = notificationMessageProvider,
        newMessageStateStore = newMessageStateStore,
        notificationContentCreator = notificationContentCreator,
        generalSettingsManager = FakeGeneralSettingsManager(
            GeneralSettings(platformConfigProvider = FakePlatformConfigProvider()),
        ),
        notificationDataStore = NotificationDataStore(
            accountManager = accountManager,
            notificationIdRegistry = DefaultAccountNotificationIdRegistry(accountManager),
        ),
        accountManager = accountManager,
    )

    @Test
    fun `restoreNotifications should restore active and inactive notifications`() {
        val activeContent = addNotificationMessage(messageUid = "active", notificationId = 10, timestamp = 20L)
        val inactiveContent = addNotificationMessage(messageUid = "inactive", notificationId = null, timestamp = 30L)

        val result = testSubject.restoreNotifications(accountId)

        assertThat(result).isNotNull().all {
            prop(NotificationData::activeNotifications).containsExactly(
                NotificationHolder(notificationId = 10, timestamp = 20L, content = activeContent),
            )
            prop(NotificationData::inactiveNotifications).containsExactly(
                InactiveNotificationHolder(timestamp = 30L, content = inactiveContent),
            )
        }
    }

    @Test
    fun `restoreNotifications should return null when there are no active notifications`() {
        addNotificationMessage(messageUid = "inactive", notificationId = null, timestamp = 30L)

        val result = testSubject.restoreNotifications(accountId)

        assertThat(result).isNull()
    }

    @Test
    fun `restoreNotifications should only restore notifications once`() {
        addNotificationMessage(messageUid = "active", notificationId = 10, timestamp = 20L)
        testSubject.restoreNotifications(accountId)

        val result = testSubject.restoreNotifications(accountId)

        assertThat(result).isNull()
    }

    @Test
    fun `restoreNotifications should return null when the account does not exist`() {
        addNotificationMessage(messageUid = "active", notificationId = 10, timestamp = 20L)
        accountManager.remove(accountId)

        val result = testSubject.restoreNotifications(accountId)

        assertThat(result).isNull()
    }

    @Test
    fun `restoreNotifications should tell the content creator whether a message was sent by the user`() {
        val ownMessage = createMessage(from = FakeLegacyAccount.ACCOUNT_EMAIL)
        val otherMessage = createMessage(from = OTHER_EMAIL)
        notificationMessageProvider.add(accountId, NotificationMessage(ownMessage, notificationId = 1, timestamp = 1L))
        notificationMessageProvider.add(
            accountId,
            NotificationMessage(otherMessage, notificationId = null, timestamp = 2L),
        )
        stubbing(notificationContentCreator) {
            on { createFromMessage(any(), any()) } doReturn createNotificationContent("irrelevant")
        }

        testSubject.restoreNotifications(accountId)

        verify(notificationContentCreator).createFromMessage(ownMessage, isFromSelf = true)
        verify(notificationContentCreator).createFromMessage(otherMessage, isFromSelf = false)
    }

    @Test
    fun `addNotification should persist the notification and mark the message as new`() {
        val content = createNotificationContent(messageUid = "new")

        val result = testSubject.addNotification(accountId, content, timestamp = 40L)

        val notificationId = assertNotNull(result).notificationHolder.notificationId
        assertThat(notificationStore.operations).containsExactly(
            NotificationStoreOperation.Add(content.messageReference, notificationId, timestamp = 40L),
        )
        assertThat(newMessageStateStore.newMessageStates).containsOnly(content.messageReference to true)
    }

    @Test
    fun `removeNotifications should keep the new message state when asked to`() {
        val content = addNotificationMessage(messageUid = "active", notificationId = 10, timestamp = 20L)

        val result = testSubject.removeNotifications(accountId, clearNewMessageState = false) { it }

        assertThat(result).isNotNull()
        assertThat(notificationStore.operations).containsExactly(
            NotificationStoreOperation.Remove(content.messageReference),
        )
        assertThat(newMessageStateStore.newMessageStates).isEmpty()
    }

    @Test
    fun `clearNotifications should clear the notification store and the new message state`() {
        testSubject.clearNotifications(accountId, clearNewMessageState = true)

        assertThat(notificationStore.isCleared).isTrue()
        assertThat(newMessageStateStore.clearedAccountIds).containsExactly(accountId)
    }

    @Test
    fun `clearNotifications should keep the new message state when asked to`() {
        testSubject.clearNotifications(accountId, clearNewMessageState = false)

        assertThat(notificationStore.isCleared).isTrue()
        assertThat(newMessageStateStore.clearedAccountIds).isEmpty()
    }

    private fun addNotificationMessage(messageUid: String, notificationId: Int?, timestamp: Long): NotificationContent {
        val message = createMessage(from = OTHER_EMAIL)
        val content = createNotificationContent(messageUid)
        stubbing(notificationContentCreator) {
            on { createFromMessage(eq(message), any()) } doReturn content
        }
        notificationMessageProvider.add(accountId, NotificationMessage(message, notificationId, timestamp))

        return content
    }

    private fun createMessage(from: String): LocalMessage {
        return mock {
            on { this.from } doReturn arrayOf(Address(from))
        }
    }

    private fun createNotificationContent(messageUid: String): NotificationContent {
        return NotificationContent(
            messageReference = MessageReference(accountId, FOLDER_ID, messageUid),
            sender = Address(OTHER_EMAIL),
            subject = "subject",
            preview = "preview",
            summary = "summary",
        )
    }

    private class FakeGeneralSettingsManager(generalSettings: GeneralSettings) : GeneralSettingsManager {
        private val settings = MutableStateFlow(generalSettings)

        @Deprecated("Use PreferenceManager<GeneralSettings>.getConfig() instead")
        override fun getSettings(): GeneralSettings = settings.value

        @Deprecated("Use PreferenceManager<GeneralSettings>.getConfigFlow() instead")
        override fun getSettingsFlow(): Flow<GeneralSettings> = settings

        override fun save(config: GeneralSettings) {
            settings.value = config
        }

        override fun getConfig(): GeneralSettings = settings.value

        override fun getConfigFlow(): Flow<GeneralSettings> = settings
    }
}
