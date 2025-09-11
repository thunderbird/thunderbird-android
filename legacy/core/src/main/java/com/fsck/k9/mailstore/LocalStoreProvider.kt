package com.fsck.k9.mailstore

import android.content.Context
import app.k9mail.legacy.di.DI
import java.util.concurrent.ConcurrentHashMap
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.account.storage.legacy.mapper.LegacyAccountDataMapper

class LocalStoreProvider {
    private val localStores = ConcurrentHashMap<String, LocalStore>()
    private val accountLocks = ConcurrentHashMap<String, Any>()

    @Throws(MessagingException::class)
    fun getInstance(account: LegacyAccountDto): LocalStore {
        val context = DI.get(Context::class.java)
        val generalSettingsManager = DI.get(GeneralSettingsManager::class.java)
        val accountUuid = account.uuid

        return getInstanceById(accountUuid) {
            LocalStore.createInstance(account, context, generalSettingsManager)
        }
    }

    @Throws
    fun getInstanceByLegacyAccount(account: LegacyAccount): LocalStore {
        val context = DI.get(Context::class.java)
        val legacyAccountMapper = DI.get(LegacyAccountDataMapper::class.java)
        val generalSettingsManager = DI.get(GeneralSettingsManager::class.java)
        val accountUuid = account.uuid
        val accountDto = legacyAccountMapper.toDto(account)

        return getInstanceById(accountUuid) {
            LocalStore.createInstance(accountDto, context, generalSettingsManager)
        }
    }

    private fun getInstanceById(uuid: String, create: () -> LocalStore): LocalStore {
        // Use per-account locks so DatabaseUpgradeService always knows which account database is currently upgraded.
        synchronized(accountLocks.getOrPut(uuid) { Any() }) {
            // Creating a LocalStore instance will create or upgrade the database if
            // necessary. This could take some time.
            return localStores.getOrPut(uuid) {
                create()
            }
        }
    }

    fun removeInstance(uuid: String) {
        localStores.remove(uuid)
    }
}
