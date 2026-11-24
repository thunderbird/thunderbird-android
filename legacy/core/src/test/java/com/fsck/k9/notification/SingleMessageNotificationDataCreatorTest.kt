package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.FakeLegacyAccount
import com.fsck.k9.mail.Address
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.preference.LockScreenNotificationVisibility
import net.thunderbird.core.preference.NotificationQuickDelete
import net.thunderbird.core.preference.interaction.InteractionSettings
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import net.thunderbird.core.preference.notification.NotificationPreference
import net.thunderbird.core.preference.notification.NotificationPreferenceManager
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing

class SingleMessageNotificationDataCreatorTest {

    private val accountId = AccountIdFactory.create()
    private val account = createAccount()
    private val accountManager = mock<LegacyAccountManager> {
        on { findById(accountId) } doReturn account
    }
    private val notificationIdRegistry = DefaultAccountNotificationIdRegistry(accountManager)
    private val fakeInteractionPreferences = FakeInteractionSettingsPreferenceManager()
    private val fakeNotificationPreferences = FakeNotificationPreferenceManager()
    private val notificationDataCreator = SingleMessageNotificationDataCreator(
        interactionPreferences = fakeInteractionPreferences,
        notificationPreference = fakeNotificationPreferences,
        accountManager = accountManager,
        notificationIdRegistry = notificationIdRegistry,
    )

    @Test
    fun `base properties`() {
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            accountId = accountId,
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
            NotificationIds.getNewMailSummaryNotificationId(account.accountNumber),
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
            accountId = accountId,
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
        setMessageActions(cutoff = 3)
        fakeInteractionPreferences.setConfirmDeleteFromNotification(false)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            accountId = accountId,
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
        setMessageActions(cutoff = 3)
        fakeInteractionPreferences.setConfirmDeleteFromNotification(true)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            accountId = accountId,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.actions).contains(NotificationAction.Delete)
        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Delete)
    }

    @Test
    fun `only show actions above cutoff`() {
        setMessageActions(cutoff = 2)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            accountId = accountId,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.actions).containsExactly(
            NotificationAction.Reply,
            NotificationAction.MarkAsRead,
        )
    }

    @Test
    fun `show no actions when cutoff is zero`() {
        setMessageActions(cutoff = 0)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            accountId = accountId,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.actions).isEmpty()
    }

    @Test
    fun `archive action with archive folder`() {
        val accountWithArchive = account.copy(archiveFolderId = 1L)
        stubbing(accountManager) {
            on { findById(accountId) } doReturn accountWithArchive
        }
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            accountId = accountId,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.wearActions).contains(WearNotificationAction.Archive)
    }

    @Test
    fun `archive action without archive folder`() {
        val accountWithoutArchive = account.copy(archiveFolderId = null)
        stubbing(accountManager) {
            on { findById(accountId) } doReturn accountWithoutArchive
        }
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            accountId = accountId,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Archive)
    }

    @Test
    fun `spam action with spam folder and without spam confirmation`() {
        val accountWithSpam = account.copy(spamFolderId = 1L)
        stubbing(accountManager) {
            on { findById(accountId) } doReturn accountWithSpam
        }
        fakeInteractionPreferences.setConfirmSpam(false)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            accountId = accountId,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.wearActions).contains(WearNotificationAction.Spam)
    }

    @Test
    fun `spam action with spam folder and with spam confirmation`() {
        val accountWithSpam = account.copy(spamFolderId = 1L)
        stubbing(accountManager) {
            on { findById(accountId) } doReturn accountWithSpam
        }
        fakeInteractionPreferences.setConfirmSpam(true)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            accountId = accountId,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Spam)
    }

    @Test
    fun `spam action without spam folder and without spam confirmation`() {
        val accountWithoutSpam = account.copy(spamFolderId = null)
        stubbing(accountManager) {
            on { findById(accountId) } doReturn accountWithoutSpam
        }
        fakeInteractionPreferences.setConfirmSpam(false)
        val content = createNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            accountId = accountId,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false,
        )

        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Spam)
    }

    private fun setMessageActions(cutoff: Int) {
        fakeNotificationPreferences.setMessageActions(
            order = listOf("reply", "mark_as_read", "delete", "archive", "spam"),
            cutoff = cutoff,
        )
    }

    private fun createAccount(): LegacyAccount {
        return FakeLegacyAccount.ACCOUNT.copy(
            id = accountId,
            accountNumber = 42,
        )
    }

    private fun createNotificationContent() = NotificationContent(
        messageReference = MessageReference(accountId, 1, "irrelevant"),
        sender = Address("irrelevant", "irrelevant"),
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
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.MESSAGE_COUNT,
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
    private class FakeNotificationPreferenceManager : NotificationPreferenceManager {
        private val prefs = MutableStateFlow(NotificationPreference())

        override fun save(config: NotificationPreference) = Unit

        override fun getConfig(): NotificationPreference = prefs.value

        override fun getConfigFlow(): Flow<NotificationPreference> = prefs

        fun setNotificationQuickDeleteBehaviour(behaviour: NotificationQuickDelete) {
            prefs.update { it.copy(notificationQuickDeleteBehaviour = behaviour) }
        }

        fun setMessageActions(order: List<String>, cutoff: Int) {
            prefs.update { it.copy(messageActionsOrder = order, messageActionsCutoff = cutoff) }
        }
    }
}
