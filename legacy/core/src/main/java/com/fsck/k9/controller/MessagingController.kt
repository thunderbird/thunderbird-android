package com.fsck.k9.controller

import android.content.Context
import android.os.Process
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import app.k9mail.legacy.di.DI.get
import app.k9mail.legacy.mailstore.FolderDetailsAccessor
import app.k9mail.legacy.mailstore.MessageStore
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.message.controller.MessagingControllerMailChecker
import app.k9mail.legacy.message.controller.MessagingControllerRegistry
import app.k9mail.legacy.message.controller.MessagingListener
import app.k9mail.legacy.message.controller.SimpleMessagingListener
import com.fsck.k9.K9
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.controller.ControllerExtension.ControllerInternals
import com.fsck.k9.controller.MessagingControllerCommands.PendingAppend
import com.fsck.k9.controller.MessagingControllerCommands.PendingCommand
import com.fsck.k9.controller.MessagingControllerCommands.PendingDelete
import com.fsck.k9.controller.MessagingControllerCommands.PendingEmptySpam
import com.fsck.k9.controller.MessagingControllerCommands.PendingEmptyTrash
import com.fsck.k9.controller.MessagingControllerCommands.PendingExpunge
import com.fsck.k9.controller.MessagingControllerCommands.PendingMarkAllAsRead
import com.fsck.k9.controller.MessagingControllerCommands.PendingMoveAndMarkAsRead
import com.fsck.k9.controller.MessagingControllerCommands.PendingMoveOrCopy
import com.fsck.k9.controller.MessagingControllerCommands.PendingReplace
import com.fsck.k9.controller.MessagingControllerCommands.PendingSetFlag
import com.fsck.k9.core.BuildConfig
import com.fsck.k9.helper.MutableBoolean
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.CertificateValidationException
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.power.PowerManager
import com.fsck.k9.mail.power.WakeLock
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.MessageListCache.Companion.getCache
import com.fsck.k9.mailstore.SaveMessageDataCreator
import com.fsck.k9.mailstore.SendState
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import com.fsck.k9.notification.NotificationController
import com.fsck.k9.notification.NotificationStrategy
import java.util.EnumSet
import java.util.LinkedList
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.Volatile
import kotlin.coroutines.Continuation
import kotlinx.coroutines.Dispatchers.IO
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.exception.rootCauseMessage
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.folder.api.hasPendingMessagesSync
import net.thunderbird.feature.mail.message.list.LocalDeleteOperationDecider
import net.thunderbird.feature.mail.message.list.LocalMessageUidPrefixProvider
import net.thunderbird.feature.notification.api.NotificationManager
import net.thunderbird.feature.notification.api.command.outcome.Failure
import net.thunderbird.feature.notification.api.command.outcome.Success
import net.thunderbird.feature.notification.api.content.AuthenticationErrorNotification
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.content.NotificationFactoryCoroutineCompat
import net.thunderbird.feature.notification.api.dismisser.compat.NotificationDismisserCompat
import net.thunderbird.feature.notification.api.sender.compat.NotificationSenderCompat
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.legacy.logging.Log.d
import net.thunderbird.legacy.logging.Log.e
import net.thunderbird.legacy.logging.Log.i
import net.thunderbird.legacy.logging.Log.v
import net.thunderbird.legacy.logging.Log.w

/**
 * Starts a long running (application) Thread that will run through commands that require remote mailbox access. This
 * class is used to serialize and prioritize these commands. Each method that will submit a command requires a
 * MessagingListener instance to be provided. It is expected that that listener has also been added as a registered
 * listener using addListener(). When a command is to be executed, if the listener that was provided with the command is
 * no longer registered the command is skipped. The design idea for the above is that when an Activity starts it
 * registers as a listener. When it is paused it removes itself. Thus, any commands that that activity submitted are
 * removed from the queue once the activity is no longer active.
 */
