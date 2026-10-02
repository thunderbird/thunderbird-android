package com.fsck.k9.mailstore

import android.content.Context
import app.k9mail.legacy.di.DI
import java.util.concurrent.ConcurrentHashMap
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.legacy.mapper.LegacyAccountDataMapper
import net.thunderbird.feature.mail.message.list.LocalMessageUidPrefixProvider

class LocalStoreProvider {
    private val localStores = ConcurrentHashMap<AccountId, LocalStore>()
    private val accountLocks = ConcurrentHashMap<AccountId, Any>()

    @Throws(MessagingException::class)
    fun getInstance(account: LegacyAccountDto): LocalStore {
        val context = DI.get(Context::class.java)
        val generalSettingsManager = DI.get(GeneralSettingsManager::class.java)
        val localMessageUidPrefixProvider = DI.get(LocalMessageUidPrefixProvider::class.java)
        val accountId = account.id

        return getInstanceById(accountId) {
            LocalStore.createInstance(account, context, generalSettingsManager, localMessageUidPrefixProvider)
        }
    }

    @Throws(MessagingException::class)
    fun getInstanceByLegacyAccount(account: LegacyAccount): LocalStore {
        val context = DI.get(Context::class.java)
        val legacyAccountMapper = DI.get(LegacyAccountDataMapper::class.java)
        val generalSettingsManager = DI.get(GeneralSettingsManager::class.java)
        val localMessageUidPrefixProvider = DI.get(LocalMessageUidPrefixProvider::class.java)
        val accountId = account.id
        val accountDto = legacyAccountMapper.toDto(account)

        return getInstanceById(accountId) {
            LocalStore.createInstance(accountDto, context, generalSettingsManager, localMessageUidPrefixProvider)
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
