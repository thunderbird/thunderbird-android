package com.fsck.k9.storage.notifications

import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.notification.NotificationStore
import com.fsck.k9.notification.NotificationStoreProvider
import net.thunderbird.core.android.account.LegacyAccount

class K9NotificationStoreProvider(private val localStoreProvider: LocalStoreProvider) : NotificationStoreProvider {
    override fun getNotificationStore(account: LegacyAccount): NotificationStore {
        val localStore = localStoreProvider.getInstance(account)
        return K9NotificationStore(lockableDatabase = localStore.database)
    }
}
