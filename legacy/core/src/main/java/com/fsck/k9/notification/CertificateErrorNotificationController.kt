package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.account.AccountId

internal open class CertificateErrorNotificationController(
    private val notificationHelper: NotificationHelper,
    private val actionCreator: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider,
    private val generalSettingsManager: GeneralSettingsManager,
    private val accountManager: LegacyAccountManager,
    private val notificationIdRegistry: AccountNotificationIdRegistry,
) {
    fun showCertificateErrorNotification(accountId: AccountId, incoming: Boolean) {
        val account = accountManager.findById(accountId) ?: return

        val notificationId = getNotificationId(accountId, incoming)
        val editServerSettingsPendingIntent = createContentIntent(account, incoming)
        val title = resourceProvider.certificateErrorTitle(account.profile.name)
        val text = resourceProvider.certificateErrorBody()

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(
                account.id,
                NotificationChannelManager.ChannelType.MISCELLANEOUS,
                account.messagesNotificationChannelVersion,
            )
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.profile.color)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(editServerSettingsPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPublicVersion(createLockScreenNotification(account))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setErrorAppearance(generalSettingsManager = generalSettingsManager)

        notificationHelper.notify(notificationId, notificationBuilder.build())
    }

    fun clearCertificateErrorNotifications(accountId: AccountId, incoming: Boolean) {
        val notificationId = getNotificationId(accountId, incoming)
        notificationManager.cancel(notificationId)
    }

    private fun getNotificationId(accountId: AccountId, incoming: Boolean): Int {
        val kind = if (incoming) {
            AccountNotificationKind.CertificateErrorIncoming
        } else {
            AccountNotificationKind.CertificateErrorOutgoing
        }
        return notificationIdRegistry.getOrAllocate(accountId, kind)
    }

    protected open fun createContentIntent(account: LegacyAccount, incoming: Boolean): PendingIntent {
        return if (incoming) {
            actionCreator.getEditIncomingServerSettingsIntent(account)
        } else {
            actionCreator.getEditOutgoingServerSettingsIntent(account)
        }
    }

    private fun createLockScreenNotification(account: LegacyAccount): Notification {
        return notificationHelper
            .createNotificationBuilder(
                account.id,
                NotificationChannelManager.ChannelType.MISCELLANEOUS,
                account.messagesNotificationChannelVersion,
            )
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.profile.color)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(resourceProvider.certificateErrorTitle())
            .build()
    }

    private val notificationManager: NotificationManagerCompat
        get() = notificationHelper.getNotificationManager()
}
