package com.fsck.k9.mailstore

import android.content.Context
import app.k9mail.legacy.di.DI
import java.util.concurrent.ConcurrentHashMap
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.LocalMessageUidPrefixProvider

class LocalStoreProvider {
    private val localStores = ConcurrentHashMap<AccountId, LocalStore>()
    private val accountLocks = ConcurrentHashMap<AccountId, Any>()

    /**
     * Retrieves an instance of [LocalStore] associated with the specified [AccountId].
     *
     * @param accountId The unique identifier of the account for which the [LocalStore] instance is to be retrieved.
     * @return An instance of [LocalStore] corresponding to the given [accountId], or `null` if no such instance exists.
     */
    @Throws(MessagingException::class)
    fun getInstance(accountId: AccountId): LocalStore {
        val context = DI.get(Context::class.java)
        val generalSettingsManager = DI.get(GeneralSettingsManager::class.java)
        val localMessageUidPrefixProvider = DI.get(LocalMessageUidPrefixProvider::class.java)

        return getInstanceById(accountId) {
            LocalStore.createInstance(accountId, context, generalSettingsManager, localMessageUidPrefixProvider)
        }
    }

    private fun getInstanceById(accountId: AccountId, create: () -> LocalStore): LocalStore {
        // Use per-account locks so DatabaseUpgradeService always knows which account database is currently upgraded.
        synchronized(accountLocks.getOrPut(accountId) { Any() }) {
            // Creating a LocalStore instance will create or upgrade the database if
            // necessary. This could take some time.
            return localStores.getOrPut(accountId) {
                create()
            }
        }
    }

    fun removeInstance(accountId: AccountId) {
        localStores.remove(accountId)
    }
}
