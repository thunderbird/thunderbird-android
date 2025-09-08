package com.fsck.k9

import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import app.k9mail.legacy.di.DI
import com.fsck.k9.mailstore.LocalStoreProvider
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
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.UNASSIGNED_ACCOUNT_NUMBER
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.AccountRemovedListener
import net.thunderbird.core.android.account.AccountsChangeListener
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StoragePersister
import net.thunderbird.feature.account.storage.legacy.AccountDtoStorageHandler
import org.koin.java.KoinJavaComponent.inject

@Suppress("MaxLineLength")
class Preferences internal constructor(
    private val storagePersister: StoragePersister,
    private val localStoreProvider: LocalStoreProvider,
    private val legacyAccountStorageHandler: AccountDtoStorageHandler,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val accountDefaultsProvider: AccountDefaultsProvider,
) : AccountManager {

    private val generalSettingsManager: GeneralSettingsManager by inject(GeneralSettingsManager::class.java)
    private val accountLock = Any()
    private val storageLock = Any()

    @GuardedBy("accountLock")
    private var accountsMap: MutableMap<String, LegacyAccountDto>? = null

    @GuardedBy("accountLock")
    private var accountsInOrder = mutableListOf<LegacyAccountDto>()

    @GuardedBy("accountLock")
    private var newAccount: LegacyAccountDto? = null
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
            val accounts = mutableMapOf<String, LegacyAccountDto>()
            val accountsInOrder = mutableListOf<LegacyAccountDto>()

            val accountUuids = storage.getStringOrNull("accountUuids")
            if (!accountUuids.isNullOrEmpty()) {
                accountUuids.split(",").forEach { uuid ->
                    val existingAccount = accountsMap?.get(uuid)
                    val account = existingAccount ?: LegacyAccountDto(
                        uuid,
                        { generalSettingsManager.getConfig().debugging.isSensitiveLoggingEnabled },
                    )
                    legacyAccountStorageHandler.load(account, storage)

                    accounts[uuid] = account
                    accountsInOrder.add(account)
                    accountDefaultsProvider.applyOverwrites(account, storage)
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

    override fun getAccounts(): List<LegacyAccountDto> {
        synchronized(accountLock) {
            if (accountsMap == null) {
                loadAccounts()
            }

            return accountsInOrder.toList()
        }
    }

    private val completeAccounts: List<LegacyAccountDto>
        get() = getAccounts().filter { it.isFinishedSetup }

    override fun getAccount(accountUuid: String): LegacyAccountDto? {
        synchronized(accountLock) {
            if (accountsMap == null) {
                loadAccounts()
            }

            return accountsMap!![accountUuid]
        }
    }

    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccountDto> {
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
    override fun getAccountsFlow(): Flow<List<LegacyAccountDto>> {
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

    fun newAccount(): LegacyAccountDto {
        val accountUuid = UUID.randomUUID().toString()
        return newAccount(accountUuid)
    }

    fun newAccount(accountUuid: String): LegacyAccountDto {
        val account =
            LegacyAccountDto(accountUuid, { generalSettingsManager.getConfig().debugging.isSensitiveLoggingEnabled })
        accountDefaultsProvider.applyDefaults(account)

        synchronized(accountLock) {
            newAccount = account
            accountsMap!![account.uuid] = account
            accountsInOrder.add(account)
        }

        return account
    }

    fun deleteAccount(account: LegacyAccountDto) {
        synchronized(accountLock) {
            accountsMap?.remove(account.uuid)
            accountsInOrder.remove(account)

            val storageEditor = createStorageEditor()
            legacyAccountStorageHandler.delete(account, storage, storageEditor)
            storageEditor.commit()

            if (account === newAccount) {
                newAccount = null
            }
        }

        notifyAccountRemovedListeners(account)
        notifyAccountsChangeListeners()
    }

    val defaultAccount: LegacyAccountDto?
        get() = getAccounts().firstOrNull()

    override fun saveAccount(account: LegacyAccountDto) {
        ensureAssignedAccountNumber(account)
        processChangedValues(account)

        synchronized(accountLock) {
            val editor = createStorageEditor()
            legacyAccountStorageHandler.save(account, storage, editor)
            editor.commit()

            loadAccounts()
        }

        notifyAccountsChangeListeners()
    }

    private fun ensureAssignedAccountNumber(account: LegacyAccountDto) {
        if (account.accountNumber != UNASSIGNED_ACCOUNT_NUMBER) return

        account.accountNumber = generateAccountNumber()
    }

    private fun processChangedValues(account: LegacyAccountDto) {
        if (account.isChangedVisibleLimits) {
            try {
                localStoreProvider.getInstance(account).resetVisibleLimits(account.displayCount)
            } catch (e: MessagingException) {
                Log.e(e, "Failed to load LocalStore!")
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

    override fun moveAccount(account: LegacyAccountDto, newPosition: Int) {
        synchronized(accountLock) {
            val storageEditor = createStorageEditor()
            moveToPosition(account, storage, storageEditor, newPosition)
            storageEditor.commit()

            loadAccounts()
        }

        notifyAccountsChangeListeners()
    }

    private fun moveToPosition(account: LegacyAccountDto, storage: Storage, editor: StorageEditor, newPosition: Int) {
        val accountUuids = storage.getStringOrDefault("accountUuids", "").split(",").filter { it.isNotEmpty() }
        val oldPosition = accountUuids.indexOf(account.uuid)
        if (oldPosition == -1 || oldPosition == newPosition) return

        val newAccountUuidsString = accountUuids.toMutableList()
            .apply {
                removeAt(oldPosition)
                add(newPosition, account.uuid)
            }
            .joinToString(separator = ",")

        editor.putString("accountUuids", newAccountUuidsString)
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

    private fun notifyAccountRemovedListeners(account: LegacyAccountDto) {
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
