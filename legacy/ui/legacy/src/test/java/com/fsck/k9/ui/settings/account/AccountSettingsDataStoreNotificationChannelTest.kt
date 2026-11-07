package com.fsck.k9.ui.settings.account

import android.app.NotificationManager
import android.content.Context
import androidx.core.net.toUri
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.fsck.k9.FakeLegacyAccount
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.notification.K9NotificationResourceProvider
import com.fsck.k9.notification.NotificationChannelManager
import com.fsck.k9.notification.NotificationLightDecoder
import kotlinx.coroutines.test.TestScope
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.feature.notification.NotificationSettings
import org.junit.Test
import org.mockito.kotlin.mock
import org.robolectric.RuntimeEnvironment

class AccountSettingsDataStoreNotificationChannelTest : K9RobolectricTest() {
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
        resourceProvider = K9NotificationResourceProvider(context),
        notificationLightDecoder = NotificationLightDecoder(),
        // Never advanced, so channels are only updated when the test asks for it
        coroutineScope = TestScope(),
    )

    private val testSubject = AccountSettingsDataStore(
        accountManager = accountManager,
        executorService = DirectExecutorService(),
        account = account,
        jobManager = mock(),
        notificationChannelManager = notificationChannelManager,
        notificationController = mock(),
        messagingController = mock(),
        logger = TestLogger(),
    )

    @Test
    fun `changing the ringtone should recreate the messages channel with the new ringtone`() {
        // Arrange
        notificationChannelManager.updateChannels()

        // Act
        testSubject.putString("account_ringtone", NEW_RINGTONE)

        // Assert
        val storedAccount = accountManager.findById(account.id)
        assertThat(storedAccount).isNotNull().all {
            prop(LegacyAccount::messagesNotificationChannelVersion).isEqualTo(1)
            prop("ringtone") { it.notificationSettings.ringtone }.isEqualTo(NEW_RINGTONE)
        }
        assertThat(notificationManager.getNotificationChannel(messagesChannelId(version = 1)))
            .isNotNull()
            .prop("sound") { it.sound }
            .isEqualTo(NEW_RINGTONE.toUri())
        assertThat(notificationManager.getNotificationChannel(messagesChannelId(version = 0))).isNull()
    }

    @Test
    fun `saving another setting after recreating the channel should keep the new channel version`() {
        // Arrange
        notificationChannelManager.updateChannels()
        testSubject.putString("account_ringtone", NEW_RINGTONE)

        // Act
        testSubject.putBoolean("always_show_cc_bcc", true)

        // Assert
        val storedAccount = accountManager.findById(account.id)
        assertThat(storedAccount).isNotNull().all {
            prop(LegacyAccount::messagesNotificationChannelVersion).isEqualTo(1)
            prop(LegacyAccount::isAlwaysShowCcBcc).isTrue()
        }
    }

    private fun messagesChannelId(version: Int): String {
        val suffix = if (version == 0) "" else "_$version"
        return "messages_channel_${account.id}$suffix"
    }

    private companion object {
        const val OLD_RINGTONE = "content://settings/system/notification_sound"
        const val NEW_RINGTONE = "content://media/internal/audio/media/42"
    }
}
