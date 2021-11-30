package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.K9
import com.fsck.k9.K9.LockScreenNotificationVisibility
import com.fsck.k9.NotificationSetting
import com.google.common.truth.Truth.assertThat
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
    fun `account name from description property`() {
        account.description = "description"
        account.email = "irrelevant@k9mail.example"
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.accountName).isEqualTo("description")
    }

    @Test
    fun `account description is blank`() {
        account.description = ""
        account.email = "test@k9mail.example"
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.accountName).isEqualTo("test@k9mail.example")
    }

    @Test
    fun `account description is null`() {
        account.description = null
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

        assertThat(result.lockScreenNotificationData).isInstanceOf(LockScreenNotificationData.SenderNames::class.java)
        val senderNamesData = result.lockScreenNotificationData as LockScreenNotificationData.SenderNames
        assertThat(senderNamesData.senderNames).isEqualTo("Sender Three, Sender Two, Sender One")
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
        account.notificationSetting.ringtone = "content://ringtone/1"
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.appearance.ringtone).isEqualTo("content://ringtone/1")
    }

    @Test
    fun `vibration pattern`() {
        account.notificationSetting.vibratePattern = 3
        account.notificationSetting.vibrateTimes = 2
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.appearance.vibrationPattern).isEqualTo(NotificationSetting.getVibration(3, 2))
    }

    @Test
    fun `led color`() {
        account.notificationSetting.ledColor = 0x00FF00
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.appearance.ledColor).isEqualTo(0x00FF00)
    }

    private fun setLockScreenMode(mode: LockScreenNotificationVisibility) {
        K9.lockScreenNotificationVisibility = mode
    }

    private fun createNotificationData(senders: List<String> = emptyList()): NotificationData {
        val notificationData = NotificationData(account)
        for (sender in senders) {
            notificationData.addNotificationContent(
                NotificationContent(
                    messageReference = mock(),
                    sender = sender,
                    preview = "irrelevant",
                    summary = "irrelevant",
                    subject = "irrelevant"
                ),
                timestamp = 0L
            )
        }
        return notificationData
    }

    private fun createAccount(): Account {
        return Account("00000000-0000-4000-0000-000000000000").apply {
            description = "account name"
            identities = listOf(Identity())
            notificationSetting.vibrateTimes = 1
        }
    }
}
