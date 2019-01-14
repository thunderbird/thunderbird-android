package com.fsck.k9.ui.settings.account

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.job.K9JobManager
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.concurrent.ExecutorService

class AccountSettingsDataStoreFactory(
        private val preferences: Preferences,
        private val executorService: ExecutorService
): KoinComponent {
    private val jobManager: K9JobManager by inject()
    fun create(account: Account): AccountSettingsDataStore {
        return AccountSettingsDataStore(preferences, executorService, account, jobManager)
    }
}
