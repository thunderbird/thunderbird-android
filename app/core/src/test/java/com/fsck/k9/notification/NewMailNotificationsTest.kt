package com.fsck.k9.notification

import android.app.Notification
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.K9.NotificationHideSubject
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mailstore.LocalMessage
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing
import org.mockito.kotlin.whenever

private const val ACCOUNT_NUMBER = 23

class NewMailNotificationsTest : K9RobolectricTest() {
    private val account = createAccount()
    private val notificationManager = createNotificationManager()
    private val contentCreator = createNotificationContentCreator()
    private val deviceNotifications = createDeviceNotifications()
    private val wearNotifications = createWearNotifications()
    private val newMailNotifications = TestNewMailNotifications(
        notificationHelper = createNotificationHelper(notificationManager),
        contentCreator = contentCreator,
        deviceNotifications = deviceNotifications,
        wearNotifications = wearNotifications
    )

    @Test
    fun testAddNewMailNotification() {
        val notificationIndex = 0
        val message = createLocalMessage()
        val content = createNotificationContent()
        val holder = createNotificationHolder(content, notificationIndex)
        addToNotificationContentCreator(message, content)
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder))
        val wearNotification = createNotification()
        val summaryNotification = createNotification()
        addToWearNotifications(holder, wearNotification)
        addToDeviceNotifications(summaryNotification)

        newMailNotifications.addNewMailNotification(account, message, 42)

        val wearNotificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex)
        verify(notificationManager).notify(wearNotificationId, wearNotification)
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
        val wearNotification = createNotification()
        val summaryNotification = createNotification()
        addToWearNotifications(holder, wearNotification)
        addToDeviceNotifications(summaryNotification)

        newMailNotifications.addNewMailNotification(account, message, 42)

        val wearNotificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex)
        verify(notificationManager).notify(wearNotificationId, wearNotification)
        verify(notificationManager).cancel(wearNotificationId)
        val summaryNotificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        verify(notificationManager).notify(summaryNotificationId, summaryNotification)
    }

    @Test
    fun testAddNewMailNotificationWithPrivacyModeEnabled() {
        enablePrivacyMode()
        val notificationIndex = 0
        val message = createLocalMessage()
        val content = createNotificationContent()
        val holder = createNotificationHolder(content, notificationIndex)
        addToNotificationContentCreator(message, content)
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder))
        val wearNotification = createNotification()
        addToDeviceNotifications(wearNotification)

        newMailNotifications.addNewMailNotification(account, message, 42)

        val wearNotificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex)
        val summaryNotificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        verify(notificationManager, never()).notify(eq(wearNotificationId), any())
        verify(notificationManager).notify(summaryNotificationId, wearNotification)
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
        val wearNotificationOne = createNotification()
        val wearNotificationTwo = createNotification()
        val summaryNotification = createNotification()
        addToWearNotifications(holderOne, wearNotificationOne)
        addToWearNotifications(holderTwo, wearNotificationTwo)
        addToDeviceNotifications(summaryNotification)

        newMailNotifications.addNewMailNotification(account, messageOne, 42)
        newMailNotifications.addNewMailNotification(account, messageTwo, 42)

        val wearNotificationIdOne = NotificationIds.getNewMailStackedNotificationId(account, notificationIndexOne)
        verify(notificationManager).notify(wearNotificationIdOne, wearNotificationOne)
        val wearNotificationIdTwo = NotificationIds.getNewMailStackedNotificationId(account, notificationIndexTwo)
        verify(notificationManager).notify(wearNotificationIdTwo, wearNotificationTwo)
        val summaryNotificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        verify(notificationManager, times(2)).notify(summaryNotificationId, summaryNotification)
    }

    @Test
    fun testRemoveNewMailNotificationWithoutNotificationData() {
        val messageReference = createMessageReference(1)

        newMailNotifications.removeNewMailNotification(account, messageReference)

        verify(notificationManager, never()).cancel(anyInt())
    }

    @Test
    fun testRemoveNewMailNotificationWithUnknownMessageReference() {
        enablePrivacyMode()
        val messageReference = createMessageReference(1)
        val notificationIndex = 0
        val message = createLocalMessage()
        val content = createNotificationContent()
        val holder = createNotificationHolder(content, notificationIndex)
        addToNotificationContentCreator(message, content)
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder))
        val summaryNotification = createNotification()
        addToDeviceNotifications(summaryNotification)
        newMailNotifications.addNewMailNotification(account, message, 23)
        whenRemovingContentReturn(messageReference, RemoveNotificationResult.unknownNotification())

        newMailNotifications.removeNewMailNotification(account, messageReference)

        verify(notificationManager, never()).cancel(anyInt())
    }

    @Test
    fun testRemoveNewMailNotification() {
        enablePrivacyMode()
        val messageReference = createMessageReference(1)
        val notificationIndex = 0
        val notificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex)
        val message = createLocalMessage()
        val content = createNotificationContent()
        val holder = createNotificationHolder(content, notificationIndex)
        addToNotificationContentCreator(message, content)
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder))
        val summaryNotification = createNotification()
        addToDeviceNotifications(summaryNotification)
        newMailNotifications.addNewMailNotification(account, message, 23)
        whenRemovingContentReturn(messageReference, RemoveNotificationResult.cancelNotification(notificationId))

        newMailNotifications.removeNewMailNotification(account, messageReference)

        verify(notificationManager).cancel(notificationId)
        val summaryNotificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        verify(notificationManager, times(2)).notify(summaryNotificationId, summaryNotification)
    }

    @Test
    fun testRemoveNewMailNotificationClearingAllNotifications() {
        val messageReference = createMessageReference(1)
        val notificationIndex = 0
        val notificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex)
        val message = createLocalMessage()
        val content = createNotificationContent()
        val holder = createNotificationHolder(content, notificationIndex)
        addToNotificationContentCreator(message, content)
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder))
        val summaryNotification = createNotification()
        addToDeviceNotifications(summaryNotification)
        newMailNotifications.addNewMailNotification(account, message, 23)
        whenRemovingContentReturn(messageReference, RemoveNotificationResult.cancelNotification(notificationId))
        whenever(newMailNotifications.notificationData.newMessagesCount).thenReturn(0)
        setActiveNotificationIds()

        newMailNotifications.removeNewMailNotification(account, messageReference)

        verify(notificationManager).cancel(notificationId)
        val summaryNotificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        verify(notificationManager).cancel(summaryNotificationId)
    }

    @Test
    fun testRemoveNewMailNotificationWithCreateNotification() {
        val messageReference = createMessageReference(1)
        val notificationIndex = 0
        val notificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex)
        val message = createLocalMessage()
        val contentOne = createNotificationContent()
        val contentTwo = createNotificationContent()
        val holderOne = createNotificationHolder(contentOne, notificationIndex)
        val holderTwo = createNotificationHolder(contentTwo, notificationIndex)
        addToNotificationContentCreator(message, contentOne)
        whenAddingContentReturn(contentOne, AddNotificationResult.newNotification(holderOne))
        val summaryNotification = createNotification()
        addToDeviceNotifications(summaryNotification)
        val wearNotificationOne = createNotification()
        val wearNotificationTwo = createNotification()
        addToWearNotifications(holderOne, wearNotificationOne)
        addToWearNotifications(holderTwo, wearNotificationTwo)
        newMailNotifications.addNewMailNotification(account, message, 23)
        whenRemovingContentReturn(messageReference, RemoveNotificationResult.createNotification(holderTwo))

        newMailNotifications.removeNewMailNotification(account, messageReference)

        verify(notificationManager).cancel(notificationId)
        verify(notificationManager).notify(notificationId, wearNotificationTwo)
        val summaryNotificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        verify(notificationManager, times(2)).notify(summaryNotificationId, summaryNotification)
    }

    @Test
    fun testClearNewMailNotificationsWithoutNotificationData() {
        newMailNotifications.clearNewMailNotifications(account)

        verify(notificationManager, never()).cancel(anyInt())
    }

    @Test
    fun testClearNewMailNotifications() {
        val notificationIndex = 0
        val notificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex)
        val message = createLocalMessage()
        val content = createNotificationContent()
        val holder = createNotificationHolder(content, notificationIndex)
        addToNotificationContentCreator(message, content)
        setActiveNotificationIds(notificationId)
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder))
        newMailNotifications.addNewMailNotification(account, message, 3)

        newMailNotifications.clearNewMailNotifications(account)

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
        val messageReference = MessageReference("irrelevant", 1, "irrelevant", null)
        return NotificationContent(messageReference, "irrelevant", "irrelevant", "irrelevant", "irrelevant", false)
    }

    private fun createNotificationHolder(content: NotificationContent, index: Int): NotificationHolder {
        val notificationId = NotificationIds.getNewMailStackedNotificationId(account, index)
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

    private fun createDeviceNotifications(): DeviceNotifications = mock()

    private fun addToDeviceNotifications(notificationToReturn: Notification) {
        stubbing(deviceNotifications) {
            on {
                buildSummaryNotification(eq(account), eq(newMailNotifications.notificationData), anyBoolean())
            } doReturn notificationToReturn
        }
    }

    private fun createNotification(): Notification = mock()

    private fun createWearNotifications(): WearNotifications = mock()

    private fun createMessageReference(number: Int): MessageReference {
        return MessageReference("account", 1, number.toString(), null)
    }

    private fun addToWearNotifications(notificationHolder: NotificationHolder, notificationToReturn: Notification) {
        whenever(wearNotifications.buildStackedNotification(account, notificationHolder))
            .thenReturn(notificationToReturn)
    }

    private fun whenAddingContentReturn(content: NotificationContent, result: AddNotificationResult) {
        val notificationData = newMailNotifications.notificationData
        val newCount = notificationData.newMessagesCount + 1

        stubbing(notificationData) {
            on { addNotificationContent(content) } doReturn result
            on { newMessagesCount } doReturn newCount
        }
    }

    private fun whenRemovingContentReturn(messageReference: MessageReference, result: RemoveNotificationResult) {
        stubbing(newMailNotifications.notificationData) {
            on { removeNotificationForMessage(messageReference) } doReturn result
        }
    }

    private fun setActiveNotificationIds(vararg notificationIds: Int) {
        stubbing(newMailNotifications.notificationData) {
            on { getActiveNotificationIds() } doReturn notificationIds
        }
    }

    private fun enablePrivacyMode() {
        K9.notificationHideSubject = NotificationHideSubject.ALWAYS
    }

    internal class TestNewMailNotifications(
        notificationHelper: NotificationHelper,
        contentCreator: NotificationContentCreator,
        deviceNotifications: DeviceNotifications,
        wearNotifications: WearNotifications
    ) : NewMailNotifications(
        notificationHelper, contentCreator, deviceNotifications, wearNotifications
    ) {
        val notificationData = mock<NotificationData>()

        override fun createNotificationData(account: Account, unreadMessageCount: Int): NotificationData {
            return notificationData
        }
    }
}
