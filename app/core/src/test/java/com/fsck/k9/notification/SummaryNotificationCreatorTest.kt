package com.fsck.k9.notification

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.K9.NotificationQuickDelete
import com.fsck.k9.RobolectricTest
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.testing.MockHelper.mockBuilder
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing

private const val NEW_MESSAGE_COUNT = 2
private const val ACCOUNT_NAME = "accountName"
private const val ACCOUNT_NUMBER = 3
private const val ACCOUNT_COLOR = 0xABCDEF
private const val SUMMARY = "summary"
private const val PREVIEW = "preview"
private const val SUBJECT = "subject"
private const val SENDER = "sender"
private const val SUMMARY_2 = "summary2"
private const val PREVIEW_2 = "preview2"
private const val SUBJECT_2 = "subject2"
private const val SENDER_2 = "sender2"
private const val NOTIFICATION_ID = 23

class SummaryNotificationCreatorTest : RobolectricTest() {
    private val notification = mock<Notification>()
    private val bigTextStyle = mockBuilder<NotificationCompat.BigTextStyle>()
    private val resourceProvider: NotificationResourceProvider = TestNotificationResourceProvider()
    private val account = createFakeAccount()
    private val notificationData = createFakeNotificationData(account)
    private val builder = createFakeNotificationBuilder()
    private val builder2 = createFakeNotificationBuilder()
    private val lockScreenNotificationCreator = mock<LockScreenNotificationCreator>()
    private val notificationCreator = createSummaryNotificationCreator(builder, lockScreenNotificationCreator)

    @Test
    fun buildSummaryNotification_withSingleMessageNotification() {
        K9.notificationQuickDeleteBehaviour = NotificationQuickDelete.ALWAYS
        stubbing(notificationData) {
            on { isSingleMessageNotification } doReturn true
        }

        val result = notificationCreator.buildSummaryNotification(account, notificationData, false)

        verify(builder).setSmallIcon(resourceProvider.iconNewMail)
        verify(builder).color = ACCOUNT_COLOR
        verify(builder).setAutoCancel(true)
        verify(builder).setTicker(SUMMARY)
        verify(builder).setContentText(SUBJECT)
        verify(builder).setContentTitle(SENDER)
        verify(builder).setStyle(bigTextStyle)
        verify(bigTextStyle).bigText(PREVIEW)
        verify(builder).addAction(resourceProvider.iconReply, "Reply", null)
        verify(builder).addAction(resourceProvider.iconMarkAsRead, "Mark Read", null)
        verify(builder).addAction(resourceProvider.iconDelete, "Delete", null)
        verify(lockScreenNotificationCreator).configureLockScreenNotification(builder, notificationData)
        assertThat(result).isEqualTo(notification)
    }

    @Test
    fun buildSummaryNotification_withMultiMessageNotification() {
        K9.notificationQuickDeleteBehaviour = NotificationQuickDelete.ALWAYS
        stubbing(notificationData) {
            on { isSingleMessageNotification } doReturn false
        }

        val result = notificationCreator.buildSummaryNotification(account, notificationData, false)

        verify(builder).setSmallIcon(resourceProvider.iconNewMail)
        verify(builder).color = ACCOUNT_COLOR
        verify(builder).setAutoCancel(true)
        verify(builder).setTicker(SUMMARY)
        verify(builder).setContentTitle("$NEW_MESSAGE_COUNT new messages")
        verify(builder).setSubText(ACCOUNT_NAME)
        verify(builder).setGroup("newMailNotifications-$ACCOUNT_NUMBER")
        verify(builder).setGroupSummary(true)
        verify(builder).setStyle(notificationCreator.inboxStyle)
        verify(notificationCreator.inboxStyle).setBigContentTitle("$NEW_MESSAGE_COUNT new messages")
        verify(notificationCreator.inboxStyle).setSummaryText(ACCOUNT_NAME)
        verify(notificationCreator.inboxStyle).addLine(SUMMARY)
        verify(notificationCreator.inboxStyle).addLine(SUMMARY_2)
        verify(builder).addAction(resourceProvider.iconMarkAsRead, "Mark Read", null)
        verify(builder).addAction(resourceProvider.iconDelete, "Delete", null)
        verify(lockScreenNotificationCreator).configureLockScreenNotification(builder, notificationData)
        assertThat(result).isEqualTo(notification)
    }

    @Test
    fun buildSummaryNotification_withAdditionalMessages() {
        K9.notificationQuickDeleteBehaviour = NotificationQuickDelete.ALWAYS
        stubbing(notificationData) {
            on { isSingleMessageNotification } doReturn false
            on { hasSummaryOverflowMessages() } doReturn true
            on { getSummaryOverflowMessagesCount() } doReturn 23
        }

        notificationCreator.buildSummaryNotification(account, notificationData, false)

        verify(notificationCreator.inboxStyle).setSummaryText("+ 23 more on $ACCOUNT_NAME")
    }

