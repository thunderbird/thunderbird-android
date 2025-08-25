package com.fsck.k9.notification

import android.os.Build
import androidx.annotation.RequiresApi
import com.fsck.k9.Preferences
import net.thunderbird.core.android.account.LegacyAccount

/**
 * Update accounts with notification settings read from their "Messages" `NotificationChannel`.
 */
class NotificationSettingsUpdater(
    private val preferences: Preferences,
    private val notificationChannelManager: NotificationChannelManager,
    private val notificationConfigurationConverter: NotificationConfigurationConverter,
) {
    fun updateNotificationSettings(accountUuids: Collection<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        accountUuids
            .mapNotNull { accountUuid -> preferences.getAccount(accountUuid) }
            .forEach { account ->
                updateNotificationSettings(account)
                preferences.saveAccount(account)
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateNotificationSettings(account: LegacyAccount) {
        val notificationConfiguration = notificationChannelManager.getNotificationConfiguration(account)
        val notificationSettings = notificationConfigurationConverter.convert(account, notificationConfiguration)

        if (notificationSettings != account.notificationSettings) {
            account.updateNotificationSettings { notificationSettings }
        }
    }
}
