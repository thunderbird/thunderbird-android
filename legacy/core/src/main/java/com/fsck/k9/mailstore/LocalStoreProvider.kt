package com.fsck.k9.mailstore

import android.content.Context
import app.k9mail.legacy.di.DI
import com.fsck.k9.Account
import com.fsck.k9.mail.MessagingException
import java.util.concurrent.ConcurrentHashMap

class LocalStoreProvider {
    private val localStores = ConcurrentHashMap<String, LocalStore>()
    private val accountLocks = ConcurrentHashMap<String, Any>()

    @Throws(MessagingException::class)
    fun getInstance(account: Account): LocalStore {
        val context = DI.get(Context::class.java)
        val accountUuid = account.uuid

        // Use per-account locks so DatabaseUpgradeService always knows which account database is currently upgraded.
        synchronized(accountLocks.getOrPut(accountUuid) { Any() }) {
            // Creating a LocalStore instance will create or upgrade the database if
            // necessary. This could take some time.
            return localStores.getOrPut(accountUuid) { LocalStore.createInstance(account, context) }
        }
    }

    fun removeInstance(account: Account) {
        val accountUuid = account.uuid
        localStores.remove(accountUuid)
    }
}
