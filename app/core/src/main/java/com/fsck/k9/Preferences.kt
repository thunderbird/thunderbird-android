package com.fsck.k9

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.preferences.Storage
import com.fsck.k9.preferences.StorageEditor
import com.fsck.k9.preferences.StoragePersister
import java.util.HashMap
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

class Preferences internal constructor(
    private val context: Context,
    private val storagePersister: StoragePersister,
    private val localStoreProvider: LocalStoreProvider,
    private val localKeyStoreManager: LocalKeyStoreManager,
    private val accountPreferenceSerializer: AccountPreferenceSerializer
) : KoinComponent {
    private val backendManager: BackendManager by inject()

    private val accountLock = Any()

    @GuardedBy("accountLock")
    private var accountsMap: MutableMap<String, Account>? = null

    @GuardedBy("accountLock")
    private var accountsInOrder = mutableListOf<Account>()

    @GuardedBy("accountLock")
    private var newAccount: Account? = null
    private val accountsChangeListeners = CopyOnWriteArrayList<AccountsChangeListener>()

    val storage = Storage()

    init {
        val persistedStorageValues = storagePersister.loadValues()
        storage.replaceAll(persistedStorageValues)

        if (storage.isEmpty) {
            Timber.i("Preferences storage is zero-size, importing from Android-style preferences")

            val editor = createStorageEditor()
            editor.copy(context.getSharedPreferences("AndroidMail.Main", Context.MODE_PRIVATE))
            editor.commit()
        }
    }

    fun createStorageEditor(): StorageEditor {
        return storagePersister.createStorageEditor(storage)
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
                    val newAccount = Account(uuid)
                    accountPreferenceSerializer.loadAccount(newAccount, storage)

                    accounts[uuid] = newAccount
                    accountsInOrder.add(newAccount)
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

    val accounts: List<Account>
        get() {
            synchronized(accountLock) {
                if (accountsMap == null) {
                    loadAccounts()
                }

                return accountsInOrder.toList()
            }
        }

    val availableAccounts: Collection<Account>
        get() = accounts.filter { it.isAvailable(context) }

    fun getAccount(uuid: String): Account? {
        synchronized(accountLock) {
            if (accountsMap == null) {
                loadAccounts()
            }

            return accountsMap!![uuid]
        }
    }

    fun newAccount(): Account {
        val accountUuid = UUID.randomUUID().toString()
        val account = Account(accountUuid)
        accountPreferenceSerializer.loadDefaults(account)

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

            try {
                backendManager.removeBackend(account)
            } catch (e: Exception) {
                Timber.e(e, "Failed to reset remote store for account %s", account.uuid)
            }

            LocalStore.removeAccount(account)

            val storageEditor = createStorageEditor()
            accountPreferenceSerializer.delete(storageEditor, storage, account)
            storageEditor.commit()

            localKeyStoreManager.deleteCertificates(account)

            if (account === newAccount) {
                newAccount = null
            }
        }

        notifyListeners()
    }

    var defaultAccount: Account?
        get() {
            return getDefaultAccountOrNull() ?: availableAccounts.firstOrNull()?.also { newDefaultAccount ->
                defaultAccount = newDefaultAccount
            }
        }
        set(account) {
            requireNotNull(account)

            createStorageEditor()
                .putString("defaultAccountUuid", account.uuid)
                .commit()
        }

    private fun getDefaultAccountOrNull(): Account? {
        return synchronized(accountLock) {
            storage.getString("defaultAccountUuid", null)?.let { defaultAccountUuid ->
                getAccount(defaultAccountUuid)
            }
        }
    }

    fun saveAccount(account: Account) {
        ensureAssignedAccountNumber(account)
        processChangedValues(account)

        val editor = createStorageEditor()
        accountPreferenceSerializer.save(editor, storage, account)
        editor.commit()

        notifyListeners()
    }

    private fun ensureAssignedAccountNumber(account: Account) {
        if (account.accountNumber != Account.UNASSIGNED_ACCOUNT_NUMBER) return

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
        val accountNumbers = accounts.map { it.accountNumber }
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

    fun move(account: Account, up: Boolean) {
        synchronized(accountLock) {
            val storageEditor = createStorageEditor()
            accountPreferenceSerializer.move(storageEditor, account, storage, up)
            storageEditor.commit()

            loadAccounts()
        }

        notifyListeners()
    }

    private fun notifyListeners() {
        for (listener in accountsChangeListeners) {
            listener.onAccountsChanged()
        }
    }

    fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        accountsChangeListeners.add(accountsChangeListener)
    }

    fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        accountsChangeListeners.remove(accountsChangeListener)
    }

    companion object {
        @JvmStatic
        fun getPreferences(context: Context): Preferences {
            return DI.get(Preferences::class.java)
        }
    }
}
