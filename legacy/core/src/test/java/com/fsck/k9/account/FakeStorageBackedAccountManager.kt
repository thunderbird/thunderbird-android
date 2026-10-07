package com.fsck.k9.account

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.preference.storage.StorageProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.legacy.AccountStorageHandler

/** Loads accounts written to the test's preference storage instead of returning unstubbed mock results. */
class FakeStorageBackedAccountManager(
    private val storageProvider: StorageProvider,
    private val accountStorageHandler: AccountStorageHandler,
    private val accountDefaultsProvider: AccountDefaultsProvider,
) : LegacyAccountManager {
    override fun findAll(): List<LegacyAccount> {
        val storage = storageProvider.loadLatestStorage()
        return storage.getStringOrNull("accountUuids")
            ?.split(',')
            ?.filter { it.isNotEmpty() }
            ?.map { accountId -> load(AccountIdFactory.of(accountId)) }
            ?: emptyList()
    }

    override fun findById(accountId: AccountId): LegacyAccount? =
        findAll().firstOrNull { it.id == accountId }

    override fun observeAll(): Flow<List<LegacyAccount>> = flowOf(findAll())

    override fun observeById(accountId: AccountId): Flow<LegacyAccount?> = flowOf(findById(accountId))

    override fun moveAccount(accountId: AccountId, newPosition: Int) = error("Not supported by this fake")

    override suspend fun update(account: LegacyAccount) = updateSync(account)

    override fun updateSync(account: LegacyAccount) {
        val editor = storageProvider.createStorageEditor()
        accountStorageHandler.save(account, storageProvider.storage, editor)
        check(editor.commit())
    }

    private fun load(accountId: AccountId): LegacyAccount {
        val storage = storageProvider.storage
        val account = accountStorageHandler.load(accountId, storage)
        return accountDefaultsProvider.applyOverwrites(account, storage)
    }
}