class MessagingController internal constructor(
    private val context: Context?,
    private val notificationController: NotificationController,
    private val notificationStrategy: NotificationStrategy,
    private val localStoreProvider: LocalStoreProvider,
    private val backendManager: BackendManager,
    private val messageStoreManager: MessageStoreManager,
    private val saveMessageDataCreator: SaveMessageDataCreator,
    private val specialLocalFoldersCreator: SpecialLocalFoldersCreator?,
    private val localDeleteOperationDecider: LocalDeleteOperationDecider,
    private val localMessageUidPrefixProvider: LocalMessageUidPrefixProvider,
    controllerExtensions: MutableList<ControllerExtension>,
    private val featureFlagProvider: FeatureFlagProvider,
    private val syncDebugLogger: Logger,
    notificationManager: NotificationManager,
    outboxFolderManager: OutboxFolderManager,
    accountManager: LegacyAccountManager
) : MessagingControllerRegistry, MessagingControllerMailChecker {
    private val controllerThread: Thread

    private val queuedCommands: BlockingQueue<Command?> = PriorityBlockingQueue<Command?>()
    val listeners: MutableSet<MessagingListener> = CopyOnWriteArraySet<MessagingListener>()
    private val threadPool: ExecutorService = Executors.newCachedThreadPool()
    private val memorizingMessagingListener = MemorizingMessagingListener()
    private val draftOperations: DraftOperations
    private val notificationOperations: NotificationOperations
    private val archiveOperations: ArchiveOperations
    private val outboxFolderManager: OutboxFolderManager
    private val notificationSender: NotificationSenderCompat
    private val notificationDismisser: NotificationDismisserCompat
    private val accountManager: LegacyAccountManager

    @Volatile
    private var stopped = false

    private fun initializeControllerExtensions(controllerExtensions: MutableList<ControllerExtension>) {
        if (controllerExtensions.isEmpty()) {
            return
        }

        val internals: ControllerInternals = object : ControllerInternals {
            override fun put(
                description: String, listener: MessagingListener?,
                runnable: Runnable
            ) {
                this@MessagingController.put(description, listener, runnable)
            }

            override fun putBackground(
                description: String, listener: MessagingListener?,
                runnable: Runnable
            ) {
                this@MessagingController.putBackground(description, listener, runnable)
            }
        }

        for (extension in controllerExtensions) {
            extension.init(this, backendManager, internals)
        }
    }

    @VisibleForTesting
    @Throws(InterruptedException::class)
    fun stop() {
        stopped = true
        controllerThread.interrupt()
        controllerThread.join(1000L)
    }

    private fun runInBackground() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        while (!stopped) {
            var commandDescription: String? = null
            try {
                val command = queuedCommands.take()

                if (command != null) {
                    commandDescription = command.description

                    i(
                        TAG, "Running command '%s', seq = %s (%s priority)",
                        command.description,
                        command.sequence,
                        if (command.isForegroundPriority) "foreground" else "background"
                    )

                    command.runnable!!.run()

                    i(TAG, " Command '%s' completed", command.description)
                }
            } catch (e: Exception) {
                e(TAG, e, "Error running command '%s'", commandDescription)
            }
        }
    }

    private fun put(description: String?, listener: MessagingListener?, runnable: Runnable) {
        putCommand(queuedCommands, description, listener, runnable, true)
    }

    fun putBackground(description: String?, listener: MessagingListener?, runnable: Runnable) {
        putCommand(queuedCommands, description, listener, runnable, false)
    }

    private fun putCommand(
        queue: BlockingQueue<Command?>, description: String?, listener: MessagingListener?,
        runnable: Runnable, isForeground: Boolean
    ) {
        var retries = 10
        var e: Exception? = null
        while (retries-- > 0) {
            try {
                val command = Command()
                command.listener = listener
                command.runnable = runnable
                command.description = description
                command.isForegroundPriority = isForeground
                queue.put(command)
                return
            } catch (ie: InterruptedException) {
                SystemClock.sleep(200)
                e = ie
            }
        }
        throw Error(e)
    }

    fun getBackend(accountId: AccountId): Backend {
        return backendManager.getBackend(accountId)
    }

    /**
     * Returns `null` when the account no longer exists, e.g. when it was removed while the UI or background work
     * still referred to it.
     */
    private fun getBackendOrNull(accountId: AccountId): Backend? {
        if (accountManager.findById(accountId) == null) {
            return null
        }
        return getBackend(accountId)
    }

    fun getLocalStoreOrThrow(accountId: AccountId): LocalStore {
        try {
            return localStoreProvider.getInstance(accountId)
        } catch (e: MessagingException) {
            throw IllegalStateException("Couldn't get LocalStore for account " + accountId)
        }
    }

    private fun getFolderServerId(accountId: AccountId, folderId: Long): String {
        val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)
        val folderServerId: String = messageStore.getFolderServerId(folderId)!!
        checkNotNull(folderServerId) { "Folder not found (ID: " + folderId + ")" }
        return folderServerId
    }

    private fun getFolderId(accountId: AccountId, folderServerId: String): Long {
        val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)
        val folderId = messageStore.getFolderId(folderServerId)
        checkNotNull(folderId) { "Folder not found (server ID: " + folderServerId + ")" }
        return folderId
    }

    override fun addListener(listener: MessagingListener) {
        listeners.add(listener)
        refreshListener(listener)
    }

    fun refreshListener(listener: MessagingListener?) {
        if (listener != null) {
            memorizingMessagingListener.refreshOther(listener)
        }
    }

    override fun removeListener(listener: MessagingListener) {
        listeners.remove(listener)
    }

    fun getListeners(listener: MessagingListener?): MutableSet<MessagingListener> {
        if (listener == null) {
            return listeners
        }

        val listeners: MutableSet<MessagingListener> = HashSet<MessagingListener>(this.listeners)
        listeners.add(listener)
        return listeners
    }

    fun suppressMessages(accountId: AccountId, messages: MutableList<LocalMessage>) {
        val cache = getCache(accountId)
        cache.hideMessages(messages)
    }

    private fun unsuppressMessages(accountId: AccountId, messages: MutableList<LocalMessage>) {
        val cache = getCache(accountId)
        cache.unhideMessages(messages)
    }

    fun isMessageSuppressed(message: LocalMessage): Boolean {
        val messageId = message.getDatabaseId()
        val folderId = message.getFolder().getDatabaseId()

        val cache = getCache(message.getFolder().getAccountId())
        return cache.isMessageHidden(messageId, folderId)
    }

    private fun setFlagInCache(
        accountId: AccountId, messageIds: MutableList<Long?>,
        flag: Flag, newState: Boolean
    ) {
        val cache = getCache(accountId)
        cache.setFlagForMessages(messageIds, flag, newState)
    }

    private fun removeFlagFromCache(
        accountId: AccountId, messageIds: MutableList<Long?>,
        flag: Flag
    ) {
        val cache = getCache(accountId)
        cache.removeFlagForMessages(messageIds, flag)
    }

    private fun setFlagForThreadsInCache(
        accountId: AccountId, threadRootIds: MutableList<Long?>,
        flag: Flag, newState: Boolean
    ) {
        val cache = getCache(accountId)
        cache.setValueForThreads(threadRootIds, flag, newState)
    }

    private fun removeFlagForThreadsFromCache(
        accountId: AccountId, messageIds: MutableList<Long?>,
        flag: Flag
    ) {
        val cache = getCache(accountId)
        cache.removeFlagForThreads(messageIds, flag)
    }

    fun refreshFolderList(accountId: AccountId) {
        put("refreshFolderList", null, Runnable { refreshFolderListSynchronous(accountId) })
    }

    fun refreshFolderListBlocking(accountId: AccountId) {
        val latch = CountDownLatch(1)
        putBackground(
            "refreshFolderListBlocking", null,
            Runnable {
                try {
                    refreshFolderListSynchronous(accountId)
                } finally {
                    latch.countDown()
                }
            }
        )

        try {
            latch.await()
        } catch (e: Exception) {
            e(TAG, e, "Interrupted while awaiting latch release")
        }
    }

    fun refreshFolderListSynchronous(accountId: AccountId) {
        val account = accountManager.findById(accountId)
        if (account == null) {
            return
        }

        try {
            if (isAuthenticationProblem(accountId, true)) {
                d(TAG, "Authentication will fail. Skip refreshing the folder list.")
                handleAuthenticationFailure(accountId, true)
                return
            }

            val backend = getBackend(accountId)
            val folderPathDelimiter = backend.refreshFolderList()

            val now = System.currentTimeMillis()
            d(TAG, "Folder list successfully refreshed @ %tc", now)

            // Refreshing the folder list may update the account (e.g. special folders), so only change what is ours
            accountManager.updateSync(accountId) { currentAccount: LegacyAccount ->
                var updatedAccount = currentAccount
                if (folderPathDelimiter != null && !folderPathDelimiter.isEmpty() && (folderPathDelimiter != currentAccount.folderPathDelimiter)) {
                    updatedAccount = updatedAccount.updateFolderDelimiter(folderPathDelimiter)
                }
                updatedAccount.updateLastFolderListRefreshTime(now)
            }
        } catch (e: Exception) {
            e(TAG, e, "Could not refresh folder list for account %s", accountId)
            handleException(accountId, e)
        }
    }

    fun searchRemoteMessages(
        accountId: AccountId, folderId: Long, query: String?, requiredFlags: MutableSet<Flag?>?,
        forbiddenFlags: MutableSet<Flag?>?, listener: MessagingListener?
    ): Future<*>? {
        i(TAG, "searchRemoteMessages (acct = %s, folderId = %d, query = %s)", accountId, folderId, query)

        return threadPool.submit(
            Runnable {
                searchRemoteMessagesSynchronous(
                    accountId,
                    folderId,
                    query,
                    requiredFlags,
                    forbiddenFlags,
                    listener
                )
            }
        )
    }

    @VisibleForTesting
    fun searchRemoteMessagesSynchronous(
        accountId: AccountId, folderId: Long, query: String?, requiredFlags: MutableSet<Flag?>?,
        forbiddenFlags: MutableSet<Flag?>?, listener: MessagingListener?
    ) {
        val account = accountManager.findById(accountId)

        if (listener != null) {
            listener.remoteSearchStarted(folderId)
        }

        var extraResults: MutableList<String?> = ArrayList<String?>()
        try {
            val localStore = localStoreProvider.getInstance(accountId)

            val localFolder = localStore.getFolder(folderId)
            if (!localFolder.exists()) {
                throw MessagingException("Folder not found")
            }

            localFolder.open()
            val folderServerId = localFolder.getServerId()

            val backend = getBackend(accountId)

            val performFullTextSearch = account!!.isRemoteSearchFullText
            var messageServerIds: MutableList<String> = backend.search(
                folderServerId, query, requiredFlags, forbiddenFlags,
                performFullTextSearch
            )

            i(TAG, "Remote search got %d results", messageServerIds.size)

            // There's no need to fetch messages already completely downloaded
            messageServerIds = localFolder.extractNewMessages(messageServerIds)

            if (listener != null) {
                listener.remoteSearchServerQueryComplete(
                    folderId, messageServerIds.size,
                    account.remoteSearchNumResults
                )
            }

            val resultLimit = account.remoteSearchNumResults
            if (resultLimit > 0 && messageServerIds.size > resultLimit) {
                extraResults = messageServerIds.subList(resultLimit, messageServerIds.size)
                messageServerIds = messageServerIds.subList(0, resultLimit)
            }

            loadSearchResultsSynchronous(accountId, messageServerIds, localFolder)
        } catch (e: Exception) {
            if (Thread.currentThread().isInterrupted()) {
                i(TAG, e, "Caught exception on aborted remote search; safe to ignore.")
            } else {
                e(TAG, e, "Could not complete remote search")
                if (listener != null) {
                    listener.remoteSearchFailed(null, e.message)
                }
                e(TAG, e, "Remote search failed for account %s, folder %d", accountId, folderId)
            }
        } finally {
            if (listener != null) {
                listener.remoteSearchFinished(folderId, 0, account!!.remoteSearchNumResults, extraResults)
            }
        }
    }

    fun loadSearchResults(
        accountId: AccountId, folderId: Long, messageServerIds: MutableList<String>,
        listener: MessagingListener?
    ) {
        threadPool.execute(
            Runnable {
                if (listener != null) {
                    listener.enableProgressIndicator(true)
                }
                try {
                    val localStore = localStoreProvider.getInstance(accountId)
                    val localFolder = localStore.getFolder(folderId)
                    if (!localFolder.exists()) {
                        throw MessagingException("Folder not found")
                    }

                    localFolder.open()

                    loadSearchResultsSynchronous(accountId, messageServerIds, localFolder)
                } catch (e: MessagingException) {
                    e(TAG, e, "Exception in loadSearchResults")
                } finally {
                    if (listener != null) {
                        listener.enableProgressIndicator(false)
                    }
                }
            }
        )
    }

    @Throws(MessagingException::class)
    private fun loadSearchResultsSynchronous(
        accountId: AccountId, messageServerIds: MutableList<String>,
        localFolder: LocalFolder
    ) {
        val backend = getBackend(accountId)
        val folderServerId = localFolder.getServerId()

        for (messageServerId in messageServerIds) {
            val localMessage = localFolder.getMessage(messageServerId)

            if (localMessage == null) {
                backend.downloadMessageStructure(folderServerId, messageServerId)
            }
        }
    }

    fun loadMoreMessages(accountId: AccountId, folderId: Long) {
        putBackground("loadMoreMessages", null, Runnable { loadMoreMessagesSynchronous(accountId, folderId) })
    }

    fun loadMoreMessagesSynchronous(accountId: AccountId, folderId: Long) {
        val account = accountManager.findById(accountId)
        if (account == null) {
            return
        }

        val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)
        val visibleLimit = messageStore.getFolder<Int?>(folderId, FolderDetailsAccessor::visibleLimit)
        if (visibleLimit == null) {
            v(TAG, "loadMoreMessages(%s, %d): Folder not found", accountId, folderId)
            return
        }

        if (visibleLimit > 0) {
            val newVisibleLimit = visibleLimit + account.displayCount
            messageStore.setVisibleLimit(folderId, newVisibleLimit)
        }

        synchronizeMailboxSynchronous(accountId, folderId, false, null, NotificationState())
    }

    /**
     * Start background synchronization of the specified folder.
     */
    fun synchronizeMailbox(
        accountId: AccountId, folderId: Long, notify: Boolean,
        listener: MessagingListener
    ) {
        putBackground(
            "synchronizeMailbox",
            listener,
            Runnable { synchronizeMailboxSynchronous(accountId, folderId, notify, listener, NotificationState()) }
        )
    }

    fun synchronizeMailboxBlocking(accountId: AccountId, folderServerId: String) {
        val folderId = getFolderId(accountId, folderServerId)

        val latch = CountDownLatch(1)
        putBackground(
            "synchronizeMailbox", null,
            Runnable {
                try {
                    synchronizeMailboxSynchronous(accountId, folderId, true, null, NotificationState())
                } finally {
                    latch.countDown()
                }
            }
        )

        try {
            latch.await()
        } catch (e: Exception) {
            e(TAG, e, "Interrupted while awaiting latch release")
        }
    }

    private fun synchronizeMailboxSynchronous(
        accountId: AccountId, folderId: Long, notify: Boolean,
        listener: MessagingListener, notificationState: NotificationState
    ) {
        refreshFolderListIfStale(accountId)

        val backend = getBackend(accountId)
        syncFolder(accountId, folderId, notify, listener, backend, notificationState)
    }

    private fun refreshFolderListIfStale(accountId: AccountId) {
        val account = accountManager.findById(accountId)
        if (account == null) {
            return
        }

        val lastFolderListRefresh = account.lastFolderListRefreshTime
        val now = System.currentTimeMillis()

        if (lastFolderListRefresh > now || lastFolderListRefresh + FOLDER_LIST_STALENESS_THRESHOLD <= now) {
            d(TAG, "Last folder list refresh @ %tc. Refreshing now…", lastFolderListRefresh)
            refreshFolderListSynchronous(accountId)
        } else {
            d(TAG, "Last folder list refresh @ %tc. Not refreshing now.", lastFolderListRefresh)
        }
    }

    private fun syncFolder(
        accountId: AccountId, folderId: Long, notify: Boolean, listener: MessagingListener,
        backend: Backend,
        notificationState: NotificationState
    ) {
        if (isAuthenticationProblem(accountId, true)) {
            d(TAG, "Authentication will fail. Skip synchronizing folder %d.", folderId)
            handleAuthenticationFailure(accountId, true)
            return
        }

        var commandException: Exception? = null
        try {
            processPendingCommandsSynchronous(accountId)
        } catch (e: Exception) {
            e(TAG, e, "Failure processing command, but allow message sync attempt")
            commandException = e
        }

        val localFolder: LocalFolder
        try {
            val localStore = localStoreProvider.getInstance(accountId)
            localFolder = localStore.getFolder(folderId)
            localFolder.open()
        } catch (e: MessagingException) {
            syncDebugLogger.error("MessagingException", null, e::message)
            e(TAG, e, "syncFolder: Couldn't load local folder %d", folderId)
            return
        }

        // We can't sync local folders
        if (localFolder.isLocalOnly()) {
            return
        }

        val suppressNotifications: Boolean
        if (notify) {
            val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)
            val lastChecked = messageStore.getFolder<Long?>(folderId, FolderDetailsAccessor::lastChecked)
            suppressNotifications = lastChecked == null
        } else {
            suppressNotifications = true
        }

        val folderServerId = localFolder.getServerId()
        val syncConfig = createSyncConfig(accountId)
        val syncListener =
            ControllerSyncListener(accountId, listener, suppressNotifications, notificationState)

        backend.sync(folderServerId, syncConfig, syncListener)

        if (commandException != null && !syncListener.syncFailed) {
            val rootMessage = commandException.rootCauseMessage
            syncDebugLogger.error("MessagingException", null) { rootMessage!! }
            e(TAG, "Root cause failure in %s:%s was '%s'", accountId, folderServerId, rootMessage)
            updateFolderStatus(accountId, folderId, rootMessage)
            listener.synchronizeMailboxFailed(accountId, folderId, rootMessage)
        }
    }

    private fun createSyncConfig(accountId: AccountId): SyncConfig {
        val account = accountManager.findById(accountId)
        requireNotNull(account) { "Account not found for " + accountId }
        return SyncConfig(
            account.expungePolicy.toBackendExpungePolicy(),
            account.getEarliestPollDate(),
            account.isSyncRemoteDeletions,
            account.maximumAutoDownloadMessageSize,
            AccountDefaultsProvider.DEFAULT_VISIBLE_LIMIT,
            SYNC_FLAGS
        )
    }

    private fun updateFolderStatus(accountId: AccountId, folderId: Long, status: String?) {
        val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.setStatus(folderId, status)
    }

    fun handleAuthenticationFailure(accountId: AccountId, incoming: Boolean) {
        val account = accountManager.findById(accountId)
        if (account != null && account.shouldMigrateToOAuth) {
            migrateAccountToOAuth(accountId)
        }

        if (featureFlagProvider.provide(GeneratedFeatureFlagKey.DISPLAY_IN_APP_NOTIFICATIONS).isEnabled()) {
            d(TAG, "handleAuthenticationFailure: sending in-app notification")
            val notification =
                createAuthenticationErrorNotification(accountId, incoming)

            if (notification != null) {
                notificationSender.send(
                    notification,
                    NotificationSenderCompat.OnResultListener { outcome: Outcome<out Success<out Notification?>?, out Failure<out Notification?>?>? ->
                        v(
                            TAG, "notificationSender outcome = " + outcome
                        )
                    }
                )
            }
        }

        if (featureFlagProvider
                .provide(GeneratedFeatureFlagKey.USE_NOTIFICATION_SENDER_FOR_SYSTEM_NOTIFICATIONS)
                .isDisabled()
        ) {
            d(TAG, "handleAuthenticationFailure: sending system notification via old notification controller")
            notificationController.showAuthenticationErrorNotification(accountId, incoming)
        }
    }

    private fun createAuthenticationErrorNotification(
        accountId: AccountId,
        incoming: Boolean
    ): AuthenticationErrorNotification? {
        val account = accountManager.findById(accountId)
        if (account == null) {
            return null
        }

        return NotificationFactoryCoroutineCompat.create<AuthenticationErrorNotification?> { continuation: Continuation<in AuthenticationErrorNotification?>? ->
            AuthenticationErrorNotification.Companion.invoke(
                account.id,
                account.profile.name,
                account.accountNumber,
                incoming,
                continuation
            )
        }
    }

    private fun migrateAccountToOAuth(accountId: AccountId) {
        accountManager.updateSync(accountId) { account: LegacyAccount ->
            val incomingServerSettings = account.incomingServerSettings
                .newAuthenticationType(AuthType.XOAUTH2)
            val outgoingServerSettings = account.outgoingServerSettings
                .newAuthenticationType(AuthType.XOAUTH2)
            account.updateIncomingServerSettings(incomingServerSettings)
                .updateOutgoingServerSettings(outgoingServerSettings)
                .updateShouldMigrateToOAuth(false)
        }
    }

    fun handleException(accountId: AccountId, exception: Exception?) {
        if (exception is AuthenticationFailedException) {
            handleAuthenticationFailure(accountId, true)
        } else {
            notifyUserIfCertificateProblem(accountId, exception, true)
        }
    }

    fun queuePendingCommand(accountId: AccountId, command: PendingCommand) {
        try {
            val localStore = localStoreProvider.getInstance(accountId)
            localStore.addPendingCommand(command)
        } catch (e: Exception) {
            throw RuntimeException("Unable to enqueue pending command", e)
        }
    }

    fun processPendingCommands(accountId: AccountId) {
        putBackground(
            "processPendingCommands", null,
            object : Runnable {
                override fun run() {
                    try {
                        processPendingCommandsSynchronous(accountId)
                    } catch (me: MessagingException) {
                        e(TAG, me, "processPendingCommands")

                        /*
                     * Ignore any exceptions from the commands. Commands will be processed
                     * on the next round.
                     */
                    }
                }
            }
        )
    }

    @Throws(MessagingException::class)
    fun processPendingCommandsSynchronous(accountId: AccountId) {
        val localStore = localStoreProvider.getInstance(accountId)
        val commands = localStore.getPendingCommands()

        var processingCommand: PendingCommand? = null
        try {
            for (command in commands) {
                processingCommand = command
                val commandName = command.getCommandName()
                d(TAG, "Processing pending command '%s'", commandName)

                /*
                 * We specifically do not catch any exceptions here. If a command fails it is
                 * most likely due to a server or IO error and it must be retried before any
                 * other command processes. This maintains the order of the commands.
                 */
                try {
                    command.execute(this, accountId)

                    localStore.removePendingCommand(command)

                    d(TAG, "Done processing pending command '%s'", commandName)
                } catch (me: MessagingException) {
                    if (me.isPermanentFailure) {
                        e(TAG, me, "Failure of command '%s' was permanent, removing command from queue", commandName)
                        localStore.removePendingCommand(processingCommand)
                    } else {
                        throw me
                    }
                } catch (e: Exception) {
                    e(TAG, e, "Unexpected exception with command '%s', removing command from queue", commandName)
                    localStore.removePendingCommand(processingCommand)

                    if (BuildConfig.DEBUG) {
                        throw AssertionError("Unexpected exception while processing pending command", e)
                    }
                }

                // TODO: When removing a pending command due to an error the local changes should be reverted. Pending
                //  commands that depend on this command should be canceled and local changes be reverted. In most cases
                //  the user should be notified about the failure as well.
            }
        } catch (me: MessagingException) {
            notifyUserIfCertificateProblem(accountId, me, true)
            e(TAG, me, "Could not process command '%s'", processingCommand)
            throw me
        }
    }

    /**
     * Process a pending append message command. This command uploads a local message to the server, first checking to
     * be sure that the server message is not newer than the local message. Once the local message is successfully
     * processed it is deleted so that the server message will be synchronized down without an additional copy being
     * created.
     */
    @Throws(MessagingException::class)
    fun processPendingAppend(command: PendingAppend, accountId: AccountId) {
        val localStore = localStoreProvider.getInstance(accountId)
        val folderId = command.folderId
        val localFolder = localStore.getFolder(folderId)
        localFolder.open()

        val folderServerId = localFolder.getServerId()
        val uid = command.uid

        val localMessage = localFolder.getMessage(uid)
        if (localMessage == null) {
            return
        }

        if (!localMessage.getUid().startsWith(localMessageUidPrefixProvider.get())) {
            //FIXME: This should never happen. Throw in debug builds.
            return
        }

        val backend = getBackend(accountId)

        if (localMessage.isSet(Flag.X_REMOTE_COPY_STARTED)) {
            w(
                TAG,
                "Local message with uid %s has flag %s  already set, checking for remote message with " +
                    "same message id",
                localMessage.getUid(), Flag.X_REMOTE_COPY_STARTED
            )

            val messageServerId = backend.findByMessageId(folderServerId, localMessage.getMessageId())
            if (messageServerId != null) {
                w(
                    TAG,
                    "Local message has flag %s already set, and there is a remote message with uid %s, " +
                        "assuming message was already copied and aborting this copy",
                    Flag.X_REMOTE_COPY_STARTED, messageServerId
                )

                val oldUid = localMessage.getUid()
                localMessage.setUid(messageServerId)
                localFolder.changeUid(localMessage)

                for (l in this.listeners) {
                    l.messageUidChanged(accountId, folderId, oldUid, localMessage.getUid())
                }

                return
            } else {
                w(TAG, "No remote message with message-id found, proceeding with append")
            }
        }

        /*
         * If the message does not exist remotely we just upload it and then
         * update our local copy with the new uid.
         */
        val fp = FetchProfile()
        fp.add(FetchProfile.Item.BODY)
        localFolder.fetch(mutableListOf<LocalMessage?>(localMessage), fp, null)
        val oldUid = localMessage.getUid()
        localMessage.setFlag(Flag.X_REMOTE_COPY_STARTED, true)

        val messageServerId = backend.uploadMessage(folderServerId, localMessage)

        if (messageServerId == null) {
            // We didn't get the server UID of the uploaded message. Remove the local message now. The uploaded
            // version will be downloaded during the next sync.
            localFolder.destroyMessages(mutableListOf<LocalMessage?>(localMessage))
        } else {
            localMessage.setUid(messageServerId)
            localFolder.changeUid(localMessage)

            for (l in this.listeners) {
                l.messageUidChanged(accountId, folderId, oldUid, localMessage.getUid())
            }
        }
    }

    fun processPendingReplace(pendingReplace: PendingReplace, accountId: AccountId) {
        draftOperations.processPendingReplace(pendingReplace, accountId)
    }

    private fun queueMoveOrCopy(
        accountId: AccountId, srcFolderId: Long, destFolderId: Long,
        operation: MoveOrCopyFlavor,
        uidMap: MutableMap<String?, String?>?
    ) {
        val command: PendingCommand?
        when (operation) {
            MoveOrCopyFlavor.MOVE -> command = PendingMoveOrCopy.create(srcFolderId, destFolderId, false, uidMap)
            MoveOrCopyFlavor.COPY -> command = PendingMoveOrCopy.create(srcFolderId, destFolderId, true, uidMap)
            MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ -> command =
                PendingMoveAndMarkAsRead.create(srcFolderId, destFolderId, uidMap)

            else -> return
        }
        queuePendingCommand(accountId, command)
    }

    @Throws(MessagingException::class)
    fun processPendingMoveOrCopy(command: PendingMoveOrCopy, accountId: AccountId) {
        val srcFolder = command.srcFolderId
        val destFolder = command.destFolderId
        val operation = if (command.isCopy) MoveOrCopyFlavor.COPY else MoveOrCopyFlavor.MOVE

        val newUidMap = command.newUidMap
        val uids = if (newUidMap != null) ArrayList<String?>(newUidMap.keys) else command.uids

        processPendingMoveOrCopy(accountId, srcFolder, destFolder, uids, operation, newUidMap!!)
    }

    @Throws(MessagingException::class)
    fun processPendingMoveAndRead(command: PendingMoveAndMarkAsRead, accountId: AccountId) {
        val srcFolder = command.srcFolderId
        val destFolder = command.destFolderId
        val newUidMap = command.newUidMap
        val uids: MutableList<String?> = ArrayList<String?>(newUidMap.keys)

        processPendingMoveOrCopy(
            accountId, srcFolder, destFolder, uids,
            MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ, newUidMap
        )
    }

    @VisibleForTesting
    @Throws(MessagingException::class)
    fun processPendingMoveOrCopy(
        accountId: AccountId, srcFolderId: Long, destFolderId: Long, uids: MutableList<String?>,
        operation: MoveOrCopyFlavor, newUidMap: MutableMap<String?, String?>
    ) {
        requireNotNull<MutableMap<String?, String?>>(newUidMap)

        val localStore = localStoreProvider.getInstance(accountId)

        val localSourceFolder = localStore.getFolder(srcFolderId)
        localSourceFolder.open()
        val srcFolderServerId = localSourceFolder.getServerId()

        val localDestFolder = localStore.getFolder(destFolderId)
        localDestFolder.open()
        val destFolderServerId = localDestFolder.getServerId()

        val backend = getBackend(accountId)

        var remoteUidMap: MutableMap<String?, String?>?
        when (operation) {
            MoveOrCopyFlavor.COPY -> remoteUidMap = backend.copyMessages(srcFolderServerId, destFolderServerId, uids)
            MoveOrCopyFlavor.MOVE -> remoteUidMap = backend.moveMessages(srcFolderServerId, destFolderServerId, uids)
            MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ -> remoteUidMap =
                backend.moveMessagesAndMarkAsRead(srcFolderServerId, destFolderServerId, uids)

            else -> throw RuntimeException("Unsupported messaging operation")
        }

        if (operation != MoveOrCopyFlavor.COPY) {
            destroyPlaceholderMessages(localSourceFolder, uids)
        }

        // TODO: Change Backend interface to ensure we never receive null for remoteUidMap
        if (remoteUidMap == null) {
            remoteUidMap = mutableMapOf<String?, String?>()
        }

        // Update local messages (that currently have local UIDs) with new server IDs
        for (uid in uids) {
            val localUid = newUidMap.get(uid)
            val newUid = remoteUidMap.get(uid)

            val localMessage = localDestFolder.getMessage(localUid)
            if (localMessage == null) {
                // Local message no longer exists
                continue
            }

            if (newUid != null) {
                // Update local message with new server ID
                localMessage.setUid(newUid)
                localDestFolder.changeUid(localMessage)
                for (l in this.listeners) {
                    l.messageUidChanged(accountId, destFolderId, localUid, newUid)
                }
            } else {
                // New server ID wasn't provided. Remove local message.
                localMessage.destroy()
            }
        }
    }

    @Throws(MessagingException::class)
    fun destroyPlaceholderMessages(localFolder: LocalFolder, uids: MutableList<String?>) {
        for (uid in uids) {
            val placeholderMessage = localFolder.getMessage(uid)
            if (placeholderMessage == null) {
                continue
            }

            if (placeholderMessage.isSet(Flag.DELETED)) {
                placeholderMessage.destroy()
            } else {
                w(
                    TAG, "Expected local message %s in folder %s to be a placeholder, but DELETE flag wasn't set",
                    uid, localFolder.getServerId()
                )

                if (BuildConfig.DEBUG) {
                    throw AssertionError("Placeholder message must have the DELETED flag set")
                }
            }
        }
    }

    private fun queueSetFlag(
        accountId: AccountId,
        folderId: Long,
        newState: Boolean,
        flag: Flag?,
        uids: MutableList<String>?
    ) {
        val command: PendingCommand = PendingSetFlag.create(folderId, newState, flag, uids)
        queuePendingCommand(accountId, command)
    }

    /**
     * Processes a pending mark read or unread command.
     */
    @Throws(MessagingException::class)
    fun processPendingSetFlag(command: PendingSetFlag, accountId: AccountId) {
        val backend = getBackend(accountId)
        val folderServerId = getFolderServerId(accountId, command.folderId)
        backend.setFlag(folderServerId, command.uids, command.flag, command.newState)
    }

    private fun queueDelete(accountId: AccountId, folderId: Long, uids: MutableList<String>?) {
        val command: PendingCommand = PendingDelete.create(folderId, uids)
        queuePendingCommand(accountId, command)
    }

    @Throws(MessagingException::class)
    fun processPendingDelete(command: PendingDelete, accountId: AccountId) {
        val folderId = command.folderId
        val uids = command.uids

        val backend = getBackend(accountId)
        val folderServerId = getFolderServerId(accountId, folderId)
        backend.deleteMessages(folderServerId, uids)

        val localStore = localStoreProvider.getInstance(accountId)
        val localFolder = localStore.getFolder(folderId)
        localFolder.open()
        destroyPlaceholderMessages(localFolder, uids)
    }

    private fun queueExpunge(accountId: AccountId, folderId: Long) {
        val command: PendingCommand = PendingExpunge.create(folderId)
        queuePendingCommand(accountId, command)
    }

    @Throws(MessagingException::class)
    fun processPendingExpunge(command: PendingExpunge, accountId: AccountId) {
        val backend = getBackend(accountId)
        val folderServerId = getFolderServerId(accountId, command.folderId)
        backend.expunge(folderServerId)
    }

    @Throws(MessagingException::class)
    fun processPendingMarkAllAsRead(command: PendingMarkAllAsRead, accountId: AccountId) {
        val folderId = command.folderId
        val localStore = localStoreProvider.getInstance(accountId)
        val localFolder = localStore.getFolder(folderId)

        localFolder.open()
        val folderServerId = localFolder.getServerId()

        i(TAG, "Marking all messages in %s:%s as read", accountId, folderServerId)

        // TODO: Make this one database UPDATE operation
        val messages = localFolder.getMessages(false)
        for (message in messages) {
            if (!message.isSet(Flag.SEEN)) {
                message.setFlag(Flag.SEEN, true)
            }
        }

        for (l in this.listeners) {
            l.folderStatusChanged(accountId, folderId)
        }

        val backend = getBackend(accountId)
        if (backend.supportsFlags) {
            backend.markAllAsRead(folderServerId)
        }
    }

    fun markAllMessagesRead(accountId: AccountId, folderId: Long) {
        val command: PendingCommand = PendingMarkAllAsRead.create(folderId)
        queuePendingCommand(accountId, command)
        processPendingCommands(accountId)
    }

    fun setFlag(
        accountId: AccountId, messageIds: MutableList<Long?>, flag: Flag,
        newState: Boolean
    ) {
        setFlagInCache(accountId, messageIds, flag, newState)

        putBackground(
            "setFlag", null, Runnable { setFlagSynchronous(accountId, messageIds, flag, newState, false) }
        )
    }

    fun setFlagForThreads(
        accountId: AccountId, threadRootIds: MutableList<Long?>,
        flag: Flag, newState: Boolean
    ) {
        setFlagForThreadsInCache(accountId, threadRootIds, flag, newState)

        putBackground(
            "setFlagForThreads", null, Runnable { setFlagSynchronous(accountId, threadRootIds, flag, newState, true) }
        )
    }

    private fun setFlagSynchronous(
        accountId: AccountId, ids: MutableList<Long?>,
        flag: Flag, newState: Boolean, threadedList: Boolean
    ) {
        val localStore: LocalStore?
        try {
            localStore = localStoreProvider.getInstance(accountId)
        } catch (e: MessagingException) {
            e(TAG, e, "Couldn't get LocalStore instance")
            return
        }

        // Update affected messages in the database. This should be as fast as possible so the UI
        // can be updated with the new state.
        try {
            if (threadedList) {
                localStore.setFlagForThreads(ids, flag, newState)
                removeFlagForThreadsFromCache(accountId, ids, flag)
            } else {
                localStore.setFlag(ids, flag, newState)
                removeFlagFromCache(accountId, ids, flag)
            }
        } catch (e: MessagingException) {
            e(TAG, e, "Couldn't set flags in local database")
        }

        // Read folder ID and UID of messages from the database
        val folderMap: MutableMap<Long?, MutableList<String?>?>
        try {
            folderMap = localStore.getFolderIdsAndUids(ids, threadedList)
        } catch (e: MessagingException) {
            e(TAG, e, "Couldn't get folder name and UID of messages")
            return
        }

        val accountSupportsFlags = supportsFlags(accountId)

        // Loop over all folders
        for (entry in folderMap.entries) {
            val folderId: Long = entry.key!!
            val uids: MutableList<String> = entry.value

            // Notify listeners of changed folder status
            for (l in this.listeners) {
                l.folderStatusChanged(accountId, folderId)
            }

            if (flag == Flag.SEEN && newState) {
                cancelNotificationsForMessages(accountId, folderId, uids)
            }

            if (accountSupportsFlags) {
                val localFolder = localStore.getFolder(folderId)
                try {
                    localFolder.open()
                    if (!localFolder.isLocalOnly()) {
                        // Send flag change to server
                        queueSetFlag(accountId, folderId, newState, flag, uids)
                        processPendingCommands(accountId)
                    }
                } catch (e: MessagingException) {
                    e(TAG, e, "Couldn't open folder. Account: %s, folder ID: %d", accountId, folderId)
                }
            }
        }
    }

    private fun cancelNotificationsForMessages(accountId: AccountId, folderId: Long, uids: MutableList<String>) {
        for (uid in uids) {
            val messageReference = MessageReference(accountId, folderId, uid)
            notificationController.removeNewMailNotification(accountId, messageReference)
        }
    }

    /**
     * Set or remove a flag for a set of messages in a specific folder.
     *
     *
     * The [Message] objects passed in are updated to reflect the new flag state.
     *
     */
    fun setFlag(
        accountId: AccountId, folderId: Long, messages: MutableList<LocalMessage?>, flag: Flag?,
        newState: Boolean
    ) {
        // TODO: Put this into the background, but right now some callers depend on the message
        //       objects being modified right after this method returns.
        try {
            val localStore = localStoreProvider.getInstance(accountId)
            val localFolder = localStore.getFolder(folderId)
            localFolder.open()

            // Update the messages in the local store
            localFolder.setFlags(messages, mutableSetOf<Flag?>(flag), newState)

            for (l in this.listeners) {
                l.folderStatusChanged(accountId, folderId)
            }

            // Handle the remote side
            if (supportsFlags(accountId) && !localFolder.isLocalOnly()) {
                val uids: MutableList<String> = getUidsFromMessages(messages)
                queueSetFlag(accountId, folderId, newState, flag, uids)
                processPendingCommands(accountId)
            }
        } catch (me: MessagingException) {
            throw RuntimeException(me)
        }
    }

    /**
     * Set or remove a flag for a message referenced by message UID.
     */
    fun setFlag(accountId: AccountId, folderId: Long, uid: String?, flag: Flag?, newState: Boolean) {
        try {
            val localStore = localStoreProvider.getInstance(accountId)
            val localFolder = localStore.getFolder(folderId)
            localFolder.open()

            val message = localFolder.getMessage(uid)
            if (message != null) {
                setFlag(accountId, folderId, mutableListOf<LocalMessage?>(message), flag, newState)
            }
        } catch (me: MessagingException) {
            throw RuntimeException(me)
        }
    }

    fun loadMessageRemotePartial(
        accountId: AccountId, folderId: Long, uid: String,
        listener: MessagingListener?
    ) {
        put(
            "loadMessageRemotePartial",
            listener,
            Runnable { loadMessageRemoteSynchronous(accountId, folderId, uid, listener, true) }
        )
    }

    //TODO: Fix the callback mess. See GH-782
    fun loadMessageRemote(accountId: AccountId, folderId: Long, uid: String, listener: MessagingListener?) {
        put(
            "loadMessageRemote",
            listener,
            Runnable { loadMessageRemoteSynchronous(accountId, folderId, uid, listener, false) }
        )
    }

    private fun loadMessageRemoteSynchronous(
        accountId: AccountId, folderId: Long, messageServerId: String,
        listener: MessagingListener?, loadPartialFromSearch: Boolean
    ) {
        try {
            require(!messageServerId.startsWith(localMessageUidPrefixProvider.get())) { "Must not be called with a local UID" }

            val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)

            val folderServerId: String = messageStore.getFolderServerId(folderId)!!
            checkNotNull(folderServerId) { "Folder not found (ID: " + folderId + ")" }

            val backend = getBackend(accountId)

            if (loadPartialFromSearch) {
                val syncConfig = createSyncConfig(accountId)
                backend.downloadMessage(syncConfig, folderServerId, messageServerId)
            } else {
                backend.downloadCompleteMessageBlocking(IO, folderServerId, messageServerId)
            }

            for (l in getListeners(listener)) {
                l.loadMessageRemoteFinished(accountId, folderId, messageServerId)
            }
        } catch (e: Exception) {
            for (l in getListeners(listener)) {
                l.loadMessageRemoteFailed(accountId, folderId, messageServerId, e)
            }

            notifyUserIfCertificateProblem(accountId, e, true)
            e(TAG, e, "Error while loading remote message")
            syncDebugLogger.error("MessagingException", null) { "Error while loading remote message" }
        }
    }

    @Throws(MessagingException::class)
    fun loadMessage(accountId: AccountId, folderId: Long, uid: String?): LocalMessage {
        val localStore = localStoreProvider.getInstance(accountId)
        val localFolder = localStore.getFolder(folderId)
        localFolder.open()

        val message = localFolder.getMessage(uid)
        if (message == null || message.getDatabaseId() == 0L) {
            val folderName = localFolder.getName()
            throw IllegalArgumentException("Message not found: folder=" + folderName + ", uid=" + uid)
        }

        val fp = FetchProfile()
        fp.add(FetchProfile.Item.BODY)
        localFolder.fetch(mutableListOf<LocalMessage?>(message), fp, null)

        return message
    }

    @Throws(MessagingException::class)
    fun loadMessageMetadata(accountId: AccountId, folderId: Long, uid: String?): LocalMessage {
        val localStore = localStoreProvider.getInstance(accountId)
        val localFolder = localStore.getFolder(folderId)
        localFolder.open()

        val message = localFolder.getMessage(uid)
        if (message == null || message.getDatabaseId() == 0L) {
            val folderName = localFolder.getName()
            throw IllegalArgumentException("Message not found: folder=" + folderName + ", uid=" + uid)
        }

        val fp = FetchProfile()
        fp.add(FetchProfile.Item.ENVELOPE)
        localFolder.fetch(mutableListOf<LocalMessage?>(message), fp, null)

        return message
    }

    fun markMessageAsOpened(accountId: AccountId, message: LocalMessage) {
        threadPool.execute(
            Runnable { notificationController.removeNewMailNotification(accountId, message.makeMessageReference()) }
        )

        if (message.isSet(Flag.SEEN)) {
            // Nothing to do if the message is already marked as read
            return
        }

        val account = accountManager.findById(accountId)
        if (account == null) {
            return
        }

        val markMessageAsRead = account.isMarkMessageAsReadOnView
        if (markMessageAsRead) {
            // Mark the message itself as read right away
            try {
                message.setFlagInternal(Flag.SEEN, true)
            } catch (e: MessagingException) {
                e(TAG, e, "Error while marking message as read")
            }

            // Also mark the message as read in the cache
            val messageIds = mutableListOf<Long?>(message.getDatabaseId())
            setFlagInCache(accountId, messageIds, Flag.SEEN, true)
        }

        putBackground(
            "markMessageAsOpened", null,
            Runnable {
                markMessageAsOpenedBlocking(accountId, message, markMessageAsRead)
            }
        )
    }

    private fun markMessageAsOpenedBlocking(
        accountId: AccountId, message: LocalMessage,
        markMessageAsRead: Boolean
    ) {
        if (markMessageAsRead) {
            markMessageAsRead(accountId, message)
        } else {
            // Marking a message as read will automatically mark it as "not new". But if we don't mark the message
            // as read on opening, we have to manually mark it as "not new".
            markMessageAsNotNew(accountId, message)
        }
    }

    private fun markMessageAsRead(accountId: AccountId, message: LocalMessage) {
        val messageIds = mutableListOf<Long?>(message.getDatabaseId())
        setFlagSynchronous(accountId, messageIds, Flag.SEEN, true, false)
    }

    private fun markMessageAsNotNew(accountId: AccountId, message: LocalMessage) {
        val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)
        val folderId = message.getFolder().getDatabaseId()
        val messageServerId = message.getUid()
        messageStore.setNewMessageState(folderId, messageServerId, false)
    }

    fun clearNewMessages(accountId: AccountId) {
        put("clearNewMessages", null, Runnable { clearNewMessagesBlocking(accountId) })
    }

    private fun clearNewMessagesBlocking(accountId: AccountId) {
        val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.clearNewMessageState()
    }

    fun loadAttachment(
        accountId: AccountId, message: LocalMessage, part: Part,
        listener: MessagingListener?
    ) {
        put(
            "loadAttachment", listener,
            object : Runnable {
                override fun run() {
                    try {
                        val folderServerId = message.getFolder().getServerId()

                        val localStore = localStoreProvider.getInstance(accountId)
                        val localFolder = localStore.getFolder(folderServerId)

                        val bodyFactory = ProgressBodyFactory(
                            object : ProgressBodyFactory.ProgressListener {
                                override fun updateProgress(progress: Int) {
                                    for (listener in this.listeners) {
                                        listener.updateProgress(progress)
                                    }
                                }
                            }
                        )

                        val backend = getBackend(accountId)
                        backend.fetchPart(folderServerId, message.getUid(), part, bodyFactory)

                        localFolder.addPartToMessage(message, part)

                        for (l in getListeners(listener)) {
                            l.loadAttachmentFinished(accountId, message, part)
                        }
                    } catch (me: MessagingException) {
                        v(TAG, me, "Exception loading attachment")

                        for (l in getListeners(listener)) {
                            l.loadAttachmentFailed(accountId, message, part, me.message)
                        }
                        notifyUserIfCertificateProblem(accountId, me, true)
                    }
                }
            }
        )
    }

    /**
     * Stores the given message in the Outbox and starts a sendPendingMessages command to attempt to send the message.
     */
    fun sendMessage(
        accountId: AccountId, message: Message, plaintextSubject: String?,
        listener: MessagingListener?
    ) {
        try {
            val outboxFolderId = outboxFolderManager
                .getOutboxFolderIdSync(
                    accountId,
                    true
                )

            message.setFlag(Flag.SEEN, true)

            val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)
            val messageData = saveMessageDataCreator.createSaveMessageData(
                message, MessageDownloadState.FULL, plaintextSubject
            )
            val messageId = messageStore.saveLocalMessage(outboxFolderId, messageData, null)

            val localStore = localStoreProvider.getInstance(accountId)
            val outboxStateRepository = localStore.getOutboxStateRepository()
            outboxStateRepository.initializeOutboxState(messageId)

            sendPendingMessages(accountId, listener)
        } catch (e: Exception) {
            e(TAG, e, "Error sending message")
        }
    }

    @Throws(MessagingException::class)
    fun sendMessageBlocking(accountId: AccountId, message: Message) {
        val backend = getBackend(accountId)
        backend.sendMessage(message)
    }

    /**
     * Attempt to send any messages that are sitting in the Outbox.
     */
    fun sendPendingMessages(
        accountId: AccountId,
        listener: MessagingListener?
    ) {
        putBackground(
            "sendPendingMessages", listener,
            object : Runnable {
                override fun run() {
                    if (outboxFolderManager.hasPendingMessagesSync(accountId)) {
                        showSendingNotificationIfNecessary(accountId)

                        try {
                            sendPendingMessagesSynchronous(accountId)
                        } finally {
                            clearSendingNotificationIfNecessary(accountId)
                        }
                    }
                }
            }
        )
    }

    private fun showSendingNotificationIfNecessary(accountId: AccountId) {
        val account = accountManager.findById(accountId)
        if (account != null && account.isNotifySync) {
            notificationController.showSendingNotification(accountId)
        }
    }

    private fun clearSendingNotificationIfNecessary(accountId: AccountId) {
        val account = accountManager.findById(accountId)
        if (account != null && account.isNotifySync) {
            notificationController.clearSendingNotification(accountId)
        }
    }

    private fun messagesPendingSend(accountId: AccountId): Boolean {
        val outboxFolderId = outboxFolderManager
            .getOutboxFolderIdSync(
                accountId,
                true
            )
        if (outboxFolderId == -1L) {
            w(TAG, "Could not get Outbox folder ID from Account")
            return false
        }

        val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)
        return messageStore.getMessageCount(outboxFolderId) > 0
    }

    /**
     * Attempt to send any messages that are sitting in the Outbox.
     */
    @VisibleForTesting
    protected fun sendPendingMessagesSynchronous(accountId: AccountId) {
        var lastFailure: Exception? = null
        try {
            if (isAuthenticationProblem(accountId, false)) {
                d(TAG, "Authentication will fail. Skip sending messages.")
                handleAuthenticationFailure(accountId, false)
                return
            }

            val localStore = localStoreProvider.getInstance(accountId)
            val outboxStateRepository = localStore.getOutboxStateRepository()
            val outboxFolderId = outboxFolderManager
                .getOutboxFolderIdSync(
                    accountId,
                    true
                )
            val localFolder = localStore.getFolder(outboxFolderId)
            if (!localFolder.exists()) {
                w(TAG, "Outbox does not exist")
                return
            }

            localFolder.open()

            val localMessages = localFolder.getMessages()
            var progress = 0
            val todo = localMessages.size
            for (l in this.listeners) {
                l.synchronizeMailboxProgress(accountId, outboxFolderId, progress, todo)
            }
            /*
             * The profile we will use to pull all of the content
             * for a given local message into memory for sending.
             */
            val fp = FetchProfile()
            fp.add(FetchProfile.Item.ENVELOPE)
            fp.add(FetchProfile.Item.BODY)

            i(TAG, "Scanning Outbox folder for messages to send")

            val backend = getBackend(accountId)

            for (message in localMessages) {
                if (message.isSet(Flag.DELETED)) {
                    //FIXME: When uploading a message to the remote Sent folder the move code creates a placeholder
                    // message in the Outbox. This code gets rid of these messages. It'd be preferable if the
                    // placeholder message was never created, though.
                    message.destroy()
                    continue
                }
                try {
                    val messageId = message.getDatabaseId()
                    val outboxState = outboxStateRepository.getOutboxState(messageId)

                    val sendState = outboxState.sendState
                    if (sendState != SendState.READY) {
                        v(
                            TAG, "Skipping sending message %s (reason: %s - %s)", message.getUid(),
                            sendState.databaseName, outboxState.sendError
                        )

                        if (sendState == SendState.RETRIES_EXCEEDED) {
                            lastFailure = MessagingException("Retries exceeded", true)
                        } else {
                            lastFailure = MessagingException(outboxState.sendError, true)
                        }
                        continue
                    }

                    i(
                        TAG, "Send count for message %s is %d", message.getUid(),
                        outboxState.numberOfSendAttempts
                    )

                    localFolder.fetch(mutableListOf<LocalMessage?>(message), fp, null)
                    try {
                        if (message.getHeader(K9.IDENTITY_HEADER).size > 0 || message.isSet(Flag.DRAFT)) {
                            v(
                                TAG,
                                "The user has set the Outbox and Drafts folder to the same thing. " +
                                    "This message appears to be a draft, so K-9 will not send it"
                            )
                            continue
                        }

                        outboxStateRepository.incrementSendAttempts(messageId)
                        message.setFlag(Flag.X_SEND_IN_PROGRESS, true)

                        i(TAG, "Sending message with UID %s", message.getUid())
                        backend.sendMessage(message)

                        message.setFlag(Flag.X_SEND_IN_PROGRESS, false)
                        message.setFlag(Flag.SEEN, true)
                        progress++
                        for (l in this.listeners) {
                            l.synchronizeMailboxProgress(accountId, outboxFolderId, progress, todo)
                        }
                        moveOrDeleteSentMessage(accountId, localStore, message)

                        outboxStateRepository.removeOutboxState(messageId)
                    } catch (e: AuthenticationFailedException) {
                        outboxStateRepository.decrementSendAttempts(messageId)
                        lastFailure = e

                        handleAuthenticationFailure(accountId, false)
                        handleSendFailure(accountId, localFolder, message, e)
                    } catch (e: CertificateValidationException) {
                        outboxStateRepository.decrementSendAttempts(messageId)
                        lastFailure = e

                        notifyUserIfCertificateProblem(accountId, e, false)
                        handleSendFailure(accountId, localFolder, message, e)
                    } catch (e: MessagingException) {
                        lastFailure = e

                        if (e.isPermanentFailure) {
                            val errorMessage = e.message
                            outboxStateRepository.setSendAttemptError(messageId, errorMessage!!)
                        } else if (outboxState.numberOfSendAttempts + 1 >= MAX_SEND_ATTEMPTS) {
                            outboxStateRepository.setSendAttemptsExceeded(messageId)
                        }

                        handleSendFailure(accountId, localFolder, message, e)
                    } catch (e: Exception) {
                        lastFailure = e

                        handleSendFailure(accountId, localFolder, message, e)
                    }
                } catch (e: Exception) {
                    lastFailure = e

                    e(TAG, e, "Failed to fetch message for sending")
                    notifySynchronizeMailboxFailed(accountId, localFolder, e)
                }
            }

            if (lastFailure != null) {
                notificationController.showSendFailedNotification(accountId, lastFailure)
            }
        } catch (e: Exception) {
            v(TAG, e, "Failed to send pending messages")
        } finally {
            if (lastFailure == null) {
                notificationController.clearSendFailedNotification(accountId)
            }
        }
    }

    @Throws(MessagingException::class)
    private fun moveOrDeleteSentMessage(
        accountId: AccountId, localStore: LocalStore, message: LocalMessage
    ) {
        val account = accountManager.findById(accountId)
        if (!account!!.hasSentFolder() || !account.isUploadSentMessages) {
            i(TAG, "Not uploading sent message; deleting local message")
            message.destroy()
        } else {
            val sentFolderId: Long = account.sentFolderId!!
            val sentFolder = localStore.getFolder(sentFolderId)
            sentFolder.open()
            val sentFolderServerId = sentFolder.getServerId()
            i(TAG, "Moving sent message to folder '%s' (%d)", sentFolderServerId, sentFolderId)

            val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)
            val destinationMessageId = messageStore.moveMessage(message.getDatabaseId(), sentFolderId)

            i(TAG, "Moved sent message to folder '%s' (%d)", sentFolderServerId, sentFolderId)

            if (!sentFolder.isLocalOnly()) {
                val destinationUid = messageStore.getMessageServerId(destinationMessageId)
                if (destinationUid != null) {
                    val command: PendingCommand = PendingAppend.create(sentFolderId, destinationUid)
                    queuePendingCommand(accountId, command)
                    processPendingCommands(accountId)
                }
            }
        }

        val outboxFolderId = outboxFolderManager
            .getOutboxFolderIdSync(
                accountId,
                true
            )
        for (listener in this.listeners) {
            listener.folderStatusChanged(accountId, outboxFolderId)
        }
    }

    @Throws(MessagingException::class)
    private fun handleSendFailure(
        accountId: AccountId?, localFolder: LocalFolder, message: Message,
        exception: Exception
    ) {
        e(TAG, exception, "Failed to send message")
        message.setFlag(Flag.X_SEND_FAILED, true)

        notifySynchronizeMailboxFailed(accountId, localFolder, exception)
    }

    private fun notifySynchronizeMailboxFailed(
        accountId: AccountId?, localFolder: LocalFolder,
        exception: Exception
    ) {
        val folderId = localFolder.getDatabaseId()
        val errorMessage = exception.rootCauseMessage
        for (listener in this.listeners) {
            listener.synchronizeMailboxFailed(accountId, folderId, errorMessage)
        }
    }

    fun isMoveCapable(messageReference: MessageReference): Boolean {
        return !messageReference.uid.startsWith(localMessageUidPrefixProvider.get())
    }

    fun isCopyCapable(message: MessageReference): Boolean {
        return isMoveCapable(message)
    }

    fun isMoveCapable(accountId: AccountId): Boolean {
        val backend = getBackendOrNull(accountId)
        return backend != null && backend.supportsMove
    }

    fun isCopyCapable(accountId: AccountId): Boolean {
        val backend = getBackendOrNull(accountId)
        return backend != null && backend.supportsCopy
    }

    fun isPushCapable(accountId: AccountId): Boolean {
        val backend = getBackendOrNull(accountId)
        return backend != null && backend.isPushCapable
    }

    fun supportsFlags(accountId: AccountId): Boolean {
        return getBackend(accountId).supportsFlags
    }

    fun supportsExpunge(accountId: AccountId): Boolean {
        val backend = getBackendOrNull(accountId)
        return backend != null && backend.supportsExpunge
    }

    fun supportsSearchByDate(accountId: AccountId): Boolean {
        return getBackend(accountId).supportsSearchByDate
    }

    fun supportsUpload(accountId: AccountId): Boolean {
        return getBackend(accountId).supportsUpload
    }

    fun supportsFolderSubscriptions(accountId: AccountId): Boolean {
        return getBackend(accountId).supportsMove
    }

    fun moveMessages(
        srcAccountId: AccountId, srcFolderId: Long,
        messageReferences: MutableList<MessageReference?>, destFolderId: Long
    ) {
        actOnMessageGroup(
            srcAccountId, srcFolderId, messageReferences,
            MessageActor { accountId: AccountId?, messageFolder: LocalFolder?, messages: MutableList<LocalMessage>? ->
                suppressMessages(accountId!!, messages!!)
                putBackground(
                    "moveMessages",
                    null,
                    Runnable {
                        moveOrCopyMessageSynchronous(
                            accountId,
                            srcFolderId,
                            messages,
                            destFolderId,
                            MoveOrCopyFlavor.MOVE
                        )
                    }
                )
            }
        )
    }

    fun moveMessagesInThread(
        srcAccountId: AccountId, srcFolderId: Long,
        messageReferences: MutableList<MessageReference?>, destFolderId: Long
    ) {
        actOnMessageGroup(
            srcAccountId, srcFolderId, messageReferences,
            MessageActor { accountId: AccountId?, messageFolder: LocalFolder?, messages: MutableList<LocalMessage>? ->
                suppressMessages(accountId!!, messages!!)
                putBackground(
                    "moveMessagesInThread", null,
                    Runnable {
                        try {
                            val messagesInThreads = collectMessagesInThreads(accountId, messages)
                            moveOrCopyMessageSynchronous(
                                accountId, srcFolderId, messagesInThreads, destFolderId,
                                MoveOrCopyFlavor.MOVE
                            )
                        } catch (e: MessagingException) {
                            e(TAG, e, "Exception while moving messages")
                        }
                    }
                )
            }
        )
    }

    fun moveMessage(accountId: AccountId, srcFolderId: Long, message: MessageReference?, destFolderId: Long) {
        moveMessages(accountId, srcFolderId, mutableListOf<MessageReference?>(message), destFolderId)
    }

    fun copyMessages(
        srcAccountId: AccountId,
        srcFolderId: Long,
        messageReferences: MutableList<MessageReference?>,
        destFolderId: Long
    ) {
        actOnMessageGroup(
            srcAccountId, srcFolderId, messageReferences,
            MessageActor { accountId: AccountId?, messageFolder: LocalFolder?, messages: MutableList<LocalMessage>? ->
                putBackground(
                    "copyMessages",
                    null,
                    Runnable {
                        moveOrCopyMessageSynchronous(
                            srcAccountId,
                            srcFolderId,
                            messages!!,
                            destFolderId,
                            MoveOrCopyFlavor.COPY
                        )
                    }
                )
            }
        )
    }

    fun copyMessagesInThread(
        srcAccountId: AccountId,
        srcFolderId: Long,
        messageReferences: MutableList<MessageReference?>,
        destFolderId: Long
    ) {
        actOnMessageGroup(
            srcAccountId, srcFolderId, messageReferences,
            MessageActor { accountId: AccountId?, messageFolder: LocalFolder?, messages: MutableList<LocalMessage>? ->
                putBackground(
                    "copyMessagesInThread", null,
                    Runnable {
                        try {
                            val messagesInThreads = collectMessagesInThreads(accountId!!, messages!!)
                            moveOrCopyMessageSynchronous(
                                accountId, srcFolderId, messagesInThreads, destFolderId,
                                MoveOrCopyFlavor.COPY
                            )
                        } catch (e: MessagingException) {
                            e(TAG, e, "Exception while copying messages")
                        }
                    }
                )
            }
        )
    }

    fun copyMessage(accountId: AccountId, srcFolderId: Long, message: MessageReference?, destFolderId: Long) {
        copyMessages(accountId, srcFolderId, mutableListOf<MessageReference?>(message), destFolderId)
    }

    fun moveOrCopyMessageSynchronous(
        accountId: AccountId, srcFolderId: Long, inMessages: MutableList<LocalMessage>,
        destFolderId: Long, operation: MoveOrCopyFlavor
    ) {
        try {
            val localStore = localStoreProvider.getInstance(accountId)
            if (operation == MoveOrCopyFlavor.MOVE && !isMoveCapable(accountId)) {
                return
            }
            if (operation == MoveOrCopyFlavor.COPY && !isCopyCapable(accountId)) {
                return
            }

            val localSrcFolder = localStore.getFolder(srcFolderId)
            localSrcFolder.open()

            val localDestFolder = localStore.getFolder(destFolderId)
            localDestFolder.open()

            var unreadCountAffected = false
            val uids: MutableList<String?> = LinkedList<String?>()
            for (message in inMessages) {
                val uid = message.getUid()
                if (!uid.startsWith(localMessageUidPrefixProvider.get())) {
                    uids.add(uid)
                }

                if (operation == MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ) {
                    if (!message.isSet(Flag.SEEN)) {
                        unreadCountAffected = true
                        message.setFlag(Flag.SEEN, true)
                    }
                } else {
                    if (!unreadCountAffected && !message.isSet(Flag.SEEN)) {
                        unreadCountAffected = true
                    }
                }
            }

            val messages = localSrcFolder.getMessagesByUids(uids)
            if (messages.size > 0) {
                i(
                    TAG,
                    "moveOrCopyMessageSynchronous: source folder = %s, %d messages, destination folder = %s, " +
                        "operation = %s",
                    srcFolderId, messages.size, destFolderId, operation.name
                )

                val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)

                val messageIds: MutableList<Long?> = ArrayList<Long?>()
                val messageIdToUidMapping: MutableMap<Long?, String?> = HashMap<Long?, String?>()
                for (message in messages) {
                    val messageId = message.getDatabaseId()
                    messageIds.add(messageId)
                    messageIdToUidMapping.put(messageId, message.getUid())
                }

                val resultIdMapping: MutableMap<Long?, Long?>?
                if (operation == MoveOrCopyFlavor.COPY) {
                    resultIdMapping = messageStore.copyMessages(messageIds, destFolderId)

                    if (unreadCountAffected) {
                        // If this copy operation changes the unread count in the destination
                        // folder, notify the listeners.
                        for (l in this.listeners) {
                            l.folderStatusChanged(accountId, destFolderId)
                        }
                    }
                } else {
                    resultIdMapping = messageStore.moveMessages(messageIds, destFolderId)

                    unsuppressMessages(accountId, messages)

                    if (unreadCountAffected) {
                        // If this move operation changes the unread count, notify the listeners
                        // that the unread count changed in both the source and destination folder.
                        for (l in this.listeners) {
                            l.folderStatusChanged(accountId, srcFolderId)
                            l.folderStatusChanged(accountId, destFolderId)
                        }
                    }
                }

                val destinationMapping: MutableMap<Long?, String?> =
                    messageStore.getMessageServerIds(resultIdMapping.values)

                val uidMap: MutableMap<String?, String?> = HashMap<String?, String?>()
                for (entry in resultIdMapping.entries) {
                    val sourceMessageId = entry.key
                    val destinationMessageId = entry.value

                    val sourceUid = messageIdToUidMapping.get(sourceMessageId)
                    val destinationUid = destinationMapping.get(destinationMessageId)
                    uidMap.put(sourceUid, destinationUid)
                }

                queueMoveOrCopy(
                    accountId, localSrcFolder.getDatabaseId(), localDestFolder.getDatabaseId(),
                    operation, uidMap
                )
            }

            processPendingCommands(accountId)
        } catch (me: MessagingException) {
            throw RuntimeException("Error moving message", me)
        }
    }

    fun moveToDraftsFolder(accountId: AccountId, folderId: Long, messages: MutableList<MessageReference>) {
        putBackground("moveToDrafts", null, Runnable { moveToDraftsFolderInBackground(accountId, folderId, messages) })
    }

    private fun moveToDraftsFolderInBackground(
        accountId: AccountId, folderId: Long,
        messages: MutableList<MessageReference>
    ) {
        for (messageReference in messages) {
            try {
                val message: Message = loadMessage(accountId, folderId, messageReference.uid)
                val draftMessageId = saveDraft(accountId, message, null, message.getSubject())

                val draftSavedSuccessfully = draftMessageId != null
                if (draftSavedSuccessfully) {
                    message.destroy()
                }

                for (listener in this.listeners) {
                    listener.folderStatusChanged(accountId, folderId)
                }
            } catch (e: MessagingException) {
                e(TAG, e, "Error loading message. Draft was not saved.")
            }
        }
    }

    fun archiveThreads(messages: MutableList<MessageReference?>) {
        archiveOperations.archiveThreads(messages)
    }

    fun archiveMessages(messages: MutableList<MessageReference?>) {
        archiveOperations.archiveMessages(messages)
    }

    fun archiveMessage(message: MessageReference) {
        archiveOperations.archiveMessage(message)
    }

    fun expunge(accountId: AccountId, folderId: Long) {
        putBackground(
            "expunge", null,
            Runnable {
                queueExpunge(accountId, folderId)
                processPendingCommands(accountId)
            }
        )
    }

    fun deleteDraftSkippingTrashFolder(accountId: AccountId, messageId: Long) {
        deleteDraft(accountId, messageId, true)
    }

    fun deleteDraft(accountId: AccountId, messageId: Long) {
        deleteDraft(accountId, messageId, false)
    }

    private fun deleteDraft(accountId: AccountId, messageId: Long, skipTrashFolder: Boolean) {
        val account = accountManager.findById(accountId)
        if (account == null) {
            w(TAG, "Account not found %s", accountId)
            return
        }
        val folderId = account.draftsFolderId
        if (folderId == null) {
            w(TAG, "No Drafts folder configured. Can't delete draft.")
            return
        }

        val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)
        val messageServerId = messageStore.getMessageServerId(messageId)
        if (messageServerId != null) {
            val messageReference = MessageReference(accountId, folderId, messageServerId)
            deleteMessages(mutableListOf<MessageReference?>(messageReference), skipTrashFolder)
        }
    }

    fun deleteThreads(messages: MutableList<MessageReference?>) {
        actOnMessagesGroupedByAccountAndFolder(
            messages,
            MessageActor { accountId: AccountId?, messageFolder: LocalFolder?, accountMessages: MutableList<LocalMessage>? ->
                suppressMessages(accountId!!, accountMessages!!)
                putBackground(
                    "deleteThreads",
                    null,
                    Runnable {
                        deleteThreadsSynchronous(
                            accountId,
                            messageFolder!!.getDatabaseId(),
                            accountMessages,
                            false
                        )
                    }
                )
            }
        )
    }

    private fun deleteThreadsSynchronous(
        accountId: AccountId, folderId: Long, messages: MutableList<LocalMessage>,
        skipTrashFolder: Boolean
    ) {
        try {
            val messagesToDelete = collectMessagesInThreads(accountId, messages)
            deleteMessagesSynchronous(accountId, folderId, messagesToDelete, skipTrashFolder)
        } catch (e: MessagingException) {
            e(TAG, e, "Something went wrong while deleting threads")
        }
    }

    @Throws(MessagingException::class)
    fun collectMessagesInThreads(accountId: AccountId, messages: MutableList<LocalMessage>): MutableList<LocalMessage> {
        val localStore = localStoreProvider.getInstance(accountId)

        val messagesInThreads: MutableList<LocalMessage> = ArrayList<LocalMessage>()
        for (localMessage in messages) {
            val rootId = localMessage.getRootId()
            val threadId = if (rootId == -1L) localMessage.getThreadId() else rootId

            val messagesInThread = localStore.getMessagesInThread(threadId)

            messagesInThreads.addAll(messagesInThread)
        }

        return messagesInThreads
    }

    fun deleteMessage(message: MessageReference?) {
        deleteMessages(mutableListOf<MessageReference?>(message), false)
    }

    fun deleteMessages(messages: MutableList<MessageReference?>) {
        deleteMessages(messages, false)
    }

    private fun deleteMessages(messages: MutableList<MessageReference?>, skipTrashFolder: Boolean) {
        actOnMessagesGroupedByAccountAndFolder(
            messages,
            MessageActor { accountId: AccountId?, messageFolder: LocalFolder?, accountMessages: MutableList<LocalMessage>? ->
                suppressMessages(accountId!!, accountMessages!!)
                putBackground(
                    "deleteMessages",
                    null,
                    Runnable {
                        deleteMessagesSynchronous(
                            accountId,
                            messageFolder!!.getDatabaseId(),
                            accountMessages,
                            skipTrashFolder
                        )
                    }
                )
            }
        )
    }

    private fun deleteMessagesSynchronous(
        accountId: AccountId, folderId: Long, messages: MutableList<LocalMessage>,
        skipTrashFolder: Boolean
    ) {
        try {
            val localOnlyMessages: MutableList<LocalMessage?> = ArrayList<LocalMessage?>()
            val syncedMessages: MutableList<LocalMessage?> = ArrayList<LocalMessage?>()
            val syncedMessageUids: MutableList<String> = ArrayList<String>()
            for (message in messages) {
                notificationController.removeNewMailNotification(accountId, message.makeMessageReference())

                val uid = message.getUid()
                if (uid.startsWith(localMessageUidPrefixProvider.get())) {
                    localOnlyMessages.add(message)
                } else {
                    syncedMessages.add(message)
                    syncedMessageUids.add(uid)
                }
            }

            val backend = getBackend(accountId)

            val localStore = localStoreProvider.getInstance(accountId)
            val localFolder = localStore.getFolder(folderId)
            localFolder.open()

            var uidMap: MutableMap<String?, String?>? = null
            val account = accountManager.findById(accountId)
            if (account == null) {
                w(TAG, "Account not found %s", accountId)
                return
            }
            val trashFolderId = account.trashFolderId
            val doNotMoveToTrashFolder = skipTrashFolder ||
                localDeleteOperationDecider.isDeleteImmediately(accountId, folderId)

            var localTrashFolder: LocalFolder? = null
            if (doNotMoveToTrashFolder) {
                d(TAG, "Not moving deleted messages to local Trash folder. Removing local copies.")

                if (!localOnlyMessages.isEmpty()) {
                    localFolder.destroyMessages(localOnlyMessages)
                }
                if (!syncedMessages.isEmpty()) {
                    localFolder.setFlags(syncedMessages, mutableSetOf<Flag?>(Flag.DELETED), true)
                }
            } else {
                d(TAG, "Deleting messages in normal folder, moving")
                localTrashFolder = localStore.getFolder(trashFolderId!!)

                val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)

                val messageIds: MutableList<Long?> = ArrayList<Long?>()
                val messageIdToUidMapping: MutableMap<Long?, String?> = HashMap<Long?, String?>()
                for (message in messages) {
                    val messageId = message.getDatabaseId()
                    messageIds.add(messageId)
                    messageIdToUidMapping.put(messageId, message.getUid())
                }

                val moveMessageIdMapping: MutableMap<Long?, Long?> =
                    messageStore.moveMessages(messageIds, trashFolderId)

                val destinationMapping: MutableMap<Long?, String?> =
                    messageStore.getMessageServerIds(moveMessageIdMapping.values)
                uidMap = HashMap<String?, String?>()
                for (entry in moveMessageIdMapping.entries) {
                    val sourceMessageId: Long = entry.key!!
                    val destinationMessageId: Long = entry.value!!

                    val sourceUid = messageIdToUidMapping.get(sourceMessageId)
                    val destinationUid = destinationMapping.get(destinationMessageId)
                    uidMap.put(sourceUid, destinationUid)
                }

                if (account.isMarkMessageAsReadOnDelete) {
                    val destinationMessageIds = moveMessageIdMapping.values
                    messageStore.setFlag(destinationMessageIds, Flag.SEEN, true)
                }
            }

            for (l in this.listeners) {
                l.folderStatusChanged(accountId, folderId)
                if (localTrashFolder != null) {
                    l.folderStatusChanged(accountId, trashFolderId!!)
                }
            }

            d(TAG, "Delete policy for account %s is %s", accountId, account.deletePolicy)

            val outboxFolderId = outboxFolderManager
                .getOutboxFolderIdSync(
                    accountId,
                    true
                )

            if (outboxFolderId != -1L && folderId == outboxFolderId && supportsUpload(accountId)) {
                for (destinationUid in uidMap!!.values) {
                    // If the message was in the Outbox, then it has been copied to local Trash, and has
                    // to be copied to remote trash
                    val command: PendingCommand = PendingAppend.create(trashFolderId!!, destinationUid)
                    queuePendingCommand(accountId, command)
                }
                processPendingCommands(accountId)
            } else if (localFolder.isLocalOnly()) {
                // Nothing to do on the remote side
            } else if (!syncedMessageUids.isEmpty()) {
                if (account.deletePolicy == DeletePolicy.ON_DELETE) {
                    if (doNotMoveToTrashFolder || !backend.supportsTrashFolder) {
                        queueDelete(accountId, folderId, syncedMessageUids)
                    } else if (account.isMarkMessageAsReadOnDelete) {
                        queueMoveOrCopy(
                            accountId, folderId, trashFolderId!!,
                            MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ, uidMap
                        )
                    } else {
                        queueMoveOrCopy(
                            accountId, folderId, trashFolderId!!,
                            MoveOrCopyFlavor.MOVE, uidMap
                        )
                    }
                    processPendingCommands(accountId)
                } else if (account.deletePolicy == DeletePolicy.MARK_AS_READ) {
                    queueSetFlag(accountId, localFolder.getDatabaseId(), true, Flag.SEEN, syncedMessageUids)
                    processPendingCommands(accountId)
                } else {
                    d(TAG, "Delete policy %s prevents delete from server", account.deletePolicy)
                }
            }

            unsuppressMessages(accountId, messages)
        } catch (me: MessagingException) {
            throw RuntimeException("Error deleting message from local store.", me)
        }
    }

    @Throws(MessagingException::class)
    fun processPendingEmptySpam(accountId: AccountId) {
        val account = accountManager.findById(accountId)
        if (account == null || !account.hasSpamFolder()) {
            return
        }

        val spamFolderId = account.spamFolderId
        if (spamFolderId == null) {
            return
        }

        val localStore = localStoreProvider.getInstance(accountId)
        val folder = localStore.getFolder(spamFolderId)
        folder.open()
        val spamFolderServerId = folder.getServerId()

        val backend = getBackend(accountId)
        backend.deleteAllMessages(spamFolderServerId)

        // Remove all messages marked as deleted
        folder.destroyDeletedMessages()

        compact(accountId)
    }

    fun emptySpam(accountId: AccountId, listener: MessagingListener?) {
        putBackground(
            "emptySpam", listener,
            object : Runnable {
                override fun run() {
                    try {
                        val account = accountManager.findById(accountId)
                        if (account == null) {
                            return
                        }
                        val spamFolderId = account.spamFolderId
                        if (spamFolderId == null) {
                            w(TAG, "No Spam folder configured. Can't empty spam.")
                            return
                        }

                        val localStore = localStoreProvider.getInstance(accountId)
                        val localFolder = localStore.getFolder(spamFolderId)
                        localFolder.open()

                        localFolder.destroyLocalOnlyMessages()
                        localFolder.setFlags(mutableSetOf<Flag?>(Flag.DELETED), true)

                        for (l in this.listeners) {
                            l.folderStatusChanged(accountId, spamFolderId)
                        }

                        val command: PendingCommand = PendingEmptySpam.create()
                        queuePendingCommand(accountId, command)
                        processPendingCommands(accountId)
                    } catch (e: Exception) {
                        e(TAG, e, "emptySpam failed")
                    }
                }
            }
        )
    }

    @Throws(MessagingException::class)
    fun processPendingEmptyTrash(accountId: AccountId) {
        val account = accountManager.findById(accountId)
        if (account == null || !account.hasTrashFolder()) {
            return
        }

        val trashFolderId = account.trashFolderId
        if (trashFolderId == null) {
            return
        }

        val localStore = localStoreProvider.getInstance(accountId)
        val folder = localStore.getFolder(trashFolderId)
        folder.open()
        val trashFolderServerId = folder.getServerId()

        val backend = getBackend(accountId)
        backend.deleteAllMessages(trashFolderServerId)

        // Remove all messages marked as deleted
        folder.destroyDeletedMessages()

        compact(accountId)
    }

    fun emptyTrash(accountId: AccountId, listener: MessagingListener?) {
        putBackground(
            "emptyTrash", listener,
            object : Runnable {
                override fun run() {
                    try {
                        val account = accountManager.findById(accountId)
                        if (account == null) {
                            return
                        }
                        val trashFolderId = account.trashFolderId
                        if (trashFolderId == null) {
                            w(TAG, "No Trash folder configured. Can't empty trash.")
                            return
                        }

                        val localStore = localStoreProvider.getInstance(accountId)
                        val localFolder = localStore.getFolder(trashFolderId)
                        localFolder.open()

                        val isTrashLocalOnly = isTrashLocalOnly(accountId)
                        if (isTrashLocalOnly) {
                            localFolder.clearAllMessages()
                        } else {
                            localFolder.destroyLocalOnlyMessages()
                            localFolder.setFlags(mutableSetOf<Flag?>(Flag.DELETED), true)
                        }

                        for (l in this.listeners) {
                            l.folderStatusChanged(accountId, trashFolderId)
                        }

                        if (!isTrashLocalOnly) {
                            val command: PendingCommand = PendingEmptyTrash.create()
                            queuePendingCommand(accountId, command)
                            processPendingCommands(accountId)
                        }
                    } catch (e: Exception) {
                        e(TAG, e, "emptyTrash failed")
                    }
                }
            }
        )
    }

    fun clearFolder(accountId: AccountId, folderId: Long) {
        putBackground(
            "clearFolder", null, Runnable { clearFolderSynchronous(accountId, folderId) }
        )
    }

    @VisibleForTesting
    protected fun clearFolderSynchronous(accountId: AccountId, folderId: Long) {
        try {
            val localFolder = localStoreProvider.getInstance(accountId).getFolder(folderId)
            localFolder.open()
            localFolder.clearAllMessages()
        } catch (e: Exception) {
            e(TAG, e, "clearFolder failed")
        }
    }

    /**
     * Find out whether the account type only supports a local Trash folder.
     *
     *
     *
     * Note: Currently this is only the case for POP3 accounts.
     *
     * @param accountId The AccountId to check.
     * @return `true` if the account only has a local Trash folder that is not synchronized with a folder on the
     * server. `false` otherwise.
     */
    private fun isTrashLocalOnly(accountId: AccountId): Boolean {
        val backend = getBackend(accountId)
        return !backend.supportsTrashFolder
    }

    fun performPeriodicMailSync(accountId: AccountId): Boolean {
        val latch = CountDownLatch(1)
        val syncError = MutableBoolean(false)
        checkMail(
            accountId, false, false, true,
            object : SimpleMessagingListener() {
                override fun checkMailFinished(context: Context?, accountId: AccountId?) {
                    latch.countDown()
                }

                override fun synchronizeMailboxFailed(accountId: AccountId?, folderId: Long, message: String?) {
                    syncError.value = true
                }
            }
        )

        v(TAG, "performPeriodicMailSync(%s) about to await latch release", accountId)

        try {
            latch.await()
            v(TAG, "performPeriodicMailSync(%s) got latch release", accountId)
        } catch (e: Exception) {
            e(TAG, e, "Interrupted while awaiting latch release")
        }

        val success = !syncError.value
        if (success) {
            val now = System.currentTimeMillis()
            val updatedAccount = accountManager.updateSync(
                accountId
            ) { account: LegacyAccount -> account.updateLastSyncTime(now) }
            if (updatedAccount == null) {
                w(TAG, "Account not found %s", accountId)
            } else {
                v(TAG, "Account %s successfully synced @ %tc", accountId, now)
            }
        }

        return success
    }

    /**
     * Checks mail for one or multiple accounts. If account is null all accounts are checked.
     */
    override fun checkMail(
        accountId: AccountId?, ignoreLastCheckedTime: Boolean, useManualWakeLock: Boolean,
        notify: Boolean,
        listener: MessagingListener
    ) {
        val wakeLock: WakeLock?
        if (useManualWakeLock) {
            val pm = get<PowerManager>(PowerManager::class.java)

            wakeLock = pm.newWakeLock("K9 MessagingController.checkMail")
            wakeLock.setReferenceCounted(false)
            wakeLock.acquire(K9.MANUAL_WAKE_LOCK_TIMEOUT.toLong())
        } else {
            wakeLock = null
        }

        for (l in getListeners(listener)) {
            l.checkMailStarted(context, accountId)
        }
        putBackground(
            "checkMail", listener,
            object : Runnable {
                override fun run() {
                    try {
                        i(TAG, "Starting mail check")

                        val accountIds: MutableCollection<AccountId>?
                        if (accountId != null) {
                            accountIds = ArrayList<AccountId>(1)
                            accountIds.add(accountId)
                        } else {
                            val accounts: MutableList<LegacyAccount> = accountManager.findAll()
                            accountIds = ArrayList<AccountId>(accounts.size)
                            for (account in accountManager.findAll()) {
                                accountIds.add(account.id)
                            }
                        }

                        for (synAccountId in accountIds) {
                            checkMailForAccount(synAccountId, ignoreLastCheckedTime, notify, listener)
                        }
                    } catch (e: Exception) {
                        e(TAG, e, "Unable to synchronize mail")
                    }
                    putBackground(
                        "finalize sync", null,
                        object : Runnable {
                            override fun run() {
                                i(TAG, "Finished mail sync")

                                if (wakeLock != null) {
                                    wakeLock.release()
                                }
                                for (l in getListeners(listener)) {
                                    l.checkMailFinished(context, accountId)
                                }
                            }
                        }
                    )
                }
            }
        )
    }

    private fun checkMailForAccount(
        accountId: AccountId, ignoreLastCheckedTime: Boolean, notify: Boolean,
        listener: MessagingListener
    ) {
        i(TAG, "Synchronizing account %s", accountId)

        val notificationState = NotificationState()

        sendPendingMessages(accountId, listener)

        refreshFolderListIfStale(accountId)

        try {
            val localStore = localStoreProvider.getInstance(accountId)
            for (folder in localStore.getPersonalNamespaces(false)) {
                folder.open()

                if (!folder.isVisible()) {
                    // Never sync a folder that isn't displayed
                    continue
                }

                if (!folder.isSyncEnabled()) {
                    // Do not sync folders that are not enabled for sync.
                    continue
                }
                synchronizeFolder(accountId, folder, ignoreLastCheckedTime, notify, listener, notificationState)
            }
        } catch (e: MessagingException) {
            e(TAG, e, "Unable to synchronize account %s", accountId)
        } finally {
            putBackground(
                "clear notification flag for " + accountId, null,
                object : Runnable {
                    override fun run() {
                        v(TAG, "Clearing notification flag for %s", accountId)

                        clearFetchingMailNotification(accountId)
                    }
                }
            )
        }
    }

    private fun synchronizeFolder(
        accountId: AccountId, folder: LocalFolder, ignoreLastCheckedTime: Boolean,
        notify: Boolean, listener: MessagingListener, notificationState: NotificationState
    ) {
        putBackground(
            "sync" + folder.getServerId(), null,
            Runnable {
                synchronizeFolderInBackground(
                    accountId,
                    folder,
                    ignoreLastCheckedTime,
                    notify,
                    listener,
                    notificationState
                )
            }
        )
    }

    private fun synchronizeFolderInBackground(
        accountId: AccountId, folder: LocalFolder,
        ignoreLastCheckedTime: Boolean,
        notify: Boolean, listener: MessagingListener, notificationState: NotificationState
    ) {
        v(TAG, "Folder %s was last synced @ %tc", folder.getServerId(), folder.getLastChecked())

        if (!ignoreLastCheckedTime) {
            val lastCheckedTime = folder.getLastChecked()
            val now = System.currentTimeMillis()

            if (lastCheckedTime > now) {
                // The time this folder was last checked lies in the future. We better ignore this and sync now.
            } else {
                val account = accountManager.findById(accountId)
                if (account == null) {
                    return
                }
                val syncInterval = account.automaticCheckIntervalMinutes * 60L * 1000L
                val nextSyncTime = lastCheckedTime + syncInterval
                if (nextSyncTime > now) {
                    v(
                        TAG,
                        "Not syncing folder %s, previously synced @ %tc which would be too recent for the " +
                            "account sync interval",
                        folder.getServerId(), lastCheckedTime
                    )
                    return
                }
            }
        }

        try {
            showFetchingMailNotificationIfNecessary(accountId, folder)
            try {
                synchronizeMailboxSynchronous(accountId, folder.getDatabaseId(), notify, listener, notificationState)
            } finally {
                showEmptyFetchingMailNotificationIfNecessary(accountId)
            }
        } catch (e: Exception) {
            e(TAG, e, "Exception while processing folder %s:%s", accountId, folder.getServerId())
        }
    }

    private fun showFetchingMailNotificationIfNecessary(accountId: AccountId, folder: LocalFolder) {
        val account = accountManager.findById(accountId)
        if (account == null) {
            return
        }
        if (account.isNotifySync) {
            notificationController.showFetchingMailNotification(accountId, folder)
        }
    }

    private fun showEmptyFetchingMailNotificationIfNecessary(accountId: AccountId) {
        val account = accountManager.findById(accountId)
        if (account == null) {
            return
        }
        if (account.isNotifySync) {
            notificationController.showEmptyFetchingMailNotification(accountId)
        }
    }

    private fun clearFetchingMailNotification(accountId: AccountId) {
        notificationController.clearFetchingMailNotification(accountId)
    }

    fun compact(accountId: AccountId) {
        putBackground(
            "compact:" + accountId, null,
            Runnable {
                try {
                    val messageStore: MessageStore = messageStoreManager.getMessageStore(accountId)
                    messageStore.compact()
                } catch (e: Exception) {
                    e(TAG, e, "Failed to compact account %s", accountId)
                }
            }
        )
    }

    fun deleteAccount(accountId: AccountId) {
        notificationController.clearNewMailNotifications(accountId, false)
        memorizingMessagingListener.removeAccount(accountId)
    }

    /**
     * Save a draft message.
     */
    fun saveDraft(accountId: AccountId, message: Message, existingDraftId: Long?, plaintextSubject: String?): Long? {
        return draftOperations.saveDraft(accountId, message, existingDraftId, plaintextSubject)
    }

    fun getId(message: Message?): Long? {
        if (message is LocalMessage) {
            return message.getDatabaseId()
        } else {
            w(TAG, "MessagingController.getId() called without a LocalMessage")
            return null
        }
    }

    init {
        this.notificationSender = NotificationSenderCompat(notificationManager)
        this.notificationDismisser = NotificationDismisserCompat(notificationManager)
        this.outboxFolderManager = outboxFolderManager
        this.accountManager = accountManager

        controllerThread = Thread(
            object : Runnable {
                override fun run() {
                    runInBackground()
                }
            }
        )
        controllerThread.setName("MessagingController")
        controllerThread.start()
        addListener(memorizingMessagingListener)

        initializeControllerExtensions(controllerExtensions)

        draftOperations = DraftOperations(
            this, messageStoreManager, saveMessageDataCreator, localMessageUidPrefixProvider,
            accountManager
        )
        notificationOperations =
            NotificationOperations(notificationController, accountManager, outboxFolderManager)
        archiveOperations = ArchiveOperations(this, accountManager, featureFlagProvider)
    }

    private class Command : Comparable<Command?> {
        var runnable: Runnable? = null
        var listener: MessagingListener? = null
        var description: String? = null
        var isForegroundPriority: Boolean = false

        var sequence: Int = sequencing.getAndIncrement()

        override fun compareTo(other: Command): Int {
            if (other.isForegroundPriority && !isForegroundPriority) {
                return 1
            } else if (!other.isForegroundPriority && isForegroundPriority) {
                return -1
            } else {
                return (sequence - other.sequence)
            }
        }
    }

    fun clearNotifications(search: LocalMessageSearch) {
        put(
            "clearNotifications", null,
            Runnable {
                notificationOperations.clearNotifications(search)
            }
        )
    }

    fun cancelNotificationsForAccount(accountId: AccountId) {
        notificationController.clearNewMailNotifications(accountId, true)
    }

    fun cancelNotificationForMessage(accountId: AccountId, messageReference: MessageReference) {
        notificationController.removeNewMailNotification(accountId, messageReference)
    }

    @Deprecated("")
    fun clearCertificateErrorNotifications(accountId: AccountId, incoming: Boolean) {
        notificationController.clearCertificateErrorNotifications(accountId, incoming)
    }

    fun notifyUserIfCertificateProblem(accountId: AccountId, exception: Exception?, incoming: Boolean) {
        if (exception is CertificateValidationException) {
            notificationController.showCertificateErrorNotification(accountId, incoming)
        }
    }

    fun checkAuthenticationProblem(accountId: AccountId) {
        // checking incoming server configuration
        if (isAuthenticationProblem(accountId, true)) {
            handleAuthenticationFailure(accountId, true)
            return
        } else {
            clearAuthenticationErrorNotification(accountId, true, true)
        }

        // checking outgoing server configuration
        if (isAuthenticationProblem(accountId, false)) {
            handleAuthenticationFailure(accountId, false)
        } else {
            clearAuthenticationErrorNotification(accountId, false, true)
        }
    }

    private fun isAuthenticationProblem(accountId: AccountId, incoming: Boolean): Boolean {
        val account = accountManager.findById(accountId)
        if (account == null) {
            return false
        }

        val serverSettings = getServerSettings(account, incoming)

        return serverSettings.isMissingCredentials ||
            serverSettings.authenticationType == AuthType.XOAUTH2 && account.oAuthState == null
    }

    private fun getServerSettings(accountId: AccountId, incoming: Boolean): ServerSettings? {
        val account = accountManager.findById(accountId)
        if (account == null) {
            return null
        } else {
            return getServerSettings(account, incoming)
        }
    }

    private fun getServerSettings(account: LegacyAccount, incoming: Boolean): ServerSettings {
        return if (incoming) account.incomingServerSettings else account.outgoingServerSettings
    }

    private fun clearAuthenticationErrorNotification(
        accountId: AccountId, incoming: Boolean, clearOnlyForOAuthAccounts: Boolean
    ) {
        if (featureFlagProvider.provide(GeneratedFeatureFlagKey.DISPLAY_IN_APP_NOTIFICATIONS).isEnabled()) {
            var shouldClear = true
            val serverSettings = getServerSettings(accountId, incoming)
            if (clearOnlyForOAuthAccounts && serverSettings!!.authenticationType != AuthType.XOAUTH2) {
                shouldClear = false
            }

            if (shouldClear) {
                val notification = createAuthenticationErrorNotification(
                    accountId, incoming
                )
                notificationDismisser.dismiss(
                    notification!!,
                    NotificationDismisserCompat.OnResultListener { outcome: Outcome<out Success<out Notification?>?, out Failure<out Notification?>?>? ->
                        v(
                            TAG, "notificationDismisser outcome = " + outcome
                        )
                    }
                )
            }
        }
    }

    fun actOnMessagesGroupedByAccountAndFolder(messages: MutableList<MessageReference?>, actor: MessageActor) {
        val accountMap = groupMessagesByAccountAndFolder(messages)

        for (entry in accountMap.entries) {
            val accountId: AccountId = entry.key!!

            val folderMap: MutableMap<Long?, MutableList<MessageReference?>?> = entry.value!!
            for (folderEntry in folderMap.entries) {
                val folderId: Long = folderEntry.key!!
                val messageList: MutableList<MessageReference?> = folderEntry.value!!
                actOnMessageGroup(accountId, folderId, messageList, actor)
            }
        }
    }

    private fun groupMessagesByAccountAndFolder(
        messages: MutableList<MessageReference?>
    ): MutableMap<AccountId?, MutableMap<Long?, MutableList<MessageReference?>?>?> {
        val accountMap: MutableMap<AccountId?, MutableMap<Long?, MutableList<MessageReference?>?>?> =
            HashMap<AccountId?, MutableMap<Long?, MutableList<MessageReference?>?>?>()

        for (message in messages) {
            if (message == null) {
                continue
            }
            val accountId = message.accountId
            val folderId = message.folderId

            var folderMap = accountMap.get(accountId)
            if (folderMap == null) {
                folderMap = HashMap<Long?, MutableList<MessageReference?>?>()
                accountMap.put(accountId, folderMap)
            }
            var messageList = folderMap.get(folderId)
            if (messageList == null) {
                messageList = LinkedList<MessageReference?>()
                folderMap.put(folderId, messageList)
            }

            messageList.add(message)
        }
        return accountMap
    }

    private fun actOnMessageGroup(
        accountId: AccountId, folderId: Long, messageReferences: MutableList<MessageReference?>, actor: MessageActor
    ) {
        try {
            val messageFolder = localStoreProvider.getInstance(accountId).getFolder(folderId)
            val localMessages = messageFolder.getMessagesByReference(messageReferences)
            actor.act(accountId, messageFolder, localMessages)
        } catch (e: MessagingException) {
            e(TAG, e, "Error loading account?!")
        }
    }

    internal interface MessageActor {
        fun act(accountId: AccountId?, messageFolder: LocalFolder?, messages: MutableList<LocalMessage>?)
    }

    internal inner class ControllerSyncListener(
        private val accountId: AccountId,
        private val listener: MessagingListener?,
        private val suppressNotifications: Boolean,
        private val notificationState: NotificationState
    ) : SyncListener {
        private val localStore: LocalStore
        var syncFailed: Boolean = false

        init {
            this.localStore = getLocalStoreOrThrow(accountId)
        }

        override fun syncStarted(folderServerId: String) {
            val folderId = getFolderId(accountId, folderServerId)
            for (messagingListener in getListeners(listener)) {
                messagingListener.synchronizeMailboxStarted(accountId, folderId)
            }
        }

        override fun syncAuthenticationSuccess() {
            clearAuthenticationErrorNotification(accountId, true, false)
            notificationController.clearAuthenticationErrorNotification(accountId, true)
        }

        override fun syncHeadersStarted(folderServerId: String) {
            for (messagingListener in getListeners(listener)) {
                messagingListener.synchronizeMailboxHeadersStarted(accountId, folderServerId)
            }
        }

        override fun syncHeadersProgress(folderServerId: String, completed: Int, total: Int) {
            for (messagingListener in getListeners(listener)) {
                messagingListener.synchronizeMailboxHeadersProgress(accountId, folderServerId, completed, total)
            }
        }

        override fun syncHeadersFinished(
            folderServerId: String, totalMessagesInMailbox: Int,
            numNewMessages: Int
        ) {
            for (messagingListener in getListeners(listener)) {
                messagingListener.synchronizeMailboxHeadersFinished(
                    accountId, folderServerId, totalMessagesInMailbox,
                    numNewMessages
                )
            }
        }

        override fun syncProgress(folderServerId: String, completed: Int, total: Int) {
            val folderId = getFolderId(accountId, folderServerId)
            for (messagingListener in getListeners(listener)) {
                messagingListener.synchronizeMailboxProgress(accountId, folderId, completed, total)
            }
        }

        override fun syncNewMessage(
            folderServerId: String, messageServerId: String,
            isOldMessage: Boolean
        ) {
            // Send a notification of this message

            val message = loadMessage(folderServerId, messageServerId)
            val localFolder = message.getFolder()
            if (!suppressNotifications &&
                notificationStrategy.shouldNotifyForMessage(accountId, localFolder, message, isOldMessage)
            ) {
                // Notify with the localMessage so that we don't have to recalculate the content preview.
                val silent = notificationState.wasNotified
                notificationController.addNewMailNotification(accountId, message, silent)
                notificationState.wasNotified = true
            }

            if (!message.isSet(Flag.SEEN)) {
                for (messagingListener in getListeners(listener)) {
                    messagingListener.synchronizeMailboxNewMessage(accountId, folderServerId, message)
                }
            }
        }

        override fun syncRemovedMessage(folderServerId: String, messageServerId: String) {
            for (messagingListener in getListeners(listener)) {
                messagingListener.synchronizeMailboxRemovedMessage(accountId, folderServerId, messageServerId)
            }

            val folderId = getFolderId(accountId, folderServerId)
            val messageReference = MessageReference(accountId, folderId, messageServerId)
            notificationController.removeNewMailNotification(accountId, messageReference)
        }

        override fun syncFlagChanged(folderServerId: String, messageServerId: String) {
            var shouldBeNotifiedOf = false
            val message = loadMessage(folderServerId, messageServerId)
            if (message.isSet(Flag.DELETED) || isMessageSuppressed(message)) {
                syncRemovedMessage(folderServerId, message.getUid())
            } else {
                val localFolder = message.getFolder()
                if (notificationStrategy.shouldNotifyForMessage(accountId, localFolder, message, false)) {
                    shouldBeNotifiedOf = true
                }
            }

            // we're only interested in messages that need removing
            if (!shouldBeNotifiedOf) {
                val messageReference = message.makeMessageReference()
                notificationController.removeNewMailNotification(accountId, messageReference)
            }
        }

        override fun syncFinished(folderServerId: String) {
            val folderId = getFolderId(accountId, folderServerId)
            for (messagingListener in getListeners(listener)) {
                messagingListener.synchronizeMailboxFinished(accountId, folderId)
            }
        }

        override fun syncFailed(folderServerId: String, message: String, exception: Exception?) {
            syncFailed = true

            if (exception is AuthenticationFailedException) {
                handleAuthenticationFailure(accountId, true)
            } else {
                notifyUserIfCertificateProblem(accountId, exception, true)
            }

            val folderId = getFolderId(accountId, folderServerId)
            for (messagingListener in getListeners(listener)) {
                messagingListener.synchronizeMailboxFailed(accountId, folderId, message)
            }
        }

        override fun folderStatusChanged(folderServerId: String) {
            val folderId = getFolderId(accountId, folderServerId)
            for (messagingListener in getListeners(listener)) {
                messagingListener.folderStatusChanged(accountId, folderId)
            }
        }

        private fun loadMessage(folderServerId: String?, messageServerId: String?): LocalMessage {
            try {
                val localFolder = localStore.getFolder(folderServerId)
                localFolder.open()
                return localFolder.getMessage(messageServerId)
            } catch (e: MessagingException) {
                throw RuntimeException("Couldn't load message (" + folderServerId + ":" + messageServerId + ")", e)
            }
        }
    }

    internal enum class MoveOrCopyFlavor {
        MOVE, COPY, MOVE_AND_MARK_AS_READ
    }

    companion object {
        val SYNC_FLAGS: MutableSet<Flag?> = EnumSet.of<Flag?>(Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED, Flag.FORWARDED)

        private val FOLDER_LIST_STALENESS_THRESHOLD = 30 * 60 * 1000L

        private const val TAG = "MessagingController"

        @JvmStatic
        fun getInstance(context: Context?): MessagingController {
            return get<MessagingController>(MessagingController::class.java)
        }

        private fun getUidsFromMessages(messages: MutableList<LocalMessage?>): MutableList<String> {
            val uids: MutableList<String> = ArrayList<String>(messages.size)
            for (i in messages.indices) {
                uids.add(messages.get(i)!!.getUid())
            }
            return uids
        }

        private val sequencing = AtomicInteger(0)
    }
}
