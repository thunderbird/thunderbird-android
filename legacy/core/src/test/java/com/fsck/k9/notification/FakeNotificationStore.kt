package com.fsck.k9.notification

import net.thunderbird.feature.account.AccountId

internal class FakeNotificationStoreProvider : NotificationStoreProvider {
    private val notificationStores = mutableMapOf<AccountId, FakeNotificationStore>()

    override fun getNotificationStore(accountId: AccountId): FakeNotificationStore {
        return notificationStores.getOrPut(accountId) { FakeNotificationStore() }
    }
}

internal class FakeNotificationStore : NotificationStore {
    val operations = mutableListOf<NotificationStoreOperation>()
    var isCleared = false
        private set

    override fun persistNotificationChanges(operations: List<NotificationStoreOperation>) {
        this.operations.addAll(operations)
    }

    override fun clearNotifications() {
        isCleared = true
    }
}
