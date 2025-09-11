package com.fsck.k9.controller.push

import app.k9mail.legacy.mailstore.FolderRepository
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.helper.mapToSet
import com.fsck.k9.notification.PushNotificationManager
import com.fsck.k9.notification.PushNotificationState
import com.fsck.k9.notification.PushNotificationState.ALARM_PERMISSION_MISSING
import com.fsck.k9.notification.PushNotificationState.LISTENING
import com.fsck.k9.notification.PushNotificationState.WAIT_BACKGROUND_SYNC
import com.fsck.k9.notification.PushNotificationState.WAIT_NETWORK
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.android.network.ConnectivityChangeListener
import net.thunderbird.core.android.network.ConnectivityManager
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.preference.BackgroundOps
import net.thunderbird.core.preference.BackgroundSync
import net.thunderbird.core.preference.GeneralSettingsManager

/**
 * Starts and stops [AccountPushController]s as necessary. Manages the Push foreground service.
 */
@Suppress("LongParameterList")
class PushController internal constructor(
    private val accountManager: LegacyAccountDtoManager,
    private val generalSettingsManager: GeneralSettingsManager,
    private val backendManager: BackendManager,
    private val pushServiceManager: PushServiceManager,
    private val bootCompleteManager: BootCompleteManager,
    private val autoSyncManager: AutoSyncManager,
    private val alarmPermissionManager: AlarmPermissionManager,
    private val pushNotificationManager: PushNotificationManager,
    private val connectivityManager: ConnectivityManager,
    private val accountPushControllerFactory: AccountPushControllerFactory,
    private val folderRepository: FolderRepository,
    private val coroutineScope: CoroutineScope = GlobalScope,
    private val coroutineDispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
) {
    private val lock = Any()
    private var initializationStarted = false
    private val pushers = mutableMapOf<String, AccountPushController>()

    private val pushEnabledCollectorJobs = mutableMapOf<String, Job>()

    private val autoSyncListener = AutoSyncListener(::onAutoSyncChanged)
    private val connectivityChangeListener = object : ConnectivityChangeListener {
        override fun onConnectivityChanged() = this@PushController.onConnectivityChanged()
        override fun onConnectivityLost() = this@PushController.onConnectivityLost()
    }
    private val alarmPermissionListener = AlarmPermissionListener(::onAlarmPermissionGranted)

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
        Log.v("PushController.disablePush()")

        coroutineScope.launch(coroutineDispatcher) {
            for (account in accountManager.getAccounts()) {
                folderRepository.setPushDisabled(account)
            }
        }
    }

    private fun initInBackground() {
        Log.v("PushController.initInBackground()")

        accountManager.addOnAccountsChangeListener(::onAccountsChanged)
        listenForBackgroundSyncChanges()
        backendManager.addListener(::onBackendChanged)

        updatePushers()
    }

    private fun listenForBackgroundSyncChanges() {
        generalSettingsManager.getConfigFlow()
            .map { it.network.backgroundOps }
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

    private fun onAlarmPermissionGranted() {
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

    private fun onBackendChanged(account: LegacyAccountDto) {
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

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun updatePushers() {
        Log.v("PushController.updatePushers()")

        val generalSettings = generalSettingsManager.getSettings()

        val alarmPermissionMissing = !alarmPermissionManager.canScheduleExactAlarms()
        val backgroundSyncDisabledViaSystem = autoSyncManager.isAutoSyncDisabled
        val backgroundSyncDisabledInApp =
            generalSettings.network.backgroundOps.toBackgroundSync() == BackgroundSync.NEVER
        val networkNotAvailable = !connectivityManager.isNetworkAvailable()
        val realPushAccounts = getPushAccounts()

        val shouldDisablePushAccounts = backgroundSyncDisabledViaSystem ||
            backgroundSyncDisabledInApp ||
            networkNotAvailable ||
            alarmPermissionMissing

        val pushAccounts = if (shouldDisablePushAccounts) {
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
                Log.v("..Stopping PushController for accounts: %s", stopPushAccountUuids)
                for (accountUuid in stopPushAccountUuids) {
                    val accountPushController = pushers.remove(accountUuid)
                    accountPushController?.stop()
                }
            }

            if (startPushAccountUuids.isNotEmpty()) {
                Log.v("..Starting PushController for accounts: %s", startPushAccountUuids)
                for (accountUuid in startPushAccountUuids) {
                    val account = accountManager.getAccount(accountUuid) ?: error("Account not found: $accountUuid")
                    pushers[accountUuid] = accountPushControllerFactory.create(account).also { accountPushController ->
                        accountPushController.start()
                    }
                }
            }

            Log.v("..Running PushControllers: %s", pushers.keys)

            pushers.isNotEmpty()
        }

        updatePushEnabledListeners(getPushCapableAccounts())

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

            alarmPermissionMissing -> {
                setPushNotificationState(ALARM_PERMISSION_MISSING)
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

    private fun getPushCapableAccounts(): Set<LegacyAccountDto> {
        return accountManager.getAccounts()
            .asSequence()
            .filter { account -> backendManager.getBackend(account).isPushCapable }
            .toSet()
    }

    private fun getPushAccounts(): Set<LegacyAccountDto> {
        return getPushCapableAccounts()
            .asSequence()
            .filter { account -> folderRepository.hasPushEnabledFolder(account) }
            .toSet()
    }

    private fun setPushNotificationState(notificationState: PushNotificationState) {
        pushNotificationManager.notificationState = notificationState
    }

    private fun startServices() {
        pushServiceManager.start()
        bootCompleteManager.enableReceiver()
        registerAutoSyncListener()
        registerConnectivityChangeListener()
        registerAlarmPermissionListener()
        connectivityManager.start()
    }

    private fun stopServices() {
        pushServiceManager.stop()
        bootCompleteManager.disableReceiver()
        autoSyncManager.unregisterListener()
        unregisterConnectivityChangeListener()
        alarmPermissionManager.unregisterListener()
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

    private fun registerAlarmPermissionListener() {
        if (!alarmPermissionManager.canScheduleExactAlarms()) {
            alarmPermissionManager.registerListener(alarmPermissionListener)
        }
    }

    private fun updatePushEnabledListeners(accounts: Set<LegacyAccountDto>) {
        synchronized(lock) {
            // Stop listening to push enabled changes in accounts we no longer monitor
            val accountUuids = accounts.mapToSet { it.uuid }
            val iterator = pushEnabledCollectorJobs.iterator()
            while (iterator.hasNext()) {
                val (accountUuid, collectorJob) = iterator.next()
                if (accountUuid !in accountUuids) {
                    Log.v("..Stopping to listen for push enabled changes in account: %s", accountUuid)
                    iterator.remove()
                    collectorJob.cancel()
                }
            }

            // Start "push enabled" state collector jobs for new accounts to monitor
            val newAccounts = accounts.filterNot { account -> pushEnabledCollectorJobs.containsKey(account.uuid) }
            for (account in newAccounts) {
                pushEnabledCollectorJobs[account.uuid] = coroutineScope.launch(coroutineDispatcher) {
                    Log.v("..Starting to listen for push enabled changes in account: %s", account.uuid)
                    folderRepository.hasPushEnabledFolderFlow(account)
                        .collect {
                            updatePushers()
                        }
                }
            }
        }
    }
}

fun BackgroundOps.toBackgroundSync(): BackgroundSync {
    return when (this) {
        BackgroundOps.ALWAYS -> BackgroundSync.ALWAYS
        BackgroundOps.NEVER -> BackgroundSync.NEVER
        BackgroundOps.WHEN_CHECKED_AUTO_SYNC -> BackgroundSync.FOLLOW_SYSTEM_AUTO_SYNC
    }
}
