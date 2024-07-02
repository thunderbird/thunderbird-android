package com.fsck.k9.notification

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.K9.NotificationQuickDelete
import com.fsck.k9.controller.MessageReference
import org.junit.Test

class SingleMessageNotificationDataCreatorTest {
    private val account = createAccount()
    private val notificationDataCreator = SingleMessageNotificationDataCreator()

    @Test
    fun `base properties`() {
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 23,
            content = content,
            timestamp = 9000,
            addLockScreenNotification = true,
        )

        assertThat(result.notificationId).isEqualTo(23)
        assertThat(result.isSilent).isTrue()
        assertThat(result.timestamp).isEqualTo(9000)
        assertThat(result.content).isEqualTo(content)
        assertThat(result.addLockScreenNotification).isTrue()
    }

    @Test
    fun `summary notification base properties`() {
        val content = createNotificationContent()
        val notificationData = createNotificationData(content)

        val result = notificationDataCreator.createSummarySingleNotificationData(
            timestamp = 9000,
            silent = false,
            data = notificationData,
        )

        assertThat(result.singleNotificationData.notificationId).isEqualTo(
            NotificationIds.getNewMailSummaryNotificationId(account),
        )
        assertThat(result.singleNotificationData.isSilent).isFalse()
        assertThat(result.singleNotificationData.timestamp).isEqualTo(9000)
        assertThat(result.singleNotificationData.content).isEqualTo(content)
        assertThat(result.singleNotificationData.addLockScreenNotification).isFalse()
    }

    @Test
    fun `default actions`() {
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.actions).contains(NotificationAction.Reply)
        assertThat(result.actions).contains(NotificationAction.MarkAsRead)
        assertThat(result.wearActions).contains(WearNotificationAction.Reply)
        assertThat(result.wearActions).contains(WearNotificationAction.MarkAsRead)
    }

    @Test
    fun `always show delete action without confirmation`() {
        setDeleteAction(NotificationQuickDelete.ALWAYS)
        setConfirmDeleteFromNotification(false)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.actions).contains(NotificationAction.Delete)
        assertThat(result.wearActions).contains(WearNotificationAction.Delete)
    }

    @Test
    fun `always show delete action with confirmation`() {
        setDeleteAction(NotificationQuickDelete.ALWAYS)
        setConfirmDeleteFromNotification(true)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.actions).contains(NotificationAction.Delete)
        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Delete)
    }

    @Test
    fun `show delete action for single notification without confirmation`() {
        setDeleteAction(NotificationQuickDelete.FOR_SINGLE_MSG)
        setConfirmDeleteFromNotification(false)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.actions).contains(NotificationAction.Delete)
        assertThat(result.wearActions).contains(WearNotificationAction.Delete)
    }

    @Test
    fun `show delete action for single notification with confirmation`() {
        setDeleteAction(NotificationQuickDelete.FOR_SINGLE_MSG)
        setConfirmDeleteFromNotification(true)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.actions).contains(NotificationAction.Delete)
        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Delete)
    }

    @Test
    fun `never show delete action`() {
        setDeleteAction(NotificationQuickDelete.NEVER)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.actions).doesNotContain(NotificationAction.Delete)
        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Delete)
    }

    @Test
    fun `archive action with archive folder`() {
        account.archiveFolderId = 1
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.wearActions).contains(WearNotificationAction.Archive)
    }

    @Test
    fun `archive action without archive folder`() {
        account.archiveFolderId = null
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Archive)
    }

    @Test
    fun `spam action with spam folder and without spam confirmation`() {
        account.spamFolderId = 1
        setConfirmSpam(false)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.wearActions).contains(WearNotificationAction.Spam)
    }

    @Test
    fun `spam action with spam folder and with spam confirmation`() {
        account.spamFolderId = 1
        setConfirmSpam(true)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Spam)
    }

    @Test
    fun `spam action without spam folder and without spam confirmation`() {
        account.spamFolderId = null
        setConfirmSpam(false)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Spam)
    }

    private fun setDeleteAction(mode: NotificationQuickDelete) {
        K9.notificationQuickDeleteBehaviour = mode
    }

    private fun setConfirmDeleteFromNotification(confirm: Boolean) {
        K9.isConfirmDeleteFromNotification = confirm
    }

    private fun setConfirmSpam(confirm: Boolean) {
        K9.isConfirmSpam = confirm
    }

    private fun createAccount(): Account {
        return Account("00000000-0000-0000-0000-000000000000").apply {
            accountNumber = 42
        }
    }

    private fun createNotificationContent() = NotificationContent(
        messageReference = MessageReference("irrelevant", 1, "irrelevant"),
        sender = "irrelevant",
        subject = "irrelevant",
        preview = "irrelevant",
        summary = "irrelevant",
    )

    private fun createNotificationData(content: NotificationContent): NotificationData {
        return NotificationData(
            account,
            activeNotifications = listOf(
                NotificationHolder(
                    notificationId = 1,
                    timestamp = 0,
                    content = content,
                ),
            ),
            inactiveNotifications = emptyList(),
        )
    }
}
