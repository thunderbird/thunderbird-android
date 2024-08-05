package com.fsck.k9.ui.settings.account

import app.k9mail.legacy.account.Account
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.job.K9JobManager
import com.fsck.k9.notification.NotificationChannelManager
import com.fsck.k9.notification.NotificationController
import java.util.concurrent.ExecutorService

class AccountSettingsDataStoreFactory(
    private val preferences: Preferences,
    private val jobManager: K9JobManager,
    private val executorService: ExecutorService,
    private val notificationChannelManager: NotificationChannelManager,
    private val notificationController: NotificationController,
    private val messagingController: MessagingController,
) {
    fun create(account: Account): AccountSettingsDataStore {
        return AccountSettingsDataStore(
            preferences,
            executorService,
            account,
            jobManager,
            notificationChannelManager,
            notificationController,
            messagingController,
        )
    }
}
