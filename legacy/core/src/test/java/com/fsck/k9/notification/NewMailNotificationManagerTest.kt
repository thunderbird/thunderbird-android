package com.fsck.k9.notification

import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.doesNotContain
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.NotificationMessage
import kotlin.test.assertNotNull
import kotlinx.datetime.Instant
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.BackgroundSync
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.SubTheme
import net.thunderbird.core.preference.notification.NotificationPreference
import net.thunderbird.core.preference.privacy.PrivacySettings
import net.thunderbird.core.testing.TestClock
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing

private const val ACCOUNT_UUID = "00000000-0000-4000-0000-000000000000"
private const val ACCOUNT_NAME = "Personal"
private const val ACCOUNT_COLOR = 0xFF112233L.toInt()
private const val FOLDER_ID = 42L
private const val TIMESTAMP = 23L

class NewMailNotificationManagerTest {
    private val mockedNotificationMessages = mutableListOf<NotificationMessage>()
    private val account = createAccount()
    private val notificationContentCreator = mock<NotificationContentCreator>()
    private val localStoreProvider = createLocalStoreProvider()
    private val clock = TestClock(Instant.fromEpochMilliseconds(TIMESTAMP))
    private val manager = NewMailNotificationManager(
        notificationContentCreator,
        createNotificationRepository(),
        BaseNotificationDataCreator(),
        SingleMessageNotificationDataCreator(),
        SummaryNotificationDataCreator(
            singleMessageNotificationDataCreator = SingleMessageNotificationDataCreator(),
            generalSettingsManager = mock {
                on { getSettings() } doReturn GeneralSettings(
                    backgroundSync = BackgroundSync.ALWAYS,
                    showRecentChanges = true,
                    appTheme = AppTheme.DARK,
                    messageComposeTheme = SubTheme.DARK,
                    isShowCorrespondentNames = true,
                    fixedMessageViewTheme = true,
                    messageViewTheme = SubTheme.DARK,
                    isShowStarredCount = false,
                    isShowUnifiedInbox = false,
                    isShowMessageListStars = false,
                    isShowAnimations = false,
                    shouldShowSetupArchiveFolderDialog = false,
                    isMessageListSenderAboveSubject = false,
                    isShowContactName = false,
                    isShowContactPicture = false,
                    isChangeContactNameColor = false,
                    isColorizeMissingContactPictures = false,
                    isUseBackgroundAsUnreadIndicator = false,
                    isShowComposeButtonOnMessageList = false,
                    isThreadedViewEnabled = false,
                    isUseMessageViewFixedWidthFont = false,
                    isAutoFitWidth = false,
                    notification = NotificationPreference(),
                    privacy = PrivacySettings(),
                )
            },
        ),
        clock,
    )

    @Test
    fun `add first notification`() {
        val message = addMessageToNotificationContentCreator(
            sender = "sender",
            subject = "subject",
            preview = "preview",
            summary = "summary",
            messageUid = "msg-1",
        )

        val result = manager.addNewMailNotification(account, message, silent = false)

        assertNotNull(result)
        assertThat(result.singleNotificationData.first().content).isEqualTo(
            NotificationContent(
                messageReference = createMessageReference("msg-1"),
                sender = "sender",
                subject = "subject",
                preview = "preview",
                summary = "summary",
            ),
        )
        assertThat(result.summaryNotificationData).isNotNull().isInstanceOf<SummarySingleNotificationData>()
        val summaryNotificationData = result.summaryNotificationData as SummarySingleNotificationData
        assertThat(summaryNotificationData.singleNotificationData.isSilent).isFalse()
    }

    @Test
    fun `add second notification`() {
        val messageOne = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Hi Bob",
            preview = "How are you?",
            summary = "Alice Hi Bob",
            messageUid = "msg-1",
        )
        val messageTwo = addMessageToNotificationContentCreator(
            sender = "Zoe",
            subject = "Meeting",
            preview = "We need to talk",
            summary = "Zoe Meeting",
            messageUid = "msg-2",
        )
        manager.addNewMailNotification(account, messageOne, silent = false)
        val timestamp = TIMESTAMP + 1000
        clock.changeTimeTo(Instant.fromEpochMilliseconds(timestamp))

