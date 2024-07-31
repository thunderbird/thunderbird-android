package com.fsck.k9.notification

import app.k9mail.legacy.account.Identity
import app.k9mail.legacy.notification.NotificationLight
import app.k9mail.legacy.notification.NotificationVibration
import app.k9mail.legacy.notification.VibratePattern
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isSameInstanceAs
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.K9.LockScreenNotificationVisibility
import org.junit.Test
import org.mockito.kotlin.mock

class BaseNotificationDataCreatorTest {
    private val account = createAccount()
    private val notificationDataCreator = BaseNotificationDataCreator()

    @Test
    fun `account instance`() {
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.account).isSameInstanceAs(account)
    }

    @Test
    fun `account name from name property`() {
        account.name = "name"
        account.email = "irrelevant@k9mail.example"
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.accountName).isEqualTo("name")
    }

    @Test
    fun `account name is blank`() {
        account.name = ""
        account.email = "test@k9mail.example"
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.accountName).isEqualTo("test@k9mail.example")
    }

    @Test
    fun `account name is null`() {
        account.name = null
        account.email = "test@k9mail.example"
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.accountName).isEqualTo("test@k9mail.example")
    }

    @Test
    fun `group key`() {
        account.accountNumber = 42
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.groupKey).isEqualTo("newMailNotifications-42")
    }

    @Test
    fun `notification color`() {
        account.chipColor = 0xFF0000
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.color).isEqualTo(0xFF0000)
    }

    @Test
    fun `new messages count`() {
        val notificationData = createNotificationData(senders = listOf("irrelevant", "irrelevant"))

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.newMessagesCount).isEqualTo(2)
    }

    @Test
    fun `do not display notification on lock screen`() {
        setLockScreenMode(LockScreenNotificationVisibility.NOTHING)
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isEqualTo(LockScreenNotificationData.None)
    }

    @Test
    fun `display application name on lock screen`() {
        setLockScreenMode(LockScreenNotificationVisibility.APP_NAME)
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isEqualTo(LockScreenNotificationData.AppName)
    }

    @Test
    fun `display new message count on lock screen`() {
        setLockScreenMode(LockScreenNotificationVisibility.MESSAGE_COUNT)
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isEqualTo(LockScreenNotificationData.MessageCount)
    }

    @Test
    fun `display message sender names on lock screen`() {
        setLockScreenMode(LockScreenNotificationVisibility.SENDERS)
        val notificationData = createNotificationData(senders = listOf("Sender One", "Sender Two", "Sender Three"))

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isInstanceOf<LockScreenNotificationData.SenderNames>()
        val senderNamesData = result.lockScreenNotificationData as LockScreenNotificationData.SenderNames
        assertThat(senderNamesData.senderNames).isEqualTo("Sender One, Sender Two, Sender Three")
    }

    @Test
    fun `display notification on lock screen`() {
        setLockScreenMode(LockScreenNotificationVisibility.EVERYTHING)
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isEqualTo(LockScreenNotificationData.Public)
    }

    @Test
    fun ringtone() {
        account.updateNotificationSettings { it.copy(ringtone = "content://ringtone/1") }
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.appearance.ringtone).isEqualTo("content://ringtone/1")
    }

    @Test
    fun `vibration pattern`() {
        account.updateNotificationSettings {
            it.copy(
                vibration = NotificationVibration(
                    isEnabled = true,
                    pattern = VibratePattern.Pattern3,
                    repeatCount = 2,
                ),
            )
        }
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.appearance.vibrationPattern).isNotNull()
            .isEqualTo(NotificationVibration.getSystemPattern(VibratePattern.Pattern3, 2))
    }

    @Test
    fun `led color`() {
        account.updateNotificationSettings { it.copy(light = NotificationLight.Green) }
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.appearance.ledColor).isEqualTo(0xFF00FF00L.toInt())
    }

    private fun setLockScreenMode(mode: LockScreenNotificationVisibility) {
        K9.lockScreenNotificationVisibility = mode
    }

    private fun createNotificationData(senders: List<String> = emptyList()): NotificationData {
        val activeNotifications = senders.mapIndexed { index, sender ->
            NotificationHolder(
                notificationId = index,
                timestamp = 0L,
                content = NotificationContent(
                    messageReference = mock(),
                    sender = sender,
                    preview = "irrelevant",
                    summary = "irrelevant",
                    subject = "irrelevant",
                ),
            )
        }
        return NotificationData(account, activeNotifications, inactiveNotifications = emptyList())
    }

    private fun createAccount(): Account {
        return Account("00000000-0000-4000-0000-000000000000").apply {
            name = "account name"
            replaceIdentities(listOf(Identity()))
        }
    }
}
