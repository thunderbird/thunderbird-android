package com.fsck.k9.storage.notifications

import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.notification.NotificationStore
import com.fsck.k9.notification.NotificationStoreProvider
import net.thunderbird.feature.account.AccountId

class K9NotificationStoreProvider(private val localStoreProvider: LocalStoreProvider) : NotificationStoreProvider {
    override fun getNotificationStore(accountId: AccountId): NotificationStore {
        val localStore = localStoreProvider.getInstance(accountId)
        return K9NotificationStore(lockableDatabase = localStore.database)
    }
}
