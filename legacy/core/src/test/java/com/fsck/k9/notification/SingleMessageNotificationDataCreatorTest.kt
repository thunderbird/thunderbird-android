package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.K9
import com.fsck.k9.K9.NotificationQuickDelete
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.interaction.InteractionSettings
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import net.thunderbird.core.preference.notification.NotificationPreference
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

class SingleMessageNotificationDataCreatorTest {
    private val account = createAccount()
    private val fakeInteractionPreferences = FakeInteractionSettingsPreferenceManager()
    private val generalSettings = GeneralSettings(
        platformConfigProvider = FakePlatformConfigProvider(),
        notification = NotificationPreference(),
    )
    private val notificationDataCreator = SingleMessageNotificationDataCreator(
        interactionPreferences = fakeInteractionPreferences,
        generalSettingsManager = mock {
            on { getConfig() } doAnswer { generalSettings }
        },
    )

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
        fakeInteractionPreferences.setConfirmDeleteFromNotification(false)
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
        fakeInteractionPreferences.setConfirmDeleteFromNotification(true)
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
        fakeInteractionPreferences.setConfirmDeleteFromNotification(false)
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
        fakeInteractionPreferences.setConfirmDeleteFromNotification(true)
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
        fakeInteractionPreferences.setConfirmSpam(false)
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
        fakeInteractionPreferences.setConfirmSpam(true)
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
        fakeInteractionPreferences.setConfirmSpam(false)
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

    private fun createAccount(): LegacyAccountDto {
        return LegacyAccountDto("00000000-0000-0000-0000-000000000000").apply {
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

    private class FakeInteractionSettingsPreferenceManager : InteractionSettingsPreferenceManager {
        private val prefs = MutableStateFlow(InteractionSettings())

        override fun save(config: InteractionSettings) = Unit

        override fun getConfig(): InteractionSettings = prefs.value

        override fun getConfigFlow(): Flow<InteractionSettings> = prefs

        fun setConfirmDeleteFromNotification(confirm: Boolean) {
            prefs.update { it.copy(isConfirmDeleteFromNotification = confirm) }
        }

        fun setConfirmSpam(confirm: Boolean) {
            prefs.update { it.copy(isConfirmSpam = confirm) }
        }
    }
}
