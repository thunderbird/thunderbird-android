package com.fsck.k9.mailstore

import android.content.Context
import app.k9mail.legacy.di.DI
import com.fsck.k9.mail.MessagingException
import java.util.concurrent.ConcurrentHashMap
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.preference.GeneralSettingsManager

class LocalStoreProvider {
    private val localStores = ConcurrentHashMap<String, LocalStore>()
    private val accountLocks = ConcurrentHashMap<String, Any>()

    @Throws(MessagingException::class)
    fun getInstance(account: LegacyAccount): LocalStore {
        val context = DI.get(Context::class.java)
        val generalSettingsManager = DI.get(GeneralSettingsManager::class.java)
        val accountUuid = account.uuid

        // Use per-account locks so DatabaseUpgradeService always knows which account database is currently upgraded.
        synchronized(accountLocks.getOrPut(accountUuid) { Any() }) {
            // Creating a LocalStore instance will create or upgrade the database if
            // necessary. This could take some time.
            return localStores.getOrPut(accountUuid) {
                LocalStore.createInstance(account, context, generalSettingsManager)
            }
        }
    }

    fun removeInstance(account: LegacyAccount) {
        val accountUuid = account.uuid
        localStores.remove(accountUuid)
    }
}
