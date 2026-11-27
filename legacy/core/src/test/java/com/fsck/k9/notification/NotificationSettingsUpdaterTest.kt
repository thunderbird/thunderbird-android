package com.fsck.k9.notification

import android.app.NotificationManager
import android.content.Context
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import com.fsck.k9.FakeLegacyAccount
import com.fsck.k9.K9RobolectricTest
import kotlinx.coroutines.test.TestScope
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.feature.notification.NotificationSettings
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class NotificationSettingsUpdaterTest : K9RobolectricTest() {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    private val account = FakeLegacyAccount.create().copy(
        notificationSettings = NotificationSettings(isRingEnabled = true, ringtone = OLD_RINGTONE),
    )
    private val accountManager = FakeLegacyAccountManager(listOf(account))

    private val notificationChannelManager = NotificationChannelManager(
        accountManager = accountManager,
        backgroundExecutor = { it.run() },
        notificationManager = notificationManager,
        resourceProvider = TestNotificationResourceProvider(),
        notificationLightDecoder = NotificationLightDecoder(),
        // Never advanced, so channels are only updated when the test asks for it
        coroutineScope = TestScope(),
    )

    private val testSubject = NotificationSettingsUpdater(
        notificationChannelManager = notificationChannelManager,
        notificationConfigurationConverter = NotificationConfigurationConverter(
            notificationLightDecoder = NotificationLightDecoder(),
            notificationVibrationDecoder = NotificationVibrationDecoder(),
        ),
        accountManager = accountManager,
    )

    @Test
    fun `existing channel should be read`() {
        // Arrange
        notificationChannelManager.updateChannels()

        // Act
        val result = testSubject.updateNotificationSettings(account)

        // Assert
        assertThat(result).isNotNull()
            .prop(NotificationSettings::ringtone)
            .isEqualTo(OLD_RINGTONE)
        assertThat(notificationManager.getNotificationChannel(messagesChannelId(version = 1))).isNull()
    }

    @Test
    fun `channel replaced by a newer version should be read from the latest account`() {
        // Arrange
        val staleAccount = account
        accountManager.updateSync(account.id) {
            it.copy(
                messagesNotificationChannelVersion = 1,
                notificationSettings = it.notificationSettings.copy(ringtone = NEW_RINGTONE),
            )
        }
        notificationChannelManager.updateChannels()
        assertThat(notificationManager.getNotificationChannel(messagesChannelId(version = 0))).isNull()

        // Act
        val result = testSubject.updateNotificationSettings(staleAccount)

        // Assert
        assertThat(result).isNotNull()
            .prop(NotificationSettings::ringtone)
            .isEqualTo(NEW_RINGTONE)
    }

    @Test
    fun `missing channel should return null and recreate the channel from the account settings`() {
        // Arrange
        assertThat(notificationManager.getNotificationChannel(messagesChannelId(version = 0))).isNull()

        // Act
        val result = testSubject.updateNotificationSettings(account)

        // Assert
        assertThat(result).isNull()
        assertThat(accountManager.findById(account.id)).isNotNull()
            .prop(LegacyAccount::notificationSettings)
            .isEqualTo(account.notificationSettings)
        assertThat(notificationManager.getNotificationChannel(messagesChannelId(version = 0)))
            .isNotNull()
            .prop("sound") { it.sound?.toString() }
            .isEqualTo(OLD_RINGTONE)
    }

    private fun messagesChannelId(version: Int): String {
        return notificationChannelManager.getChannelIdFor(
            account.id,
            NotificationChannelManager.ChannelType.MESSAGES,
            version,
        )
    }

    private companion object {
        const val OLD_RINGTONE = "content://settings/system/notification_sound"
        const val NEW_RINGTONE = "content://media/internal/audio/media/42"
    }
}