    @Test
    fun buildSummaryNotification_withoutDeleteAllAction() {
        K9.notificationQuickDeleteBehaviour = NotificationQuickDelete.NEVER
        stubbing(notificationData) {
            on { isSingleMessageNotification } doReturn false
        }

        notificationCreator.buildSummaryNotification(account, notificationData, false)

        verify(builder, never()).addAction(resourceProvider.iconDelete, "Delete", null)
    }

    @Test
    @Throws(Exception::class)
    fun buildSummaryNotification_withoutDeleteAction() {
        K9.notificationQuickDeleteBehaviour = NotificationQuickDelete.NEVER
        stubbing(notificationData) {
            on { isSingleMessageNotification } doReturn true
        }

        notificationCreator.buildSummaryNotification(account, notificationData, false)

        verify(builder, never()).addAction(resourceProvider.iconDelete, "Delete", null)
    }

    private fun createFakeNotificationBuilder(): NotificationCompat.Builder {
        return mockBuilder {
            on { build() } doReturn notification
        }
    }

    private fun createFakeAccount(): Account {
        return mock {
            on { chipColor } doReturn ACCOUNT_COLOR
            on { accountNumber } doReturn ACCOUNT_NUMBER
            on { notificationSetting } doReturn mock()
        }
    }

    private fun createFakeNotificationData(account: Account): NotificationData {
        val messageReference = MessageReference("irrelevant", 1, "irrelevant")
        val content = NotificationContent(messageReference, SENDER, SUBJECT, PREVIEW, SUMMARY)
        val content2 = NotificationContent(messageReference, SENDER_2, SUBJECT_2, PREVIEW_2, SUMMARY_2)
        return mock {
            on { newMessagesCount } doReturn NEW_MESSAGE_COUNT
            on { this.account } doReturn account
            on { getContentForSummaryNotification() } doReturn listOf(content, content2)
            on { holderForLatestNotification } doReturn NotificationHolder(NOTIFICATION_ID, content)
        }
    }

    private fun createSummaryNotificationCreator(
        builder: NotificationCompat.Builder,
        lockScreenNotificationCreator: LockScreenNotificationCreator
    ): TestSummaryNotificationCreator {
        val notificationHelper = createFakeNotificationHelper(builder)
        val singleMessageNotificationCreator = TestSingleMessageNotificationCreator(
            notificationHelper = notificationHelper,
            actionCreator = mock(),
            resourceProvider = resourceProvider,
            lockScreenNotificationCreator = mock()
        )

        return TestSummaryNotificationCreator(
            notificationHelper = notificationHelper,
            actionCreator = mock(),
            lockScreenNotificationCreator = lockScreenNotificationCreator,
            singleMessageNotificationCreator = singleMessageNotificationCreator,
            resourceProvider = resourceProvider
        )
    }

    private fun createFakeNotificationHelper(builder: NotificationCompat.Builder): NotificationHelper {
        return mock {
            on { getContext() } doReturn ApplicationProvider.getApplicationContext()
            on { getAccountName(any()) } doReturn ACCOUNT_NAME
            on { createNotificationBuilder(any(), any()) } doReturn builder doReturn builder2 doAnswer {
                throw AssertionError("createNotificationBuilder() invoked more than twice")
            }
        }
    }

    internal class TestSummaryNotificationCreator(
        notificationHelper: NotificationHelper,
        actionCreator: NotificationActionCreator,
        lockScreenNotificationCreator: LockScreenNotificationCreator,
        singleMessageNotificationCreator: SingleMessageNotificationCreator,
        resourceProvider: NotificationResourceProvider
    ) : SummaryNotificationCreator(
        notificationHelper,
        actionCreator,
        lockScreenNotificationCreator,
        singleMessageNotificationCreator,
        resourceProvider
    ) {
        val inboxStyle = mockBuilder<NotificationCompat.InboxStyle>()

        override fun createInboxStyle(builder: NotificationCompat.Builder?): NotificationCompat.InboxStyle {
            return inboxStyle
        }
    }

    internal inner class TestSingleMessageNotificationCreator(
        notificationHelper: NotificationHelper,
        actionCreator: NotificationActionCreator,
        resourceProvider: NotificationResourceProvider,
        lockScreenNotificationCreator: LockScreenNotificationCreator
    ) : SingleMessageNotificationCreator(
        notificationHelper,
        actionCreator,
        resourceProvider,
        lockScreenNotificationCreator
    ) {
        override fun createBigTextStyle(builder: NotificationCompat.Builder?): NotificationCompat.BigTextStyle {
            return bigTextStyle
        }
    }
}
