package net.thunderbird.app.common.account.data

import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.legacy.AccountStorageHandler

internal class LegacyAccountLocalDataSource(
    private val storageProvider: StorageProvider,
    private val accountStorageHandler: AccountStorageHandler,
    private val accountDefaultsProvider: AccountDefaultsProvider,
) : AccountLocalDataSource {

    private val storage: Storage
        get() = storageProvider.loadLatestStorage()

    override fun loadAll(): List<LegacyAccount> {
        val accountUuids = storage.getStringOrNull("accountUuids") ?: return emptyList()

        return accountUuids.split(",")
            .filter { it.isNotEmpty() }
            .map { uuid ->
                val accountId = AccountIdFactory.of(uuid)
                val account = accountStorageHandler.load(accountId, storage)
                accountDefaultsProvider.applyOverwrites(account, storage)
            }
    }

    override fun getById(accountId: AccountId): LegacyAccount? {
        val accountUuids = storage.getStringOrNull("accountUuids")?.split(",")
        if (accountUuids == null || accountId.toString() !in accountUuids) return null

        val account = accountStorageHandler.load(accountId, storage)
        return accountDefaultsProvider.applyOverwrites(account, storage)
    }

    override fun save(account: LegacyAccount) {
        val editor = storageProvider.createStorageEditor()
        accountStorageHandler.save(account, storage, editor)
        check(editor.commit()) { "Failed to save account settings" }
    }

    override fun delete(accountId: AccountId) {
        val editor = storageProvider.createStorageEditor()
        accountStorageHandler.delete(accountId, storage, editor)
        check(editor.commit()) { "Failed to delete account settings" }
    }
}
