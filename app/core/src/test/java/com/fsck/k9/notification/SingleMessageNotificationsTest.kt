package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.K9.NotificationQuickDelete
import com.fsck.k9.RobolectricTest
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.testing.MockHelper.mockBuilder
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.ArgumentMatcher
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

private const val ACCOUNT_NUMBER = 42
private const val ACCOUNT_NAME = "accountName"

class SingleMessageNotificationsTest : RobolectricTest() {
    private val resourceProvider: NotificationResourceProvider = TestNotificationResourceProvider()
    private val account = createAccount()
    private val notification = mock<Notification>()
    private val builder = createNotificationBuilder(notification)
    private val actionCreator = mock<NotificationActionCreator>()
    private val notifications = SingleMessageNotifications(
        notificationHelper = createNotificationHelper(builder),
        actionCreator = actionCreator,
        resourceProvider = resourceProvider
    )

    @Test
    fun testBuildStackedNotification() {
        disableOptionalActions()
        val notificationIndex = 0
        val notificationId = NotificationIds.getSingleMessageNotificationId(account, notificationIndex)
        val messageReference = createMessageReference(1)
        val content = createNotificationContent(messageReference)
        val holder = createNotificationHolder(notificationId, content)
        val replyPendingIntent = createFakePendingIntent(1)
        val markAsReadPendingIntent = createFakePendingIntent(2)
        whenever(
            actionCreator.createReplyPendingIntent(
                messageReference,
                notificationId
            )
        ).thenReturn(replyPendingIntent)
        whenever(actionCreator.createMarkMessageAsReadPendingIntent(messageReference, notificationId)).thenReturn(
            markAsReadPendingIntent
        )

        val result = notifications.buildSingleMessageNotification(account, holder)

        assertThat(result).isEqualTo(notification)
        verifyExtendWasOnlyCalledOnce()
        verifyAddAction(resourceProvider.wearIconReplyAll, "Reply", replyPendingIntent)
        verifyAddAction(resourceProvider.wearIconMarkAsRead, "Mark Read", markAsReadPendingIntent)
        verifyNumberOfActions(2)
    }

    @Test
    fun testBuildStackedNotificationWithDeleteActionEnabled() {
        enableDeleteAction()
        val notificationIndex = 0
        val notificationId = NotificationIds.getSingleMessageNotificationId(account, notificationIndex)
        val messageReference = createMessageReference(1)
        val content = createNotificationContent(messageReference)
        val holder = createNotificationHolder(notificationId, content)
        val deletePendingIntent = createFakePendingIntent(1)
        whenever(actionCreator.createDeleteMessagePendingIntent(messageReference, notificationId)).thenReturn(
            deletePendingIntent
        )

        val result = notifications.buildSingleMessageNotification(account, holder)

        assertThat(result).isEqualTo(notification)
        verifyExtendWasOnlyCalledOnce()
        verifyAddAction(resourceProvider.wearIconDelete, "Delete", deletePendingIntent)
    }

    @Test
    fun testBuildStackedNotificationWithArchiveActionEnabled() {
        enableArchiveAction()
        val notificationIndex = 0
        val notificationId = NotificationIds.getSingleMessageNotificationId(account, notificationIndex)
        val messageReference = createMessageReference(1)
        val content = createNotificationContent(messageReference)
        val holder = createNotificationHolder(notificationId, content)
        val archivePendingIntent = createFakePendingIntent(1)
        whenever(actionCreator.createArchiveMessagePendingIntent(messageReference, notificationId)).thenReturn(
            archivePendingIntent
        )

        val result = notifications.buildSingleMessageNotification(account, holder)

        assertThat(result).isEqualTo(notification)
        verifyExtendWasOnlyCalledOnce()
        verifyAddAction(resourceProvider.wearIconArchive, "Archive", archivePendingIntent)
    }

    @Test
    fun testBuildStackedNotificationWithMarkAsSpamActionEnabled() {
        enableSpamAction()
        val notificationIndex = 0
        val notificationId = NotificationIds.getSingleMessageNotificationId(account, notificationIndex)
        val messageReference = createMessageReference(1)
        val content = createNotificationContent(messageReference)
        val holder = createNotificationHolder(notificationId, content)
        val markAsSpamPendingIntent = createFakePendingIntent(1)
        whenever(actionCreator.createMarkMessageAsSpamPendingIntent(messageReference, notificationId)).thenReturn(
            markAsSpamPendingIntent
        )

        val result = notifications.buildSingleMessageNotification(account, holder)

        assertThat(result).isEqualTo(notification)
        verifyExtendWasOnlyCalledOnce()
        verifyAddAction(resourceProvider.wearIconMarkAsSpam, "Spam", markAsSpamPendingIntent)
    }

