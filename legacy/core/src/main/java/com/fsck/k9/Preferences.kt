package com.fsck.k9

import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.AccountDefaultsProvider
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.UNASSIGNED_ACCOUNT_NUMBER
import app.k9mail.legacy.account.AccountManager
import app.k9mail.legacy.account.AccountRemovedListener
import app.k9mail.legacy.account.AccountsChangeListener
import app.k9mail.legacy.di.DI
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.preferences.Storage
import com.fsck.k9.preferences.StorageEditor
import com.fsck.k9.preferences.StoragePersister
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

@Suppress("MaxLineLength")
class Preferences internal constructor(
    private val storagePersister: StoragePersister,
    private val localStoreProvider: LocalStoreProvider,
    private val accountPreferenceSerializer: AccountPreferenceSerializer,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val accountDefaultsProvider: AccountDefaultsProvider,
) : AccountManager {
    private val accountLock = Any()
    private val storageLock = Any()

    @GuardedBy("accountLock")
    private var accountsMap: MutableMap<String, Account>? = null

    @GuardedBy("accountLock")
    private var accountsInOrder = mutableListOf<Account>()

    @GuardedBy("accountLock")
    private var newAccount: Account? = null
    private val accountsChangeListeners = CopyOnWriteArraySet<AccountsChangeListener>()
    private val accountRemovedListeners = CopyOnWriteArraySet<AccountRemovedListener>()

    @GuardedBy("storageLock")
    private var currentStorage: Storage? = null

    val storage: Storage
        get() = synchronized(storageLock) {
            currentStorage ?: storagePersister.loadValues().also { newStorage ->
                currentStorage = newStorage
            }
        }

    fun createStorageEditor(): StorageEditor {
        return storagePersister.createStorageEditor { updater ->
            synchronized(storageLock) {
                currentStorage = updater(storage)
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    fun clearAccounts() {
        synchronized(accountLock) {
            accountsMap = HashMap()
            accountsInOrder = LinkedList()
        }
    }

    fun loadAccounts() {
        synchronized(accountLock) {
            val accounts = mutableMapOf<String, Account>()
            val accountsInOrder = mutableListOf<Account>()

            val accountUuids = storage.getString("accountUuids", null)
            if (!accountUuids.isNullOrEmpty()) {
                accountUuids.split(",").forEach { uuid ->
                    val existingAccount = accountsMap?.get(uuid)
                    val account = existingAccount ?: Account(uuid, K9::isSensitiveDebugLoggingEnabled)
                    accountPreferenceSerializer.loadAccount(account, storage)

                    accounts[uuid] = account
                    accountsInOrder.add(account)
                    accountDefaultsProvider.applyDefaults(account)
                }
            }

            newAccount?.takeIf { it.accountNumber != -1 }?.let { newAccount ->
                accounts[newAccount.uuid] = newAccount
                if (newAccount !in accountsInOrder) {
                    accountsInOrder.add(newAccount)
                }
                this.newAccount = null
            }

            this.accountsMap = accounts
            this.accountsInOrder = accountsInOrder
        }
    }

    override fun getAccounts(): List<Account> {
        synchronized(accountLock) {
            if (accountsMap == null) {
                loadAccounts()
            }

            return accountsInOrder.toList()
        }
    }

    private val completeAccounts: List<Account>
        get() = getAccounts().filter { it.isFinishedSetup }

    override fun getAccount(accountUuid: String): Account? {
        synchronized(accountLock) {
            if (accountsMap == null) {
                loadAccounts()
            }

            return accountsMap!![accountUuid]
        }
    }

    override fun getAccountFlow(accountUuid: String): Flow<Account> {
        return callbackFlow {
            val initialAccount = getAccount(accountUuid)
            if (initialAccount == null) {
                close()
                return@callbackFlow
            }

            send(initialAccount)

            val listener = AccountsChangeListener {
                val account = getAccount(accountUuid)
                if (account != null) {
                    trySendBlocking(account)
                } else {
                    close()
                }
            }
            addOnAccountsChangeListener(listener)

            awaitClose {
                removeOnAccountsChangeListener(listener)
            }
        }.buffer(capacity = Channel.CONFLATED)
            .flowOn(backgroundDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAccountsFlow(): Flow<List<Account>> {
        return callbackFlow {
            send(completeAccounts)

            val listener = AccountsChangeListener {
                trySendBlocking(completeAccounts)
            }
            addOnAccountsChangeListener(listener)

            awaitClose {
                removeOnAccountsChangeListener(listener)
            }
        }.buffer(capacity = Channel.CONFLATED)
            .flowOn(backgroundDispatcher)
    }

    fun newAccount(): Account {
        val accountUuid = UUID.randomUUID().toString()
        return newAccount(accountUuid)
    }

    fun newAccount(accountUuid: String): Account {
        val account = Account(accountUuid, K9::isSensitiveDebugLoggingEnabled)
        accountDefaultsProvider.applyDefaults(account)

        synchronized(accountLock) {
            newAccount = account
            accountsMap!![account.uuid] = account
            accountsInOrder.add(account)
        }

        return account
    }

    fun deleteAccount(account: Account) {
        synchronized(accountLock) {
            accountsMap?.remove(account.uuid)
            accountsInOrder.remove(account)

            val storageEditor = createStorageEditor()
            accountPreferenceSerializer.delete(storageEditor, storage, account)
            storageEditor.commit()

            if (account === newAccount) {
                newAccount = null
            }
        }

        notifyAccountRemovedListeners(account)
        notifyAccountsChangeListeners()
    }

    val defaultAccount: Account?
        get() = getAccounts().firstOrNull()

    override fun saveAccount(account: Account) {
        ensureAssignedAccountNumber(account)
        processChangedValues(account)

        synchronized(accountLock) {
            val editor = createStorageEditor()
            accountPreferenceSerializer.save(editor, storage, account)
            editor.commit()
        }

        notifyAccountsChangeListeners()
    }

    private fun ensureAssignedAccountNumber(account: Account) {
        if (account.accountNumber != UNASSIGNED_ACCOUNT_NUMBER) return

        account.accountNumber = generateAccountNumber()
    }

    private fun processChangedValues(account: Account) {
        if (account.isChangedVisibleLimits) {
            try {
                localStoreProvider.getInstance(account).resetVisibleLimits(account.displayCount)
            } catch (e: MessagingException) {
                Timber.e(e, "Failed to load LocalStore!")
            }
        }
        account.resetChangeMarkers()
    }

    fun generateAccountNumber(): Int {
        val accountNumbers = getAccounts().map { it.accountNumber }
        return findNewAccountNumber(accountNumbers)
    }

    private fun findNewAccountNumber(accountNumbers: List<Int>): Int {
        var newAccountNumber = -1
        for (accountNumber in accountNumbers.sorted()) {
            if (accountNumber > newAccountNumber + 1) {
                break
            }
            newAccountNumber = accountNumber
        }
        newAccountNumber++

        return newAccountNumber
    }

    override fun moveAccount(account: Account, newPosition: Int) {
        synchronized(accountLock) {
            val storageEditor = createStorageEditor()
            accountPreferenceSerializer.move(storageEditor, account, storage, newPosition)
            storageEditor.commit()

            loadAccounts()
        }

        notifyAccountsChangeListeners()
    }

    private fun notifyAccountsChangeListeners() {
        for (listener in accountsChangeListeners) {
            listener.onAccountsChanged()
        }
    }

    override fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        accountsChangeListeners.add(accountsChangeListener)
    }

    override fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        accountsChangeListeners.remove(accountsChangeListener)
    }

    private fun notifyAccountRemovedListeners(account: Account) {
        for (listener in accountRemovedListeners) {
            listener.onAccountRemoved(account)
        }
    }

    override fun addAccountRemovedListener(listener: AccountRemovedListener) {
        accountRemovedListeners.add(listener)
    }

    fun removeAccountRemovedListener(listener: AccountRemovedListener) {
        accountRemovedListeners.remove(listener)
    }

    companion object {
        @JvmStatic
        fun getPreferences(): Preferences {
            return DI.get()
        }
    }
}