        val result = manager.addNewMailNotification(account, messageTwo, silent = false)

        assertNotNull(result)
        assertThat(result.singleNotificationData.first().content).isEqualTo(
            NotificationContent(
                messageReference = createMessageReference("msg-2"),
                sender = "Zoe",
                subject = "Meeting",
                preview = "We need to talk",
                summary = "Zoe Meeting",
            ),
        )
        assertThat(result.baseNotificationData.newMessagesCount).isEqualTo(2)
        assertThat(result.summaryNotificationData).isNotNull().isInstanceOf<SummaryInboxNotificationData>()
        val summaryNotificationData = result.summaryNotificationData as SummaryInboxNotificationData
        assertThat(summaryNotificationData.content).isEqualTo(listOf("Zoe Meeting", "Alice Hi Bob"))
        assertThat(summaryNotificationData.messageReferences).isEqualTo(
            listOf(
                createMessageReference("msg-2"),
                createMessageReference("msg-1"),
            ),
        )
        assertThat(summaryNotificationData.additionalMessagesCount).isEqualTo(0)
        assertThat(summaryNotificationData.isSilent).isFalse()
    }

    @Test
    fun `add one more notification when already displaying the maximum number of notifications`() {
        addMaximumNumberOfNotifications()
        val message = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Another one",
            preview = "Are you tired of me yet?",
            summary = "Alice Another one",
            messageUid = "msg-x",
        )

        val result = manager.addNewMailNotification(account, message, silent = false)

        assertNotNull(result)
        val notificationId = NotificationIds.getSingleMessageNotificationId(account, index = 0)
        assertThat(result.cancelNotificationIds).isEqualTo(listOf(notificationId))
        assertThat(result.singleNotificationData.first().notificationId).isEqualTo(notificationId)
    }

    @Test
    fun `remove notification when none was added before should return null`() {
        val result = manager.removeNewMailNotifications(account, clearNewMessageState = true) {
            listOf(createMessageReference("any"))
        }

        assertThat(result).isNull()
    }

    @Test
    fun `remove notification with untracked notification ID should return null`() {
        val message = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Another one",
            preview = "Are you tired of me yet?",
            summary = "Alice Another one",
            messageUid = "msg-x",
        )
        manager.addNewMailNotification(account, message, silent = false)

        val result = manager.removeNewMailNotifications(account, clearNewMessageState = true) {
            listOf(createMessageReference("untracked"))
        }

        assertThat(result).isNull()
    }

    @Test
    fun `remove last remaining notification`() {
        val message = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Hello",
            preview = "How are you?",
            summary = "Alice Hello",
            messageUid = "msg-1",
        )
        manager.addNewMailNotification(account, message, silent = false)

        val result = manager.removeNewMailNotifications(account, clearNewMessageState = true) {
            listOf(createMessageReference("msg-1"))
        }

        assertNotNull(result) { data ->
            assertThat(data.cancelNotificationIds).containsExactlyInAnyOrder(
                NotificationIds.getNewMailSummaryNotificationId(account),
                NotificationIds.getSingleMessageNotificationId(account, 0),
            )
            assertThat(data.singleNotificationData).isEmpty()
            assertThat(data.summaryNotificationData).isNull()
        }
    }

    @Test
    fun `remove one of three notifications`() {
        val messageOne = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "One",
            preview = "preview",
            summary = "Alice One",
            messageUid = "msg-1",
        )
        manager.addNewMailNotification(account, messageOne, silent = false)
        val messageTwo = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Two",
            preview = "preview",
            summary = "Alice Two",
            messageUid = "msg-2",
        )
        val dataTwo = manager.addNewMailNotification(account, messageTwo, silent = true)
        assertNotNull(dataTwo)
        val notificationIdTwo = dataTwo.singleNotificationData.first().notificationId
        val messageThree = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Three",
            preview = "preview",
            summary = "Alice Three",
            messageUid = "msg-3",
        )
        manager.addNewMailNotification(account, messageThree, silent = true)

        val result = manager.removeNewMailNotifications(account, clearNewMessageState = true) {
            listOf(createMessageReference("msg-2"))
        }

        assertNotNull(result) { data ->
            assertThat(data.cancelNotificationIds).isEqualTo(listOf(notificationIdTwo))
            assertThat(data.singleNotificationData).isEmpty()
            assertThat(data.baseNotificationData.newMessagesCount).isEqualTo(2)
            assertThat(data.summaryNotificationData).isNotNull().isInstanceOf<SummaryInboxNotificationData>()
            val summaryNotificationData = data.summaryNotificationData as SummaryInboxNotificationData
            assertThat(summaryNotificationData.content).isEqualTo(listOf("Alice Three", "Alice One"))
            assertThat(summaryNotificationData.messageReferences).isEqualTo(
                listOf(
                    createMessageReference("msg-3"),
                    createMessageReference("msg-1"),
                ),
            )
        }
    }

    @Test
    fun `remove notification when additional notifications are available`() {
        val message = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Another one",
            preview = "Are you tired of me yet?",
            summary = "Alice Another one",
            messageUid = "msg-restore",
        )
        manager.addNewMailNotification(account, message, silent = false)
        addMaximumNumberOfNotifications()

        val result = manager.removeNewMailNotifications(account, clearNewMessageState = true) {
            listOf(createMessageReference("msg-1"))
        }

        assertNotNull(result) { data ->
            assertThat(data.cancelNotificationIds).hasSize(1)
            assertThat(data.baseNotificationData.newMessagesCount)
                .isEqualTo(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS)

            val singleNotificationData = data.singleNotificationData.first()
            assertThat(singleNotificationData.notificationId).isEqualTo(data.cancelNotificationIds.first())
            assertThat(singleNotificationData.isSilent).isTrue()
            assertThat(singleNotificationData.content).isEqualTo(
                NotificationContent(
                    messageReference = createMessageReference("msg-restore"),
                    sender = "Alice",
                    subject = "Another one",
                    preview = "Are you tired of me yet?",
                    summary = "Alice Another one",
                ),
            )
        }
    }

    @Test
    fun `restore notifications without persisted notifications`() {
        val result = manager.restoreNewMailNotifications(account)

        assertThat(result).isNull()
    }

    @Test
    fun `restore notifications with single persisted notification`() {
        addNotificationMessage(
            notificationId = 10,
            timestamp = 20L,
            sender = "Sender",
            subject = "Subject",
            summary = "Summary",
            preview = "Preview",
            messageUid = "uid-1",
        )

        val result = manager.restoreNewMailNotifications(account)

        assertNotNull(result) { data ->
            assertThat(data.cancelNotificationIds).isEmpty()
            assertThat(data.baseNotificationData.newMessagesCount).isEqualTo(1)
            assertThat(data.singleNotificationData).hasSize(1)

            val singleNotificationData = data.singleNotificationData.first()
            assertThat(singleNotificationData.notificationId).isEqualTo(10)
            assertThat(singleNotificationData.isSilent).isTrue()
            assertThat(singleNotificationData.addLockScreenNotification).isTrue()
            assertThat(singleNotificationData.content).isEqualTo(
                NotificationContent(
                    messageReference = createMessageReference("uid-1"),
                    sender = "Sender",
                    subject = "Subject",
                    preview = "Preview",
                    summary = "Summary",
                ),
            )

            assertThat(data.summaryNotificationData).isNotNull().isInstanceOf<SummarySingleNotificationData>()
            val summaryNotificationData = data.summaryNotificationData as SummarySingleNotificationData
            assertThat(summaryNotificationData.singleNotificationData.isSilent).isTrue()
            assertThat(summaryNotificationData.singleNotificationData.content).isEqualTo(
                NotificationContent(
                    messageReference = createMessageReference("uid-1"),
                    sender = "Sender",
                    subject = "Subject",
                    preview = "Preview",
                    summary = "Summary",
                ),
            )
        }
    }

    @Test
    fun `restore notifications with one inactive persisted notification`() {
        addMaximumNumberOfNotificationMessages()
        addNotificationMessage(
            notificationId = null,
            timestamp = 1000L,
            sender = "inactive",
            subject = "inactive",
            summary = "inactive",
            preview = "inactive",
            messageUid = "uid-inactive",
        )

        val result = manager.restoreNewMailNotifications(account)

        assertNotNull(result) { data ->
            assertThat(data.cancelNotificationIds).isEmpty()
            assertThat(data.baseNotificationData.newMessagesCount)
                .isEqualTo(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS + 1)
            assertThat(data.singleNotificationData).hasSize(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS)
            assertThat(data.singleNotificationData.map { it.content.sender }).doesNotContain("inactive")

            assertThat(data.summaryNotificationData).isNotNull().isInstanceOf<SummaryInboxNotificationData>()
            val summaryNotificationData = data.summaryNotificationData as SummaryInboxNotificationData
            assertThat(summaryNotificationData.isSilent).isTrue()
        }
    }

    private fun createAccount(): LegacyAccount {
        return LegacyAccount(ACCOUNT_UUID).apply {
            name = ACCOUNT_NAME
            chipColor = ACCOUNT_COLOR
        }
    }

    private fun addMaximumNumberOfNotifications() {
        repeat(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) { index ->
            val message = addMessageToNotificationContentCreator(
                sender = "sender",
                subject = "subject",
                preview = "preview",
                summary = "summary",
                messageUid = "msg-$index",
            )
            manager.addNewMailNotification(account, message, silent = true)
        }
    }

    private fun addMessageToNotificationContentCreator(
        sender: String,
        subject: String,
        preview: String,
        summary: String,
        messageUid: String,
    ): LocalMessage {
        val message = mock<LocalMessage>()

        stubbing(notificationContentCreator) {
            on { createFromMessage(account, message) } doReturn
                NotificationContent(
                    messageReference = createMessageReference(messageUid),
                    sender,
                    subject,
                    preview,
                    summary,
                )
        }

        return message
    }

    private fun addNotificationMessage(
        notificationId: Int?,
        timestamp: Long,
        sender: String,
        subject: String,
        preview: String,
        summary: String,
        messageUid: String,
    ) {
        val message = mock<LocalMessage>()

        val notificationMessage = NotificationMessage(message, notificationId, timestamp)
        mockedNotificationMessages.add(notificationMessage)

        stubbing(notificationContentCreator) {
            on { createFromMessage(account, message) } doReturn
                NotificationContent(
                    messageReference = createMessageReference(messageUid),
                    sender,
                    subject,
                    preview,
                    summary,
                )
        }
    }

    private fun addMaximumNumberOfNotificationMessages() {
        repeat(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) { index ->
            addNotificationMessage(
                notificationId = index,
                timestamp = index.toLong(),
                sender = "irrelevant",
                subject = "irrelevant",
                preview = "irrelevant",
                summary = "irrelevant",
                messageUid = "uid-$index",
            )
        }
    }

    private fun createMessageReference(messageUid: String): MessageReference {
        return MessageReference(ACCOUNT_UUID, FOLDER_ID, messageUid)
    }

    private fun createLocalStoreProvider(): LocalStoreProvider {
        val localStore = createLocalStore()
        return mock {
            on { getInstance(account) } doReturn localStore
        }
    }

    private fun createLocalStore(): LocalStore {
        return mock {
            on { notificationMessages } doAnswer { mockedNotificationMessages.toList() }
        }
    }

    private fun createNotificationRepository(): NotificationRepository {
        val notificationStoreProvider = mock<NotificationStoreProvider> {
            on { getNotificationStore(account) } doReturn mock()
        }
        val messageStoreManager = mock<MessageStoreManager> {
            on { getMessageStore(account) } doReturn mock()
        }

        return NotificationRepository(
            notificationStoreProvider,
            localStoreProvider,
            messageStoreManager,
            notificationContentCreator,
        )
    }
}
