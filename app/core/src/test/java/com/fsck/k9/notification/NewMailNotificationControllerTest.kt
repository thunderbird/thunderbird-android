package com.fsck.k9.notification

import android.app.Notification
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.Account
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mailstore.LocalMessage
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing
import org.mockito.kotlin.whenever

private const val ACCOUNT_NUMBER = 23

class NewMailNotificationControllerTest : K9RobolectricTest() {
    private val account = createAccount()
    private val notificationManager = createNotificationManager()
    private val contentCreator = createNotificationContentCreator()
    private val summaryNotificationCreator = createSummaryNotificationCreator()
    private val singleMessageNotificationCreator = createSingleMessageNotificationCreator()
    private val controller = TestNewMailNotificationController(
        notificationHelper = createNotificationHelper(notificationManager),
        contentCreator = contentCreator,
        summaryNotificationCreator = summaryNotificationCreator,
        singleMessageNotificationCreator = singleMessageNotificationCreator
    )

    @Test
    fun testAddNewMailNotification() {
        val notificationIndex = 0
        val message = createLocalMessage()
        val content = createNotificationContent()
        val holder = createNotificationHolder(content, notificationIndex)
        addToNotificationContentCreator(message, content)
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder))
        val singleMessageNotification = createNotification()
        val summaryNotification = createNotification()
        addToSingleMessageNotificationCreator(holder, singleMessageNotification)
        addToSummaryNotificationCreator(summaryNotification)

        controller.addNewMailNotification(account, message, silent = false)

        val singleMessageNotificationId = NotificationIds.getSingleMessageNotificationId(account, notificationIndex)
        verify(notificationManager).notify(singleMessageNotificationId, singleMessageNotification)
        val summaryNotificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        verify(notificationManager).notify(summaryNotificationId, summaryNotification)
    }

    @Test
    fun testAddNewMailNotificationWithCancelingExistingNotification() {
        val notificationIndex = 0
        val message = createLocalMessage()
        val content = createNotificationContent()
        val holder = createNotificationHolder(content, notificationIndex)
        addToNotificationContentCreator(message, content)
        whenAddingContentReturn(content, AddNotificationResult.replaceNotification(holder))
        val singleMessageNotification = createNotification()
        val summaryNotification = createNotification()
        addToSingleMessageNotificationCreator(holder, singleMessageNotification)
        addToSummaryNotificationCreator(summaryNotification)

        controller.addNewMailNotification(account, message, silent = false)

        val singleMessageNotificationId = NotificationIds.getSingleMessageNotificationId(account, notificationIndex)
        verify(notificationManager).notify(singleMessageNotificationId, singleMessageNotification)
        verify(notificationManager).cancel(singleMessageNotificationId)
        val summaryNotificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        verify(notificationManager).notify(summaryNotificationId, summaryNotification)
    }

    @Test
    fun testAddNewMailNotificationTwice() {
        val notificationIndexOne = 0
        val notificationIndexTwo = 1
        val messageOne = createLocalMessage()
        val messageTwo = createLocalMessage()
        val contentOne = createNotificationContent()
        val contentTwo = createNotificationContent()
        val holderOne = createNotificationHolder(contentOne, notificationIndexOne)
        val holderTwo = createNotificationHolder(contentTwo, notificationIndexTwo)
        addToNotificationContentCreator(messageOne, contentOne)
        addToNotificationContentCreator(messageTwo, contentTwo)
        whenAddingContentReturn(contentOne, AddNotificationResult.newNotification(holderOne))
        whenAddingContentReturn(contentTwo, AddNotificationResult.newNotification(holderTwo))
        val singleMessageNotificationOne = createNotification()
        val singleMessageNotificationTwo = createNotification()
        val summaryNotification = createNotification()
        addToSingleMessageNotificationCreator(holderOne, singleMessageNotificationOne)
        addToSingleMessageNotificationCreator(holderTwo, singleMessageNotificationTwo)
        addToSummaryNotificationCreator(summaryNotification)

        controller.addNewMailNotification(account, messageOne, silent = false)
        controller.addNewMailNotification(account, messageTwo, silent = false)

        val singleMessageNotificationIdOne = NotificationIds.getSingleMessageNotificationId(account, notificationIndexOne)
        verify(notificationManager).notify(singleMessageNotificationIdOne, singleMessageNotificationOne)
        val singleMessageNotificationIdTwo = NotificationIds.getSingleMessageNotificationId(account, notificationIndexTwo)
        verify(notificationManager).notify(singleMessageNotificationIdTwo, singleMessageNotificationTwo)
        val summaryNotificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        verify(notificationManager, times(2)).notify(summaryNotificationId, summaryNotification)
    }

    @Test
    fun testRemoveNewMailNotificationWithoutNotificationData() {
        val messageReference = createMessageReference(1)

        controller.removeNewMailNotification(account, messageReference)

        verify(notificationManager, never()).cancel(anyInt())
    }

    @Test
    fun testRemoveNewMailNotificationWithUnknownMessageReference() {
        val messageReference = createMessageReference(1)
        val notificationIndex = 0
        val message = createLocalMessage()
        val content = createNotificationContent()
        val holder = createNotificationHolder(content, notificationIndex)
        addToNotificationContentCreator(message, content)
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder))
        val summaryNotification = createNotification()
        addToSummaryNotificationCreator(summaryNotification)
        controller.addNewMailNotification(account, message, silent = false)
        whenRemovingContentReturn(messageReference, RemoveNotificationResult.unknownNotification())

        controller.removeNewMailNotification(account, messageReference)

        verify(notificationManager, never()).cancel(anyInt())
    }

    @Test
    fun testRemoveNewMailNotification() {
        val messageReference = createMessageReference(1)
        val notificationIndex = 0
        val notificationId = NotificationIds.getSingleMessageNotificationId(account, notificationIndex)
        val message = createLocalMessage()
        val content = createNotificationContent()
        val holder = createNotificationHolder(content, notificationIndex)
        addToNotificationContentCreator(message, content)
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder))
        val summaryNotification = createNotification()
        addToSummaryNotificationCreator(summaryNotification)
        controller.addNewMailNotification(account, message, silent = false)
        whenRemovingContentReturn(messageReference, RemoveNotificationResult.cancelNotification(notificationId))

        controller.removeNewMailNotification(account, messageReference)

        verify(notificationManager).cancel(notificationId)
        val summaryNotificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        verify(notificationManager, times(2)).notify(summaryNotificationId, summaryNotification)
    }

    @Test
    fun testRemoveNewMailNotificationClearingAllNotifications() {
        val messageReference = createMessageReference(1)
        val notificationIndex = 0
        val notificationId = NotificationIds.getSingleMessageNotificationId(account, notificationIndex)
        val message = createLocalMessage()
        val content = createNotificationContent()
        val holder = createNotificationHolder(content, notificationIndex)
        addToNotificationContentCreator(message, content)
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder))
        val summaryNotification = createNotification()
        addToSummaryNotificationCreator(summaryNotification)
        controller.addNewMailNotification(account, message, silent = false)
        whenRemovingContentReturn(messageReference, RemoveNotificationResult.cancelNotification(notificationId))
        whenever(controller.notificationData.newMessagesCount).thenReturn(0)
        setActiveNotificationIds()

        controller.removeNewMailNotification(account, messageReference)

        verify(notificationManager).cancel(notificationId)
        val summaryNotificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        verify(notificationManager).cancel(summaryNotificationId)
    }

    @Test
    fun testRemoveNewMailNotificationWithCreateNotification() {
        val messageReference = createMessageReference(1)
        val notificationIndex = 0
        val notificationId = NotificationIds.getSingleMessageNotificationId(account, notificationIndex)
        val message = createLocalMessage()
        val contentOne = createNotificationContent()
        val contentTwo = createNotificationContent()
        val holderOne = createNotificationHolder(contentOne, notificationIndex)
        val holderTwo = createNotificationHolder(contentTwo, notificationIndex)
        addToNotificationContentCreator(message, contentOne)
        whenAddingContentReturn(contentOne, AddNotificationResult.newNotification(holderOne))
        val summaryNotification = createNotification()
        addToSummaryNotificationCreator(summaryNotification)
        val singleMessageNotificationOne = createNotification()
        val singleMessageNotificationTwo = createNotification()
        addToSingleMessageNotificationCreator(holderOne, singleMessageNotificationOne)
        addToSingleMessageNotificationCreator(holderTwo, singleMessageNotificationTwo)
        controller.addNewMailNotification(account, message, silent = false)
        whenRemovingContentReturn(messageReference, RemoveNotificationResult.createNotification(holderTwo))

        controller.removeNewMailNotification(account, messageReference)

        verify(notificationManager).cancel(notificationId)
        verify(notificationManager).notify(notificationId, singleMessageNotificationTwo)
        val summaryNotificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        verify(notificationManager, times(2)).notify(summaryNotificationId, summaryNotification)
    }

    @Test
    fun testClearNewMailNotificationsWithoutNotificationData() {
        controller.clearNewMailNotifications(account)

        verify(notificationManager, never()).cancel(anyInt())
    }

    @Test
    fun testClearNewMailNotifications() {
        val notificationIndex = 0
        val notificationId = NotificationIds.getSingleMessageNotificationId(account, notificationIndex)
        val message = createLocalMessage()
        val content = createNotificationContent()
        val holder = createNotificationHolder(content, notificationIndex)
        addToNotificationContentCreator(message, content)
        setActiveNotificationIds(notificationId)
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder))
        controller.addNewMailNotification(account, message, silent = false)

        controller.clearNewMailNotifications(account)

        verify(notificationManager).cancel(notificationId)
        verify(notificationManager).cancel(NotificationIds.getNewMailSummaryNotificationId(account))
    }

    private fun createAccount(): Account {
        return mock {
            on { accountNumber } doReturn ACCOUNT_NUMBER
        }
    }

    private fun createLocalMessage(): LocalMessage = mock()

    private fun createNotificationContent(): NotificationContent {
        val messageReference = MessageReference(accountUuid = "irrelevant", folderId = 1, uid = "irrelevant")
        return NotificationContent(
            messageReference = messageReference,
            sender = "irrelevant",
            subject = "irrelevant",
            preview = "irrelevant",
            summary = "irrelevant"
        )
    }

    private fun createNotificationHolder(content: NotificationContent, index: Int): NotificationHolder {
        val notificationId = NotificationIds.getSingleMessageNotificationId(account, index)
        return NotificationHolder(notificationId, content)
    }

    private fun createNotificationManager(): NotificationManagerCompat = mock()

    private fun createNotificationHelper(notificationManager: NotificationManagerCompat): NotificationHelper {
        return mock {
            on { getNotificationManager() } doReturn notificationManager
        }
    }

    private fun createNotificationContentCreator(): NotificationContentCreator = mock()

    private fun addToNotificationContentCreator(message: LocalMessage, content: NotificationContent) {
        stubbing(contentCreator) {
            on { createFromMessage(account, message) } doReturn content
        }
    }

    private fun createSummaryNotificationCreator(): SummaryNotificationCreator = mock()

    private fun addToSummaryNotificationCreator(notificationToReturn: Notification) {
        stubbing(summaryNotificationCreator) {
            on {
                buildSummaryNotification(eq(account), eq(controller.notificationData), anyBoolean())
            } doReturn notificationToReturn
        }
    }

    private fun createNotification(): Notification = mock()

    private fun createSingleMessageNotificationCreator(): SingleMessageNotificationCreator = mock()

    private fun createMessageReference(number: Int): MessageReference {
        return MessageReference("account", 1, number.toString())
    }

    private fun addToSingleMessageNotificationCreator(
        notificationHolder: NotificationHolder,
        notificationToReturn: Notification
    ) {
        stubbing(singleMessageNotificationCreator) {
            on { buildSingleMessageNotification(account, notificationHolder) } doReturn notificationToReturn
        }
    }

    private fun whenAddingContentReturn(content: NotificationContent, result: AddNotificationResult) {
        val notificationData = controller.notificationData
        val newCount = notificationData.newMessagesCount + 1

        stubbing(notificationData) {
            on { addNotificationContent(content) } doReturn result
            on { newMessagesCount } doReturn newCount
        }
    }

    private fun whenRemovingContentReturn(messageReference: MessageReference, result: RemoveNotificationResult) {
        stubbing(controller.notificationData) {
            on { removeNotificationForMessage(messageReference) } doReturn result
        }
    }

    private fun setActiveNotificationIds(vararg notificationIds: Int) {
        stubbing(controller.notificationData) {
            on { getActiveNotificationIds() } doReturn notificationIds
        }
    }

    internal class TestNewMailNotificationController(
        notificationHelper: NotificationHelper,
        contentCreator: NotificationContentCreator,
        summaryNotificationCreator: SummaryNotificationCreator,
        singleMessageNotificationCreator: SingleMessageNotificationCreator
    ) : NewMailNotificationController(
        notificationHelper, contentCreator, summaryNotificationCreator, singleMessageNotificationCreator
    ) {
        val notificationData = mock<NotificationData>()

        override fun createNotificationData(account: Account): NotificationData {
            return notificationData
        }
    }
}
