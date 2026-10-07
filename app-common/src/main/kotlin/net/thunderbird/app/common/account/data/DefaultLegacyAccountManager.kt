package net.thunderbird.app.common.account.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.UNASSIGNED_ACCOUNT_NUMBER
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

@Suppress("TooManyFunctions")
internal class DefaultLegacyAccountManager(
    private val accountStorage: AccountLocalDataSource,
    private val accountCache: LegacyAccountCache,
    private val accountDisplayOrderManager: AccountDisplayOrderManager,
    private val accountFolderUpdater: AccountFolderUpdater,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LegacyAccountManager {

    override fun observeAll(): Flow<List<LegacyAccount>> = accountCache.observeAll()
        .onStart { synchronized(accountLock) { loadAccountsIfNeeded() } }
        .map { accounts -> accounts.filter { it.isFinishedSetup } }
        .distinctUntilChanged()
        .flowOn(backgroundDispatcher)

    override fun observeById(accountId: AccountId): Flow<LegacyAccount?> =
        accountCache.observeAll()
            .onStart { synchronized(accountLock) { loadAccountsIfNeeded() } }
            .map { accounts -> accounts.firstOrNull { it.id == accountId } }
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    private val accountLock = Any()
    private var accountsLoaded = false

    override fun findAll(): List<LegacyAccount> = synchronized(accountLock) {
        loadAccountsIfNeeded()
        accountCache.findAll()
    }

    override fun findById(accountId: AccountId): LegacyAccount? = synchronized(accountLock) {
        loadAccountsIfNeeded()
        accountCache.findById(accountId)
    }

    private fun loadAccountsIfNeeded() {
        if (!accountsLoaded) {
            accountCache.updateAll(accountStorage.loadAll())
            accountsLoaded = true
        }
    }

    override fun moveAccount(accountId: AccountId, newPosition: Int) = synchronized(accountLock) {
        loadAccountsIfNeeded()
        accountDisplayOrderManager.moveToPosition(accountId, newPosition)
        accountCache.updateAll(accountStorage.loadAll())
    }

    override suspend fun update(account: LegacyAccount) = updateSync(account)

    override fun updateSync(account: LegacyAccount) = synchronized(accountLock) {
        loadAccountsIfNeeded()
        val previousAccount = accountCache.findById(account.id)
        val assignedAccount = ensureAssignedAccountNumber(account)
        if (previousAccount != null && previousAccount.displayCount != assignedAccount.displayCount) {
            accountFolderUpdater.resetVisibleLimits(account.id, assignedAccount.displayCount)
        }
        accountStorage.save(assignedAccount)
        accountCache.update(assignedAccount)
    }

    fun delete(accountId: AccountId) = synchronized(accountLock) {
        loadAccountsIfNeeded()
        accountStorage.delete(accountId)
        accountCache.delete(accountId)
    }

    private fun ensureAssignedAccountNumber(account: LegacyAccount): LegacyAccount {
        return if (account.accountNumber != UNASSIGNED_ACCOUNT_NUMBER) {
            account
        } else {
            val newNumber = findNewAccountNumber(accountCache.findAll().map { it.accountNumber })
            account.copy(accountNumber = newNumber)
        }
    }

    private fun findNewAccountNumber(accountNumbers: List<Int>): Int {
        var newAccountNumber = 0
        while (accountNumbers.contains(newAccountNumber)) {
            newAccountNumber++
        }
        return newAccountNumber
    }
}
