package com.fsck.k9.ui.settings.account

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import java.util.concurrent.ExecutorService

class AccountSettingsDataStoreFactory(
        private val context: Context,
        private val preferences: Preferences,
        private val executorService: ExecutorService
) {
    fun create(account: Account): AccountSettingsDataStore {
        return AccountSettingsDataStore(context, preferences, executorService, account)
    }
}
