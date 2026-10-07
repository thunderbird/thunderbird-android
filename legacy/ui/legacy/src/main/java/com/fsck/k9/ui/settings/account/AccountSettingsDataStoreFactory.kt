package com.fsck.k9.ui.settings.account

import com.fsck.k9.controller.MessagingController
import com.fsck.k9.job.K9JobManager
import com.fsck.k9.notification.NotificationChannelManager
import com.fsck.k9.notification.NotificationController
import java.util.concurrent.ExecutorService
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager

class AccountSettingsDataStoreFactory(
    private val accountManager: LegacyAccountManager,
    private val jobManager: K9JobManager,
    private val executorService: ExecutorService,
    private val notificationChannelManager: NotificationChannelManager,
    private val notificationController: NotificationController,
    private val messagingController: MessagingController,
) {
    fun create(account: LegacyAccount): AccountSettingsDataStore {
        return AccountSettingsDataStore(
            accountManager,
            executorService,
            account,
            jobManager,
            notificationChannelManager,
            notificationController,
            messagingController,
        )
    }
}
