package com.fsck.k9.controller.push

import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.Preferences
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.network.ConnectivityChangeListener
import com.fsck.k9.network.ConnectivityManager
import com.fsck.k9.notification.PushNotificationManager
import com.fsck.k9.notification.PushNotificationState
import com.fsck.k9.notification.PushNotificationState.LISTENING
import com.fsck.k9.notification.PushNotificationState.WAIT_BACKGROUND_SYNC
import com.fsck.k9.notification.PushNotificationState.WAIT_NETWORK
import com.fsck.k9.preferences.BackgroundSync
import com.fsck.k9.preferences.GeneralSettingsManager
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Starts and stops [AccountPushController]s as necessary. Manages the Push foreground service.
 */
class PushController internal constructor(
    private val preferences: Preferences,
    private val generalSettingsManager: GeneralSettingsManager,
    private val backendManager: BackendManager,
    private val pushServiceManager: PushServiceManager,
    private val bootCompleteManager: BootCompleteManager,
    private val autoSyncManager: AutoSyncManager,
    private val pushNotificationManager: PushNotificationManager,
    private val connectivityManager: ConnectivityManager,
    private val accountPushControllerFactory: AccountPushControllerFactory,
    private val coroutineScope: CoroutineScope = GlobalScope,
    private val coroutineDispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
) {
    private val lock = Any()
    private var initializationStarted = false
    private val pushers = mutableMapOf<String, AccountPushController>()

    private val autoSyncListener = AutoSyncListener(::onAutoSyncChanged)
    private val connectivityChangeListener = object : ConnectivityChangeListener {
        override fun onConnectivityChanged() = this@PushController.onConnectivityChanged()
        override fun onConnectivityLost() = this@PushController.onConnectivityLost()
    }

    /**
     * Initialize [PushController].
     *
     * Only call this method in situations where starting a foreground service is allowed.
     * See https://developer.android.com/about/versions/12/foreground-services
     */
    fun init() {
        synchronized(lock) {
            if (initializationStarted) {
                return
            }
            initializationStarted = true
        }

        coroutineScope.launch(coroutineDispatcher) {
            initInBackground()
        }
    }

    fun disablePush() {
        Timber.v("PushController.disablePush()")

        coroutineScope.launch(coroutineDispatcher) {
            for (account in preferences.accounts) {
                account.folderPushMode = FolderMode.NONE
                preferences.saveAccount(account)
            }
        }
    }

    private fun initInBackground() {
        Timber.v("PushController.initInBackground()")

        preferences.addOnAccountsChangeListener(::onAccountsChanged)
        listenForBackgroundSyncChanges()
        backendManager.addListener(::onBackendChanged)

        updatePushers()
    }

    private fun listenForBackgroundSyncChanges() {
        generalSettingsManager.getSettingsFlow()
            .map { it.backgroundSync }
            .distinctUntilChanged()
            .onEach {
                launchUpdatePushers()
            }
            .launchIn(coroutineScope)
    }

    private fun onAccountsChanged() {
        launchUpdatePushers()
    }

    private fun onAutoSyncChanged() {
        launchUpdatePushers()
    }

    private fun onConnectivityChanged() {
        coroutineScope.launch(coroutineDispatcher) {
            synchronized(lock) {
                for (accountPushController in pushers.values) {
                    accountPushController.reconnect()
                }
            }

            updatePushers()
        }
    }

    private fun onConnectivityLost() {
        launchUpdatePushers()
    }

    private fun onBackendChanged(account: Account) {
        coroutineScope.launch(coroutineDispatcher) {
            val accountPushController = synchronized(lock) {
                pushers.remove(account.uuid)
            }

            accountPushController?.stop()
            updatePushers()
        }
    }

    private fun launchUpdatePushers() {
        coroutineScope.launch(coroutineDispatcher) {
            updatePushers()
        }
    }

    private fun updatePushers() {
        Timber.v("PushController.updatePushers()")

        val generalSettings = generalSettingsManager.getSettings()

        val backgroundSyncDisabledViaSystem = autoSyncManager.isAutoSyncDisabled
        val backgroundSyncDisabledInApp = generalSettings.backgroundSync == BackgroundSync.NEVER
        val networkNotAvailable = !connectivityManager.isNetworkAvailable()
        val realPushAccounts = getPushAccounts()

        val pushAccounts = if (backgroundSyncDisabledViaSystem || backgroundSyncDisabledInApp || networkNotAvailable) {
            emptyList()
        } else {
            realPushAccounts
        }
        val pushAccountUuids = pushAccounts.map { it.uuid }

        val arePushersActive = synchronized(lock) {
            val currentPushAccountUuids = pushers.keys
            val startPushAccountUuids = pushAccountUuids - currentPushAccountUuids
            val stopPushAccountUuids = currentPushAccountUuids - pushAccountUuids

            if (stopPushAccountUuids.isNotEmpty()) {
                Timber.v("..Stopping PushController for accounts: %s", stopPushAccountUuids)
                for (accountUuid in stopPushAccountUuids) {
                    val accountPushController = pushers.remove(accountUuid)
                    accountPushController?.stop()
                }
            }

            if (startPushAccountUuids.isNotEmpty()) {
                Timber.v("..Starting PushController for accounts: %s", startPushAccountUuids)
                for (accountUuid in startPushAccountUuids) {
                    val account = preferences.getAccount(accountUuid) ?: error("Account not found: $accountUuid")
                    pushers[accountUuid] = accountPushControllerFactory.create(account).also { accountPushController ->
                        accountPushController.start()
                    }
                }
            }

            Timber.v("..Running PushControllers: %s", pushers.keys)

            pushers.isNotEmpty()
        }

        when {
            realPushAccounts.isEmpty() -> {
                stopServices()
            }
            backgroundSyncDisabledViaSystem -> {
                setPushNotificationState(WAIT_BACKGROUND_SYNC)
                startServices()
            }
            networkNotAvailable -> {
                setPushNotificationState(WAIT_NETWORK)
                startServices()
            }
            arePushersActive -> {
                setPushNotificationState(LISTENING)
                startServices()
            }
            else -> {
                stopServices()
            }
        }
    }

    private fun getPushAccounts(): List<Account> {
        return preferences.accounts.filter { account ->
            account.folderPushMode != FolderMode.NONE && backendManager.getBackend(account).isPushCapable
        }
    }
    private fun setPushNotificationState(notificationState: PushNotificationState) {
        pushNotificationManager.notificationState = notificationState
    }

    private fun startServices() {
        pushServiceManager.start()
        bootCompleteManager.enableReceiver()
        registerAutoSyncListener()
        registerConnectivityChangeListener()
        connectivityManager.start()
    }

    private fun stopServices() {
        pushServiceManager.stop()
        bootCompleteManager.disableReceiver()
        autoSyncManager.unregisterListener()
        unregisterConnectivityChangeListener()
        connectivityManager.stop()
    }

    private fun registerAutoSyncListener() {
        if (autoSyncManager.respectSystemAutoSync) {
            autoSyncManager.registerListener(autoSyncListener)
        } else {
            autoSyncManager.unregisterListener()
        }
    }

    private fun registerConnectivityChangeListener() {
        connectivityManager.addListener(connectivityChangeListener)
    }

    private fun unregisterConnectivityChangeListener() {
        connectivityManager.removeListener(connectivityChangeListener)
    }
}
