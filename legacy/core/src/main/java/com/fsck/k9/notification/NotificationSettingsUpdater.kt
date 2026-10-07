package com.fsck.k9.notification

import android.os.Build
import androidx.annotation.RequiresApi
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.notification.NotificationSettings
import net.thunderbird.feature.account.AccountIdFactory

/**
 * Update accounts with notification settings read from their "Messages" `NotificationChannel`.
 */
class NotificationSettingsUpdater(
    private val notificationChannelManager: NotificationChannelManager,
    private val notificationConfigurationConverter: NotificationConfigurationConverter,
    private val accountManager: LegacyAccountManager,
) {
    fun updateNotificationSettings(accountUuids: Collection<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        accountUuids
            .mapNotNull { accountUuid -> accountManager.findById(AccountIdFactory.of(accountUuid)) }
            .forEach { account ->
                val notificationSettings = updateNotificationSettings(account)
                if (notificationSettings != null && notificationSettings != account.notificationSettings) {
                    accountManager.updateSync(account.copy(notificationSettings = notificationSettings))
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateNotificationSettings(account: LegacyAccount): NotificationSettings? {
        val notificationConfiguration = notificationChannelManager.getNotificationConfiguration(
            account.id,
            account.messagesNotificationChannelVersion,
        )
        return notificationConfigurationConverter.convert(account, notificationConfiguration)
    }
}
