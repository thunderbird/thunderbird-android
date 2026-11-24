package net.thunderbird.app.common.account.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
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
        .flowOn(backgroundDispatcher)
        .distinctUntilChanged()

    override fun observeById(accountId: AccountId): Flow<LegacyAccount?> =
        accountCache.observeAll()
            .onStart { synchronized(accountLock) { loadAccountsIfNeeded() } }
            .map { accounts -> accounts.firstOrNull { it.id == accountId } }
            .flowOn(backgroundDispatcher)
            .distinctUntilChanged()

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

    override suspend fun create(account: LegacyAccount) = withContext(backgroundDispatcher) {
        createSync(account)
    }

    override fun createSync(account: LegacyAccount) {
        saveAccount(account.id) { existingAccount ->
            require(existingAccount == null) { "Account already exists: ${account.id}" }
            account
        }
    }

    override suspend fun update(
        accountId: AccountId,
        transform: (LegacyAccount) -> LegacyAccount,
    ): LegacyAccount? = withContext(backgroundDispatcher) {
        updateSync(accountId, transform)
    }

    override fun updateSync(accountId: AccountId, transform: (LegacyAccount) -> LegacyAccount): LegacyAccount? {
        return saveAccount(accountId) { currentAccount -> currentAccount?.let(transform) }
    }

    /**
     * Saves the account returned by [createAccount], which receives the cached account (or `null` if there is none).
     *
     * @return The saved account, or `null` if [createAccount] returned `null`.
     */
    private fun saveAccount(
        accountId: AccountId,
        createAccount: (LegacyAccount?) -> LegacyAccount?,
    ): LegacyAccount? {
        val (savedAccount, isDisplayCountChanged) = synchronized(accountLock) {
            loadAccountsIfNeeded()
            val previousAccount = accountCache.findById(accountId)
            val account = createAccount(previousAccount) ?: return null
            require(account.id == accountId) { "Account ID must not change: $accountId -> ${account.id}" }

            val assignedAccount = ensureAssignedAccountNumber(account)
            accountStorage.save(assignedAccount)
            // Cache what was stored, not what was passed in, so the in-memory account matches storage
            val storedAccount = checkNotNull(accountStorage.getById(assignedAccount.id)) {
                "Account ${assignedAccount.id} was not found right after saving it"
            }
            accountCache.update(storedAccount)

            storedAccount to (previousAccount != null && previousAccount.displayCount != storedAccount.displayCount)
        }

        // Must run outside accountLock: this opens the message database, whose migrations call findById().
        // Holding the lock here can deadlock with a thread that holds the database lock.
        if (isDisplayCountChanged) {
            resetVisibleLimits(accountId)
        }

        return savedAccount
    }

    private fun resetVisibleLimits(accountId: AccountId) {
        val displayCount = findById(accountId)?.displayCount ?: return
        accountFolderUpdater.resetVisibleLimits(accountId, displayCount)
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
