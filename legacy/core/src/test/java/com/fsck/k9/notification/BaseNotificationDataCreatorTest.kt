package com.fsck.k9.notification

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.fsck.k9.FakeLegacyAccount
import com.fsck.k9.mail.Address
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.preference.LockScreenNotificationVisibility
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.feature.notification.NotificationLight
import net.thunderbird.feature.notification.NotificationSettings
import net.thunderbird.feature.notification.NotificationVibration
import net.thunderbird.feature.notification.VibratePattern
import org.junit.Test
import org.mockito.kotlin.mock

class BaseNotificationDataCreatorTest {
    private val account = createAccount()
    private val notificationDataCreator = BaseNotificationDataCreator()

    @Test
    fun `account id`() {
        val notificationData = createNotificationData(
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.MESSAGE_COUNT,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.accountId).isEqualTo(account.id)
    }

    @Test
    fun `account name from name property`() {
        val account = createAccount(name = "name", email = "irrelevant@k9mail.example")
        val notificationData = createNotificationData(
            account = account,
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.MESSAGE_COUNT,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.accountName).isEqualTo("name")
    }

    @Test
    fun `account name is blank`() {
        val account = createAccount(name = "", email = "test@k9mail.example")
        val notificationData = createNotificationData(
            account = account,
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.MESSAGE_COUNT,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.accountName).isEqualTo("test@k9mail.example")
    }

    @Test
    fun `account name is null`() {
        val account = createAccount(name = null, email = "test@k9mail.example")
        val notificationData = createNotificationData(
            account = account,
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.MESSAGE_COUNT,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.accountName).isEqualTo("test@k9mail.example")
    }

    @Test
    fun `group key`() {
        val account = createAccount(accountNumber = 42)
        val notificationData = createNotificationData(
            account = account,
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.MESSAGE_COUNT,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.groupKey).isEqualTo("newMailNotifications-42")
    }

    @Test
    fun `notification color`() {
        val account = createAccount(color = 0xFF0000)
        val notificationData = createNotificationData(
            account = account,
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.MESSAGE_COUNT,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.color).isEqualTo(0xFF0000)
    }

    @Test
    fun `new messages count`() {
        val notificationData = createNotificationData(
            senders = listOf("irrelevant", "irrelevant"),
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.MESSAGE_COUNT,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.newMessagesCount).isEqualTo(2)
    }

    @Test
    fun `do not display notification on lock screen`() {
        val notificationData = createNotificationData(
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.NOTHING,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isEqualTo(LockScreenNotificationData.None)
    }

    @Test
    fun `display application name on lock screen`() {
        val notificationData = createNotificationData(
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.APP_NAME,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isEqualTo(LockScreenNotificationData.AppName)
    }

    @Test
    fun `display new message count on lock screen`() {
        val notificationData = createNotificationData(
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.MESSAGE_COUNT,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isEqualTo(LockScreenNotificationData.MessageCount)
    }

    @Test
    fun `display message sender names on lock screen`() {
        val notificationData = createNotificationData(
            senders = listOf("Sender One", "Sender Two", "Sender Three"),
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.SENDERS,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isInstanceOf<LockScreenNotificationData.SenderNames>()
        val senderNamesData = result.lockScreenNotificationData as LockScreenNotificationData.SenderNames
        assertThat(senderNamesData.senderNames)
            .isEqualTo("Sender One <irrelevant>, Sender Two <irrelevant>, Sender Three <irrelevant>")
    }

    @Test
    fun `display notification on lock screen`() {
        val notificationData = createNotificationData(
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.EVERYTHING,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isEqualTo(LockScreenNotificationData.Public)
    }

    @Test
    fun ringtone() {
        val account = this.account.copy(
            notificationSettings = this.account.notificationSettings.copy(ringtone = "content://ringtone/1"),
        )
        val notificationData = createNotificationData(
            account = account,
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.MESSAGE_COUNT,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.appearance.ringtone).isEqualTo("content://ringtone/1")
    }

    @Test
    fun `vibration pattern`() {
        val account = this.account.copy(
            notificationSettings = this.account.notificationSettings.copy(
                vibration = NotificationVibration(
                    isEnabled = true,
                    pattern = VibratePattern.Pattern3,
                    repeatCount = 2,
                ),
            ),
        )
        val notificationData = createNotificationData(
            account = account,
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.MESSAGE_COUNT,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.appearance.vibrationPattern).isNotNull()
            .isEqualTo(
                NotificationVibration.getSystemPattern(
                    VibratePattern.Pattern3,
                    2,
                ),
            )
    }

    @Test
    fun `led color`() {
        val account = this.account.copy(
            notificationSettings = this.account.notificationSettings.copy(
                light = NotificationLight.Green,
            ),
        )
        val notificationData = createNotificationData(
            account = account,
            lockScreenNotificationVisibility = LockScreenNotificationVisibility.MESSAGE_COUNT,
        )

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.appearance.ledColor).isEqualTo(0xFF00FF00L.toInt())
    }

    private fun createNotificationData(
        account: LegacyAccount = this.account,
        senders: List<String> = emptyList(),
        lockScreenNotificationVisibility: LockScreenNotificationVisibility,
    ): NotificationData {
        val activeNotifications = senders.mapIndexed { index, sender ->
            NotificationHolder(
                notificationId = index,
                timestamp = 0L,
                content = NotificationContent(
                    messageReference = mock(),
                    sender = Address("irrelevant", sender),
                    preview = "irrelevant",
                    summary = "irrelevant",
                    subject = "irrelevant",
                ),
            )
        }
        return NotificationData(
            account = account,
            activeNotifications = activeNotifications,
            inactiveNotifications = emptyList(),
            lockScreenNotificationVisibility = lockScreenNotificationVisibility,
        )
    }

    private fun createAccount(
        name: String? = "account name",
        email: String = "test@k9mail.example",
        accountNumber: Int = 1,
        color: Int = -1,
        notificationSettings: NotificationSettings = NotificationSettings(),
    ): LegacyAccount {
        val id = AccountIdFactory.create()
        return FakeLegacyAccount.ACCOUNT.copy(
            id = id,
            name = name,
            email = email,
            accountNumber = accountNumber,
            profile = ProfileDto(
                id = id,
                name = name ?: "",
                color = color,
                avatar = AvatarDto(
                    id = id,
                    avatarType = AvatarTypeDto.MONOGRAM,
                    avatarMonogram = "AN",
                    avatarImageUri = null,
                    avatarIconName = null,
                ),
            ),
            notificationSettings = notificationSettings,
        )
    }
}