    private fun disableOptionalActions() {
        disableDeleteAction()
        disableArchiveAction()
        disableSpamAction()
    }

    private fun disableDeleteAction() {
        K9.notificationQuickDeleteBehaviour = NotificationQuickDelete.NEVER
    }

    private fun disableArchiveAction() {
        whenever(account.archiveFolderId).thenReturn(null)
    }

    private fun disableSpamAction() {
        whenever(account.spamFolderId).thenReturn(null)
    }

    private fun enableDeleteAction() {
        K9.notificationQuickDeleteBehaviour = NotificationQuickDelete.ALWAYS
        K9.isConfirmDeleteFromNotification = false
    }

    private fun enableArchiveAction() {
        whenever(account.archiveFolderId).thenReturn(22L)
    }

    private fun enableSpamAction() {
        whenever(account.spamFolderId).thenReturn(11L)
    }

    private fun disableOptionalSummaryActions() {
        disableDeleteAction()
        disableArchiveAction()
    }

    private fun createNotificationBuilder(notification: Notification): NotificationCompat.Builder {
        return mockBuilder {
            on { build() } doReturn notification
        }
    }

    private fun createNotificationHelper(builder: NotificationCompat.Builder): NotificationHelper {
        return mock {
            on { createNotificationBuilder(any(), any()) } doReturn builder
            on { getAccountName(account) } doReturn ACCOUNT_NAME
            on { getContext() } doReturn ApplicationProvider.getApplicationContext()
        }
    }

    private fun createAccount(): Account {
        return mock {
            on { accountNumber } doReturn ACCOUNT_NUMBER
        }
    }

    private fun createMessagingController(): MessagingController {
        return mock {
            on { isMoveCapable(account) } doReturn true
        }
    }

    private fun createNotificationContent(messageReference: MessageReference): NotificationContent {
        return NotificationContent(messageReference, "irrelevant", "irrelevant", "irrelevant", "irrelevant", false)
    }

    private fun createNotificationHolder(notificationId: Int, content: NotificationContent): NotificationHolder {
        return NotificationHolder(notificationId, content)
    }

    private fun createMessageReference(number: Int): MessageReference {
        return MessageReference("account", 1, number.toString(), null)
    }

    private fun createFakePendingIntent(requestCode: Int): PendingIntent {
        return PendingIntent.getActivity(ApplicationProvider.getApplicationContext(), requestCode, null, 0)
    }

    private fun createMessageReferenceList(): ArrayList<MessageReference> {
        return arrayListOf(createMessageReference(1), createMessageReference(2))
    }

    private fun createNotificationData(messageReferences: ArrayList<MessageReference>): NotificationData {
        return mock {
            on { account } doReturn account
            on { getAllMessageReferences() } doReturn messageReferences
        }
    }

    private fun verifyExtendWasOnlyCalledOnce() {
        verify(builder, times(1)).extend(any())
    }

    private fun verifyAddAction(icon: Int, title: String, pendingIntent: PendingIntent) {
        verify(builder).extend(action(icon, title, pendingIntent))
    }

    private fun verifyNumberOfActions(expectedNumberOfActions: Int) {
        verify(builder).extend(numberOfActions(expectedNumberOfActions))
    }

    private fun action(icon: Int, title: String, pendingIntent: PendingIntent): NotificationCompat.WearableExtender {
        return argThat(ActionMatcher(icon, title, pendingIntent))
    }

    private fun numberOfActions(expectedNumberOfActions: Int): NotificationCompat.WearableExtender {
        return argThat(NumberOfActionsMatcher(expectedNumberOfActions))
    }

    internal class ActionMatcher(
        private val icon: Int,
        private val title: String,
        private val pendingIntent: PendingIntent
    ) : ArgumentMatcher<NotificationCompat.WearableExtender> {
        override fun matches(argument: NotificationCompat.WearableExtender): Boolean {
            return argument.actions.any { action ->
                action.icon == icon && action.title == title && action.actionIntent === pendingIntent
            }
        }
    }

    internal class NumberOfActionsMatcher(private val expectedNumberOfActions: Int) :
        ArgumentMatcher<NotificationCompat.WearableExtender> {
        override fun matches(argument: NotificationCompat.WearableExtender): Boolean {
            return argument.actions.size == expectedNumberOfActions
        }
    }
}
