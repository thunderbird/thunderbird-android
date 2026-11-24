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
                    accountManager.updateSync(account.id) { it.copy(notificationSettings = notificationSettings) }
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateNotificationSettings(account: LegacyAccount): NotificationSettings? {
        val (currentAccount, notificationConfiguration) = readNotificationConfiguration(account) ?: run {
            // There's no channel to read from. The account's settings stay the source of truth, and the regular
            // channel update recreates the missing channel from them in the background.
            notificationChannelManager.updateChannels()
            return null
        }
        return notificationConfigurationConverter.convert(currentAccount, notificationConfiguration)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readNotificationConfiguration(
        account: LegacyAccount,
    ): Pair<LegacyAccount, NotificationConfiguration>? {
        val notificationConfiguration = getNotificationConfiguration(account)
        if (notificationConfiguration != null) return account to notificationConfiguration

        // The channel may have been replaced by a newer version after `account` was loaded.
        val latestAccount = accountManager.findById(account.id)
            ?.takeIf { it.messagesNotificationChannelVersion != account.messagesNotificationChannelVersion }

        return latestAccount?.let { latest -> getNotificationConfiguration(latest)?.let { latest to it } }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getNotificationConfiguration(account: LegacyAccount): NotificationConfiguration? {
        return notificationChannelManager.getNotificationConfiguration(
            account.id,
            account.messagesNotificationChannelVersion,
        )
    }
}
