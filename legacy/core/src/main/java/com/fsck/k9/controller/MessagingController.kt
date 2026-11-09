package com.fsck.k9.controller

import android.content.Context
import android.os.Process
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import app.k9mail.legacy.di.DI
import app.k9mail.legacy.mailstore.FolderDetailsAccessor
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
import com.fsck.k9.mailstore.MessageListCache
import com.fsck.k9.mailstore.OutboxState
import com.fsck.k9.mailstore.SaveMessageDataCreator
import com.fsck.k9.mailstore.SendState
import com.fsck.k9.notification.NotificationController
import com.fsck.k9.notification.NotificationStrategy
import java.util.Date
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import net.thunderbird.components.core.logging.Logger
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
import net.thunderbird.feature.notification.api.content.AuthenticationErrorNotification
import net.thunderbird.feature.search.legacy.LocalMessageSearch

/**
 * Starts a long-running (application) Thread that will run through commands that require remote mailbox access. This
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
    private val localDeleteOperationDecider: LocalDeleteOperationDecider,
    private val localMessageUidPrefixProvider: LocalMessageUidPrefixProvider,
    controllerExtensions: List<ControllerExtension>,
    private val featureFlagProvider: FeatureFlagProvider,
    private val syncDebugLogger: Logger,
    private val logger: Logger,
    private val notificationManager: NotificationManager,
    outboxFolderManager: OutboxFolderManager,
    accountManager: LegacyAccountManager,
) : MessagingControllerRegistry, MessagingControllerMailChecker {
    private val controllerThread: Thread

    private val queuedCommands: BlockingQueue<Command> = PriorityBlockingQueue()
    val listeners: MutableSet<MessagingListener> = CopyOnWriteArraySet()
    private val threadPool: ExecutorService = Executors.newCachedThreadPool()
    private val memorizingMessagingListener = MemorizingMessagingListener()
    private val draftOperations: DraftOperations
    private val notificationOperations: NotificationOperations
    private val archiveOperations: ArchiveOperations
    private val outboxFolderManager: OutboxFolderManager
    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val accountManager: LegacyAccountManager

    @Volatile
    private var stopped = false

    private fun initializeControllerExtensions(controllerExtensions: List<ControllerExtension>) {
        if (controllerExtensions.isEmpty()) {
            return
        }

        val internals = object : ControllerInternals {
            override fun put(
                description: String, listener: MessagingListener?,
                runnable: Runnable,
            ) {
                this@MessagingController.put(description, listener, runnable)
            }

            override fun putBackground(
                description: String, listener: MessagingListener?,
                runnable: Runnable,
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
        controllerThread.join(STOP_TIMEOUT_MILLIS)
    }

    private fun runInBackground() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        while (!stopped) {
            var commandDescription: String? = null
            try {
                val command = queuedCommands.take()

                if (command != null) {
                    commandDescription = command.description

                    logger.info(TAG) {
                        "Running command '${command.description}', seq = ${command.sequence} " +
                            "(${if (command.isForegroundPriority) "foreground" else "background"} priority)"
                    }

                    command.runnable!!.run()

                    logger.info(TAG) { " Command '${command.description}' completed" }
                }
            } catch (e: Exception) {
                logger.error(TAG, e) { "Error running command '$commandDescription'" }
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
        queue: BlockingQueue<Command>, description: String?, listener: MessagingListener?,
        runnable: Runnable, isForeground: Boolean,
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
                SystemClock.sleep(PUT_COMMAND_RETRY_DELAY_MILLIS)
                e = ie
            }
        }
        throw IllegalStateException("Couldn't enqueue command", e)
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
            throw IllegalStateException("Couldn't get LocalStore for account $accountId", e)
        }
    }

    private fun getFolderServerId(accountId: AccountId, folderId: Long): String {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        return checkNotNull(messageStore.getFolderServerId(folderId)) { "Folder not found (ID: $folderId)" }
    }

    private fun getFolderId(accountId: AccountId, folderServerId: String): Long {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        val folderId = messageStore.getFolderId(folderServerId)
        checkNotNull(folderId) { "Folder not found (server ID: $folderServerId)" }
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

        val listenersWithExtra = HashSet(listeners)
        listenersWithExtra.add(listener)
        return listenersWithExtra
    }

    fun suppressMessages(accountId: AccountId, messages: List<LocalMessage>) {
        val cache = MessageListCache.getCache(accountId)
        cache.hideMessages(messages)
    }

    private fun unsuppressMessages(accountId: AccountId, messages: List<LocalMessage>) {
        val cache = MessageListCache.getCache(accountId)
        cache.unhideMessages(messages)
    }

    fun isMessageSuppressed(message: LocalMessage): Boolean {
        val messageId = message.databaseId
        val folderId = message.folder.databaseId

        val cache = MessageListCache.getCache(message.folder.accountId)
        return cache.isMessageHidden(messageId, folderId)
    }

    private fun setFlagInCache(
        accountId: AccountId, messageIds: List<Long>,
        flag: Flag, newState: Boolean,
    ) {
        val cache = MessageListCache.getCache(accountId)
        cache.setFlagForMessages(messageIds, flag, newState)
    }

    private fun removeFlagFromCache(
        accountId: AccountId, messageIds: List<Long>,
        flag: Flag,
    ) {
        val cache = MessageListCache.getCache(accountId)
        cache.removeFlagForMessages(messageIds, flag)
    }

    private fun setFlagForThreadsInCache(
        accountId: AccountId, threadRootIds: List<Long>,
        flag: Flag, newState: Boolean,
    ) {
        val cache = MessageListCache.getCache(accountId)
        cache.setValueForThreads(threadRootIds, flag, newState)
    }

    private fun removeFlagForThreadsFromCache(
        accountId: AccountId, messageIds: List<Long>,
        flag: Flag,
    ) {
        val cache = MessageListCache.getCache(accountId)
        cache.removeFlagForThreads(messageIds, flag)
    }

    fun refreshFolderList(accountId: AccountId) {
        put("refreshFolderList", null) { refreshFolderListSynchronous(accountId) }
    }

    fun refreshFolderListSynchronous(accountId: AccountId) {
        try {
            if (isAuthenticationProblem(accountId, true)) {
                logger.debug(TAG) { "Authentication will fail. Skip refreshing the folder list." }
                handleAuthenticationFailure(accountId, true)
                return
            }

            val backend = getBackend(accountId)
            val folderPathDelimiter = backend.refreshFolderList()

            val now = System.currentTimeMillis()
            logger.debug(TAG) { "Folder list successfully refreshed @ ${Date(now)}" }

            // Refreshing the folder list may update the account (e.g. special folders), so only change what is ours
            accountManager.updateSync(accountId) { currentAccount: LegacyAccount ->
                var updatedAccount = currentAccount
                if (!folderPathDelimiter.isNullOrEmpty() &&
                    folderPathDelimiter != currentAccount.folderPathDelimiter
                ) {
                    updatedAccount = updatedAccount.updateFolderDelimiter(folderPathDelimiter)
                }
                updatedAccount.updateLastFolderListRefreshTime(now)
            }
        } catch (e: Exception) {
            logger.error(TAG, e) { "Could not refresh folder list for account $accountId" }
            handleException(accountId, e)
        }
    }

    fun searchRemoteMessages(
        accountId: AccountId, folderId: Long, query: String?, requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?, listener: MessagingListener?,
    ): Future<*>? {
        logger.info(TAG) { "searchRemoteMessages (acct = $accountId, folderId = $folderId, query = $query)" }

        return threadPool.submit {
            searchRemoteMessagesSynchronous(
                accountId,
                folderId,
                query,
                requiredFlags,
                forbiddenFlags,
                listener,
            )
        }
    }

    @VisibleForTesting
    fun searchRemoteMessagesSynchronous(
        accountId: AccountId, folderId: Long, query: String?, requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?, listener: MessagingListener?,
    ) {
        val account = accountManager.findById(accountId)

        listener?.remoteSearchStarted(folderId)

        var extraResults: List<String> = ArrayList()
        try {
            val localStore = localStoreProvider.getInstance(accountId)

            val localFolder = localStore.getFolder(folderId)
            if (!localFolder.exists()) {
                throw MessagingException("Folder not found")
            }

            localFolder.open()
            val folderServerId = localFolder.serverId

            val backend = getBackend(accountId)

            val performFullTextSearch = account!!.isRemoteSearchFullText
            var messageServerIds: List<String> = backend.search(
                folderServerId, query, requiredFlags, forbiddenFlags,
                performFullTextSearch,
            )

            logger.info(TAG) { "Remote search got ${messageServerIds.size} results" }

            // There's no need to fetch messages already completely downloaded
            messageServerIds = localFolder.extractNewMessages(messageServerIds)

            listener?.remoteSearchServerQueryComplete(
                folderId, messageServerIds.size,
                account.remoteSearchNumResults,
            )

            val resultLimit = account.remoteSearchNumResults
            if (resultLimit > 0 && messageServerIds.size > resultLimit) {
                extraResults = messageServerIds.subList(resultLimit, messageServerIds.size)
                messageServerIds = messageServerIds.subList(0, resultLimit)
            }

            loadSearchResultsSynchronous(accountId, messageServerIds, localFolder)
        } catch (e: Exception) {
            if (Thread.currentThread().isInterrupted) {
                logger.info(TAG, e) { "Caught exception on aborted remote search; safe to ignore." }
            } else {
                logger.error(TAG, e) { "Could not complete remote search" }
                listener?.remoteSearchFailed(null, e.message)
                logger.error(TAG, e) { "Remote search failed for account $accountId, folder $folderId" }
            }
        } finally {
            listener?.remoteSearchFinished(folderId, 0, account!!.remoteSearchNumResults, extraResults)
        }
    }

    fun loadSearchResults(
        accountId: AccountId, folderId: Long, messageServerIds: List<String>,
        listener: MessagingListener?,
    ) {
        threadPool.execute {
            listener?.enableProgressIndicator(true)
            try {
                val localStore = localStoreProvider.getInstance(accountId)
                val localFolder = localStore.getFolder(folderId)
                if (!localFolder.exists()) {
                    throw MessagingException("Folder not found")
                }

                localFolder.open()

                loadSearchResultsSynchronous(accountId, messageServerIds, localFolder)
            } catch (e: MessagingException) {
                logger.error(TAG, e) { "Exception in loadSearchResults" }
            } finally {
                listener?.enableProgressIndicator(false)
            }
        }
    }

    @Throws(MessagingException::class)
    private fun loadSearchResultsSynchronous(
        accountId: AccountId, messageServerIds: List<String>,
        localFolder: LocalFolder,
    ) {
        val backend = getBackend(accountId)
        val folderServerId = localFolder.serverId

        for (messageServerId in messageServerIds) {
            val localMessage = localFolder.getMessage(messageServerId)

            if (localMessage == null) {
                backend.downloadMessageStructure(folderServerId, messageServerId)
            }
        }
    }

    fun loadMoreMessages(accountId: AccountId, folderId: Long) {
        putBackground("loadMoreMessages", null) { loadMoreMessagesSynchronous(accountId, folderId) }
    }

    fun loadMoreMessagesSynchronous(accountId: AccountId, folderId: Long) {
        val account = accountManager.findById(accountId) ?: return

        val messageStore = messageStoreManager.getMessageStore(accountId)
        val visibleLimit = messageStore.getFolder(folderId, FolderDetailsAccessor::visibleLimit)
        if (visibleLimit == null) {
            logger.verbose(TAG) { "loadMoreMessages($accountId, $folderId): Folder not found" }
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
        listener: MessagingListener?,
    ) {
        putBackground(
            "synchronizeMailbox",
            listener,
        ) { synchronizeMailboxSynchronous(accountId, folderId, notify, listener, NotificationState()) }
    }

    fun synchronizeMailboxBlocking(accountId: AccountId, folderServerId: String) {
        val folderId = getFolderId(accountId, folderServerId)

        val latch = CountDownLatch(1)
        putBackground("synchronizeMailbox", null) {
            try {
                synchronizeMailboxSynchronous(accountId, folderId, true, null, NotificationState())
            } finally {
                latch.countDown()
            }
        }

        try {
            latch.await()
        } catch (e: Exception) {
            logger.error(TAG, e) { "Interrupted while awaiting latch release" }
        }
    }

    private fun synchronizeMailboxSynchronous(
        accountId: AccountId, folderId: Long, notify: Boolean,
        listener: MessagingListener?, notificationState: NotificationState,
    ) {
        refreshFolderListIfStale(accountId)

        val backend = getBackend(accountId)
        syncFolder(accountId, folderId, notify, listener, backend, notificationState)
    }

    private fun refreshFolderListIfStale(accountId: AccountId) {
        val account = accountManager.findById(accountId) ?: return

        val lastFolderListRefresh = account.lastFolderListRefreshTime
        val now = System.currentTimeMillis()

        if (lastFolderListRefresh > now || lastFolderListRefresh + FOLDER_LIST_STALENESS_THRESHOLD <= now) {
            logger.debug(TAG) { "Last folder list refresh @ ${Date(lastFolderListRefresh)}. Refreshing now…" }
            refreshFolderListSynchronous(accountId)
        } else {
            logger.debug(TAG) { "Last folder list refresh @ ${Date(lastFolderListRefresh)}. Not refreshing now." }
        }
    }

    private fun syncFolder(
        accountId: AccountId, folderId: Long, notify: Boolean, listener: MessagingListener?,
        backend: Backend,
        notificationState: NotificationState,
    ) {
        if (isAuthenticationProblem(accountId, true)) {
            logger.debug(TAG) { "Authentication will fail. Skip synchronizing folder $folderId." }
            handleAuthenticationFailure(accountId, true)
            return
        }

        var commandException: Exception? = null
        try {
            processPendingCommandsSynchronous(accountId)
        } catch (e: Exception) {
            logger.error(TAG, e) { "Failure processing command, but allow message sync attempt" }
            commandException = e
        }

        val localFolder = loadSyncableLocalFolder(accountId, folderId) ?: return

        val suppressNotifications: Boolean
        if (notify) {
            val messageStore = messageStoreManager.getMessageStore(accountId)
            val lastChecked = messageStore.getFolder(folderId, FolderDetailsAccessor::lastChecked)
            suppressNotifications = lastChecked == null
        } else {
            suppressNotifications = true
        }

        val folderServerId = localFolder.serverId
        val syncConfig = createSyncConfig(accountId)
        val syncListener =
            ControllerSyncListener(accountId, listener, suppressNotifications, notificationState)

        backend.sync(folderServerId, syncConfig, syncListener)

        if (commandException != null && !syncListener.syncFailed) {
            val rootMessage = commandException.rootCauseMessage
            syncDebugLogger.error("MessagingException", null) { rootMessage.orEmpty() }
            logger.error(TAG) { "Root cause failure in $accountId:$folderServerId was '$rootMessage'" }
            updateFolderStatus(accountId, folderId, rootMessage)
            listener?.synchronizeMailboxFailed(accountId, folderId, rootMessage)
        }
    }

    /**
     * Returns `null` when the local folder couldn't be loaded or is a local-only folder, which can't be synced.
     */
    private fun loadSyncableLocalFolder(accountId: AccountId, folderId: Long): LocalFolder? {
        val localFolder: LocalFolder
        try {
            val localStore = localStoreProvider.getInstance(accountId)
            localFolder = localStore.getFolder(folderId)
            localFolder.open()
        } catch (e: MessagingException) {
            syncDebugLogger.error("MessagingException", null) { e.message.orEmpty() }
            logger.error(TAG, e) { "syncFolder: Couldn't load local folder $folderId" }
            return null
        }

        // We can't sync local folders
        return localFolder.takeUnless { it.isLocalOnly }
    }

    private fun createSyncConfig(accountId: AccountId): SyncConfig {
        val account = accountManager.findById(accountId)
        requireNotNull(account) { "Account not found for $accountId" }
        return SyncConfig(
            account.expungePolicy.toBackendExpungePolicy(),
            account.getEarliestPollDate(),
            account.isSyncRemoteDeletions,
            account.maximumAutoDownloadMessageSize,
            AccountDefaultsProvider.DEFAULT_VISIBLE_LIMIT,
            SYNC_FLAGS,
        )
    }

    private fun updateFolderStatus(accountId: AccountId, folderId: Long, status: String?) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.setStatus(folderId, status)
    }

    fun handleAuthenticationFailure(accountId: AccountId, incoming: Boolean) {
        val account = accountManager.findById(accountId)
        if (account != null && account.shouldMigrateToOAuth) {
            migrateAccountToOAuth(accountId)
        }

        if (featureFlagProvider.provide(GeneratedFeatureFlagKey.DISPLAY_IN_APP_NOTIFICATIONS).isEnabled()) {
            logger.debug(TAG) { "handleAuthenticationFailure: sending in-app notification" }
            val notification =
                createAuthenticationErrorNotification(accountId, incoming)

            if (notification != null) {
                notificationManager.send(notification)
                    .onEach { outcome -> logger.verbose(TAG) { "notificationSender outcome = $outcome" } }
                    .launchIn(notificationScope)
            }
        }

        if (featureFlagProvider
                .provide(GeneratedFeatureFlagKey.USE_NOTIFICATION_SENDER_FOR_SYSTEM_NOTIFICATIONS)
                .isDisabled()
        ) {
            logger.debug(TAG) {
                "handleAuthenticationFailure: sending system notification via old notification controller"
            }
            notificationController.showAuthenticationErrorNotification(accountId, incoming)
        }
    }

    private fun createAuthenticationErrorNotification(
        accountId: AccountId,
        incoming: Boolean,
    ): AuthenticationErrorNotification? {
        val account = accountManager.findById(accountId) ?: return null

        return runBlocking {
            AuthenticationErrorNotification(
                accountId = account.id,
                accountDisplayName = account.profile.name,
                accountNumber = account.accountNumber,
                isIncomingServerError = incoming,
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
            throw IllegalStateException("Unable to enqueue pending command", e)
        }
    }

    fun processPendingCommands(accountId: AccountId) {
        putBackground("processPendingCommands", null) {
            try {
                processPendingCommandsSynchronous(accountId)
            } catch (me: MessagingException) {
                /*
                 * Ignore any exceptions from the commands. Commands will be processed
                 * on the next round.
                 */
                logger.error(TAG, me) { "processPendingCommands" }
            }
        }
    }

    @Throws(MessagingException::class)
    fun processPendingCommandsSynchronous(accountId: AccountId) {
        val localStore = localStoreProvider.getInstance(accountId)
        val commands = localStore.pendingCommands

        var processingCommand: PendingCommand? = null
        try {
            for (command in commands) {
                processingCommand = command
                processPendingCommand(localStore, command, accountId)

                // TODO: When removing a pending command due to an error the local changes should be reverted. Pending
                //  commands that depend on this command should be canceled and local changes be reverted. In most cases
                //  the user should be notified about the failure as well.
            }
        } catch (me: MessagingException) {
            notifyUserIfCertificateProblem(accountId, me, true)
            logger.error(TAG, me) { "Could not process command '$processingCommand'" }
            throw me
        }
    }

    @Throws(MessagingException::class)
    private fun processPendingCommand(localStore: LocalStore, command: PendingCommand, accountId: AccountId) {
        val commandName = command.commandName
        logger.debug(TAG) { "Processing pending command '$commandName'" }

        /*
         * We specifically do not catch any exceptions here. If a command fails it is
         * most likely due to a server or IO error, and it must be retried before any
         * other command processes. This maintains the order of the commands.
         */
        try {
            command.execute(this, accountId)

            localStore.removePendingCommand(command)

            logger.debug(TAG) { "Done processing pending command '$commandName'" }
        } catch (me: MessagingException) {
            if (me.isPermanentFailure) {
                logger.error(TAG, me) {
                    "Failure of command '$commandName' was permanent, removing command from queue"
                }
                localStore.removePendingCommand(command)
            } else {
                throw me
            }
        } catch (e: Exception) {
            logger.error(TAG, e) {
                "Unexpected exception with command '$commandName', removing command from queue"
            }
            localStore.removePendingCommand(command)

            if (BuildConfig.DEBUG) {
                throw AssertionError("Unexpected exception while processing pending command", e)
            }
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

        val folderServerId = localFolder.serverId
        val uid = command.uid

        val localMessage = localFolder.getMessage(uid)
        if (localMessage == null || !localMessage.uid.startsWith(localMessageUidPrefixProvider.get())) {
            // TODO: A message without a local UID should never happen. Throw in debug builds.
            return
        }

        val backend = getBackend(accountId)

        if (localMessage.isSet(Flag.X_REMOTE_COPY_STARTED)) {
            logger.warn(TAG) {
                "Local message with uid ${localMessage.uid} has flag ${Flag.X_REMOTE_COPY_STARTED}  already set, " +
                    "checking for remote message with same message id"
            }

            val messageServerId = backend.findByMessageId(folderServerId, localMessage.messageId)
            if (messageServerId != null) {
                logger.warn(TAG) {
                    "Local message has flag ${Flag.X_REMOTE_COPY_STARTED} already set, and there is a remote message " +
                        "with uid $messageServerId, assuming message was already copied and aborting this copy"
                }

                val oldUid = localMessage.uid
                localMessage.uid = messageServerId
                localFolder.changeUid(localMessage)

                for (l in listeners) {
                    l.messageUidChanged(accountId, folderId, oldUid, localMessage.uid)
                }

                return
            } else {
                logger.warn(TAG) { "No remote message with message-id found, proceeding with append" }
            }
        }

        /*
         * If the message does not exist remotely we just upload it and then
         * update our local copy with the new uid.
         */
        val fp = FetchProfile()
        fp.add(FetchProfile.Item.BODY)
        localFolder.fetch(listOf(localMessage), fp, null)
        val oldUid = localMessage.uid
        localMessage.setFlag(Flag.X_REMOTE_COPY_STARTED, true)

        val messageServerId = backend.uploadMessage(folderServerId, localMessage)

        if (messageServerId == null) {
            // We didn't get the server UID of the uploaded message. Remove the local message now. The uploaded
            // version will be downloaded during the next sync.
            localFolder.destroyMessages(listOf(localMessage))
        } else {
            localMessage.uid = messageServerId
            localFolder.changeUid(localMessage)

            for (l in listeners) {
                l.messageUidChanged(accountId, folderId, oldUid, localMessage.uid)
            }
        }
    }

    fun processPendingReplace(pendingReplace: PendingReplace, accountId: AccountId) {
        draftOperations.processPendingReplace(pendingReplace, accountId)
    }

    private fun queueMoveOrCopy(
        accountId: AccountId, srcFolderId: Long, destFolderId: Long,
        operation: MoveOrCopyFlavor,
        uidMap: Map<String?, String?>?,
    ) {
        val command: PendingCommand = when (operation) {
            MoveOrCopyFlavor.MOVE -> PendingMoveOrCopy.create(srcFolderId, destFolderId, false, uidMap)
            MoveOrCopyFlavor.COPY -> PendingMoveOrCopy.create(srcFolderId, destFolderId, true, uidMap)
            MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ -> PendingMoveAndMarkAsRead.create(srcFolderId, destFolderId, uidMap)
        }
        queuePendingCommand(accountId, command)
    }

    @Throws(MessagingException::class)
    fun processPendingMoveOrCopy(command: PendingMoveOrCopy, accountId: AccountId) {
        val srcFolder = command.srcFolderId
        val destFolder = command.destFolderId
        val operation = if (command.isCopy) MoveOrCopyFlavor.COPY else MoveOrCopyFlavor.MOVE

        val newUidMap = command.newUidMap
        val uids = if (newUidMap != null) ArrayList(newUidMap.keys) else command.uids

        processPendingMoveOrCopy(accountId, srcFolder, destFolder, uids, operation, newUidMap!!)
    }

    @Throws(MessagingException::class)
    fun processPendingMoveAndRead(command: PendingMoveAndMarkAsRead, accountId: AccountId) {
        val srcFolder = command.srcFolderId
        val destFolder = command.destFolderId
        val newUidMap = command.newUidMap
        val uids = ArrayList(newUidMap.keys)

        processPendingMoveOrCopy(
            accountId, srcFolder, destFolder, uids,
            MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ, newUidMap,
        )
    }

    @VisibleForTesting
    @Throws(MessagingException::class)
    internal fun processPendingMoveOrCopy(
        accountId: AccountId, srcFolderId: Long, destFolderId: Long, uids: List<String>,
        operation: MoveOrCopyFlavor, newUidMap: Map<String, String>,
    ) {
        requireNotNull(newUidMap)

        val localStore = localStoreProvider.getInstance(accountId)

        val localSourceFolder = localStore.getFolder(srcFolderId)
        localSourceFolder.open()
        val srcFolderServerId = localSourceFolder.serverId

        val localDestFolder = localStore.getFolder(destFolderId)
        localDestFolder.open()
        val destFolderServerId = localDestFolder.serverId

        val backend = getBackend(accountId)

        var remoteUidMap: Map<String, String>?
        remoteUidMap = when (operation) {
            MoveOrCopyFlavor.COPY -> backend.copyMessages(srcFolderServerId, destFolderServerId, uids)
            MoveOrCopyFlavor.MOVE -> backend.moveMessages(srcFolderServerId, destFolderServerId, uids)
            MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ -> backend.moveMessagesAndMarkAsRead(
                srcFolderServerId,
                destFolderServerId,
                uids,
            )
        }

        if (operation != MoveOrCopyFlavor.COPY) {
            destroyPlaceholderMessages(localSourceFolder, uids)
        }

        // TODO: Change Backend interface to ensure we never receive null for remoteUidMap
        if (remoteUidMap == null) {
            remoteUidMap = emptyMap()
        }

        // Update local messages (that currently have local UIDs) with new server IDs
        for (uid in uids) {
            val localUid = newUidMap[uid]
            val newUid = remoteUidMap[uid]

            // Skip if the local message no longer exists
            val localMessage = localDestFolder.getMessage(localUid) ?: continue

            if (newUid != null) {
                // Update local message with new server ID
                localMessage.uid = newUid
                localDestFolder.changeUid(localMessage)
                for (l in listeners) {
                    l.messageUidChanged(accountId, destFolderId, localUid, newUid)
                }
            } else {
                // New server ID wasn't provided. Remove local message.
                localMessage.destroy()
            }
        }
    }

    @Throws(MessagingException::class)
    fun destroyPlaceholderMessages(localFolder: LocalFolder, uids: List<String>) {
        for (uid in uids) {
            val placeholderMessage = localFolder.getMessage(uid) ?: continue

            if (placeholderMessage.isSet(Flag.DELETED)) {
                placeholderMessage.destroy()
            } else {
                logger.warn(TAG) {
                    "Expected local message $uid in folder ${localFolder.serverId} to be a placeholder, but DELETE " +
                        "flag wasn't set"
                }

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
        flag: Flag,
        uids: List<String>?,
    ) {
        val command = PendingSetFlag.create(folderId, newState, flag, uids)
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

    private fun queueDelete(accountId: AccountId, folderId: Long, uids: List<String>?) {
        val command = PendingDelete.create(folderId, uids)
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
        val command = PendingExpunge.create(folderId)
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
        val folderServerId = localFolder.serverId

        logger.info(TAG) { "Marking all messages in $accountId:$folderServerId as read" }

        // TODO: Make this one database UPDATE operation
        val messages = localFolder.getMessages(false)
        for (message in messages) {
            if (!message.isSet(Flag.SEEN)) {
                message.setFlag(Flag.SEEN, true)
            }
        }

        for (l in listeners) {
            l.folderStatusChanged(accountId, folderId)
        }

        val backend = getBackend(accountId)
        if (backend.supportsFlags) {
            backend.markAllAsRead(folderServerId)
        }
    }

    fun markAllMessagesRead(accountId: AccountId, folderId: Long) {
        val command = PendingMarkAllAsRead.create(folderId)
        queuePendingCommand(accountId, command)
        processPendingCommands(accountId)
    }

    fun setFlag(
        accountId: AccountId, messageIds: List<Long>, flag: Flag,
        newState: Boolean,
    ) {
        setFlagInCache(accountId, messageIds, flag, newState)

        putBackground("setFlag", null) { setFlagSynchronous(accountId, messageIds, flag, newState, false) }
    }

    fun setFlagForThreads(
        accountId: AccountId, threadRootIds: List<Long>,
        flag: Flag, newState: Boolean,
    ) {
        setFlagForThreadsInCache(accountId, threadRootIds, flag, newState)

        putBackground("setFlagForThreads", null) { setFlagSynchronous(accountId, threadRootIds, flag, newState, true) }
    }

    private fun setFlagSynchronous(
        accountId: AccountId, ids: List<Long>,
        flag: Flag, newState: Boolean, threadedList: Boolean,
    ) {
        val localStore: LocalStore?
        try {
            localStore = localStoreProvider.getInstance(accountId)
        } catch (e: MessagingException) {
            logger.error(TAG, e) { "Couldn't get LocalStore instance" }
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
            logger.error(TAG, e) { "Couldn't set flags in local database" }
        }

        // Read folder ID and UID of messages from the database
        val folderMap: Map<Long, List<String>>
        try {
            folderMap = localStore.getFolderIdsAndUids(ids, threadedList)
        } catch (e: MessagingException) {
            logger.error(TAG, e) { "Couldn't get folder name and UID of messages" }
            return
        }

        val accountSupportsFlags = supportsFlags(accountId)

        // Loop over all folders
        for (entry in folderMap.entries) {
            val folderId = entry.key
            val uids = entry.value

            // Notify listeners of changed folder status
            for (l in listeners) {
                l.folderStatusChanged(accountId, folderId)
            }

            if (flag == Flag.SEEN && newState) {
                cancelNotificationsForMessages(accountId, folderId, uids)
            }

            if (accountSupportsFlags) {
                val localFolder = localStore.getFolder(folderId)
                try {
                    localFolder.open()
                    if (!localFolder.isLocalOnly) {
                        // Send flag change to server
                        queueSetFlag(accountId, folderId, newState, flag, uids)
                        processPendingCommands(accountId)
                    }
                } catch (e: MessagingException) {
                    logger.error(TAG, e) { "Couldn't open folder. Account: $accountId, folder ID: $folderId" }
                }
            }
        }
    }

    private fun cancelNotificationsForMessages(accountId: AccountId, folderId: Long, uids: List<String>) {
        for (uid in uids) {
            val messageReference = MessageReference(accountId, folderId, uid)
            notificationController.removeNewMailNotification(accountId, messageReference)
        }
    }

    /**
     * Set or remove a flag for a set of messages in a specific folder.
     *
     * The [Message] objects passed in are updated to reflect the new flag state.
     */
    fun setFlag(
        accountId: AccountId, folderId: Long, messages: List<LocalMessage>, flag: Flag,
        newState: Boolean,
    ) {
        // TODO: Put this into the background, but right now some callers depend on the message
        //       objects being modified right after this method returns.
        try {
            val localStore = localStoreProvider.getInstance(accountId)
            val localFolder = localStore.getFolder(folderId)
            localFolder.open()

            // Update the messages in the local store
            localFolder.setFlags(messages, setOf(flag), newState)

            for (l in listeners) {
                l.folderStatusChanged(accountId, folderId)
            }

            // Handle the remote side
            if (supportsFlags(accountId) && !localFolder.isLocalOnly) {
                val uids = getUidsFromMessages(messages)
                queueSetFlag(accountId, folderId, newState, flag, uids)
                processPendingCommands(accountId)
            }
        } catch (me: MessagingException) {
            throw IllegalStateException("Error setting flag", me)
        }
    }

    /**
     * Set or remove a flag for a message referenced by message UID.
     */
    fun setFlag(accountId: AccountId, folderId: Long, uid: String?, flag: Flag, newState: Boolean) {
        try {
            val localStore = localStoreProvider.getInstance(accountId)
            val localFolder = localStore.getFolder(folderId)
            localFolder.open()

            val message = localFolder.getMessage(uid)
            if (message != null) {
                setFlag(accountId, folderId, listOf(message), flag, newState)
            }
        } catch (me: MessagingException) {
            throw IllegalStateException("Error setting flag", me)
        }
    }

    fun loadMessageRemotePartial(
        accountId: AccountId, folderId: Long, uid: String,
        listener: MessagingListener?,
    ) {
        put(
            "loadMessageRemotePartial",
            listener,
        ) { loadMessageRemoteSynchronous(accountId, folderId, uid, listener, true) }
    }

    //TODO: Fix the callback mess. See GH-782
    fun loadMessageRemote(accountId: AccountId, folderId: Long, uid: String, listener: MessagingListener?) {
        put(
            "loadMessageRemote",
            listener,
        ) { loadMessageRemoteSynchronous(accountId, folderId, uid, listener, false) }
    }

    private fun loadMessageRemoteSynchronous(
        accountId: AccountId, folderId: Long, messageServerId: String,
        listener: MessagingListener?, loadPartialFromSearch: Boolean,
    ) {
        try {
            require(!messageServerId.startsWith(localMessageUidPrefixProvider.get())) {
                "Must not be called with a local UID"
            }

            val folderServerId = getFolderServerId(accountId, folderId)

            val backend = getBackend(accountId)

            if (loadPartialFromSearch) {
                val syncConfig = createSyncConfig(accountId)
                backend.downloadMessage(syncConfig, folderServerId, messageServerId)
            } else {
                backend.downloadCompleteMessageBlocking(Dispatchers.IO, folderServerId, messageServerId)
            }

            for (l in getListeners(listener)) {
                l.loadMessageRemoteFinished(accountId, folderId, messageServerId)
            }
        } catch (e: Exception) {
            for (l in getListeners(listener)) {
                l.loadMessageRemoteFailed(accountId, folderId, messageServerId, e)
            }

            notifyUserIfCertificateProblem(accountId, e, true)
            logger.error(TAG, e) { "Error while loading remote message" }
            syncDebugLogger.error("MessagingException", null) { "Error while loading remote message" }
        }
    }

    @Throws(MessagingException::class)
    fun loadMessage(accountId: AccountId, folderId: Long, uid: String?): LocalMessage {
        val localStore = localStoreProvider.getInstance(accountId)
        val localFolder = localStore.getFolder(folderId)
        localFolder.open()

        val message = localFolder.getMessage(uid)
        if (message == null || message.databaseId == 0L) {
            val folderName = localFolder.name
            throw IllegalArgumentException("Message not found: folder=$folderName, uid=$uid")
        }

        val fp = FetchProfile()
        fp.add(FetchProfile.Item.BODY)
        localFolder.fetch(listOf(message), fp, null)

        return message
    }

    @Throws(MessagingException::class)
    fun loadMessageMetadata(accountId: AccountId, folderId: Long, uid: String?): LocalMessage {
        val localStore = localStoreProvider.getInstance(accountId)
        val localFolder = localStore.getFolder(folderId)
        localFolder.open()

        val message = localFolder.getMessage(uid)
        if (message == null || message.databaseId == 0L) {
            val folderName = localFolder.name
            throw IllegalArgumentException("Message not found: folder=$folderName, uid=$uid")
        }

        val fp = FetchProfile()
        fp.add(FetchProfile.Item.ENVELOPE)
        localFolder.fetch(listOf(message), fp, null)

        return message
    }

    fun markMessageAsOpened(accountId: AccountId, message: LocalMessage) {
        threadPool.execute {
            notificationController.removeNewMailNotification(
                accountId,
                message.makeMessageReference()
            )
        }

        if (message.isSet(Flag.SEEN)) {
            // Nothing to do if the message is already marked as read
            return
        }

        val account = accountManager.findById(accountId) ?: return

        val markMessageAsRead = account.isMarkMessageAsReadOnView
        if (markMessageAsRead) {
            // Mark the message itself as read right away
            try {
                message.setFlagInternal(Flag.SEEN, true)
            } catch (e: MessagingException) {
                logger.error(TAG, e) { "Error while marking message as read" }
            }

            // Also mark the message as read in the cache
            val messageIds = listOf(message.databaseId)
            setFlagInCache(accountId, messageIds, Flag.SEEN, true)
        }

        putBackground(
            "markMessageAsOpened", null,
        ) {
            markMessageAsOpenedBlocking(accountId, message, markMessageAsRead)
        }
    }

    private fun markMessageAsOpenedBlocking(
        accountId: AccountId, message: LocalMessage,
        markMessageAsRead: Boolean,
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
        val messageIds = listOf(message.databaseId)
        setFlagSynchronous(accountId, messageIds, Flag.SEEN, true, false)
    }

    private fun markMessageAsNotNew(accountId: AccountId, message: LocalMessage) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        val folderId = message.folder.databaseId
        val messageServerId = message.uid
        messageStore.setNewMessageState(folderId, messageServerId, false)
    }

    fun clearNewMessages(accountId: AccountId) {
        put("clearNewMessages", null) { clearNewMessagesBlocking(accountId) }
    }

    private fun clearNewMessagesBlocking(accountId: AccountId) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.clearNewMessageState()
    }

    fun loadAttachment(
        accountId: AccountId, message: LocalMessage, part: Part,
        listener: MessagingListener?,
    ) {
        put("loadAttachment", listener) {
            try {
                val folderServerId = message.folder.serverId

                val localStore = localStoreProvider.getInstance(accountId)
                val localFolder = localStore.getFolder(folderServerId)

                val bodyFactory = ProgressBodyFactory(
                    object : ProgressBodyFactory.ProgressListener {
                        override fun updateProgress(progress: Int) {
                            for (listener in listeners) {
                                listener.updateProgress(progress)
                            }
                        }
                    },
                )

                val backend = getBackend(accountId)
                backend.fetchPart(folderServerId, message.uid, part, bodyFactory)

                localFolder.addPartToMessage(message, part)

                for (l in getListeners(listener)) {
                    l.loadAttachmentFinished(accountId, message, part)
                }
            } catch (me: MessagingException) {
                logger.verbose(TAG, me) { "Exception loading attachment" }

                for (l in getListeners(listener)) {
                    l.loadAttachmentFailed(accountId, message, part, me.message)
                }
                notifyUserIfCertificateProblem(accountId, me, true)
            }
        }
    }

    /**
     * Stores the given message in the Outbox and starts a sendPendingMessages command to attempt to send the message.
     */
    fun sendMessage(
        accountId: AccountId, message: Message, plaintextSubject: String?,
        listener: MessagingListener?,
    ) {
        try {
            val outboxFolderId = outboxFolderManager
                .getOutboxFolderIdSync(
                    accountId,
                    true,
                )

            message.setFlag(Flag.SEEN, true)

            val messageStore = messageStoreManager.getMessageStore(accountId)
            val messageData = saveMessageDataCreator.createSaveMessageData(
                message, MessageDownloadState.FULL, plaintextSubject,
            )
            val messageId = messageStore.saveLocalMessage(outboxFolderId, messageData, null)

            val localStore = localStoreProvider.getInstance(accountId)
            val outboxStateRepository = localStore.outboxStateRepository
            outboxStateRepository.initializeOutboxState(messageId)

            sendPendingMessages(accountId, listener)
        } catch (e: Exception) {
            logger.error(TAG, e) { "Error sending message" }
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
        listener: MessagingListener?,
    ) {
        putBackground("sendPendingMessages", listener) {
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
                true,
            )
        if (outboxFolderId == -1L) {
            logger.warn(TAG) { "Could not get Outbox folder ID from Account" }
            return false
        }

        val messageStore = messageStoreManager.getMessageStore(accountId)
        return messageStore.getMessageCount(outboxFolderId) > 0
    }

    /**
     * Attempt to send any messages that are sitting in the Outbox.
     */
    @VisibleForTesting
    fun sendPendingMessagesSynchronous(accountId: AccountId) {
        var lastFailure: Exception? = null
        try {
            if (isAuthenticationProblem(accountId, false)) {
                logger.debug(TAG) { "Authentication will fail. Skip sending messages." }
                handleAuthenticationFailure(accountId, false)
                return
            }

            val localStore = localStoreProvider.getInstance(accountId)
            val outboxFolderId = outboxFolderManager
                .getOutboxFolderIdSync(
                    accountId,
                    true,
                )
            val localFolder = localStore.getFolder(outboxFolderId)
            if (!localFolder.exists()) {
                logger.warn(TAG) { "Outbox does not exist" }
                return
            }

            localFolder.open()

            val localMessages = localFolder.messages
            var progress = 0
            val todo = localMessages.size
            for (l in listeners) {
                l.synchronizeMailboxProgress(accountId, outboxFolderId, progress, todo)
            }
            /*
             * The profile we will use to pull all of the content
             * for a given local message into memory for sending.
             */
            val fp = FetchProfile()
            fp.add(FetchProfile.Item.ENVELOPE)
            fp.add(FetchProfile.Item.BODY)

            logger.info(TAG) { "Scanning Outbox folder for messages to send" }

            val backend = getBackend(accountId)

            for (message in localMessages) {
                if (message.isSet(Flag.DELETED)) {
                    // TODO: When uploading a message to the remote Sent folder the move code creates a placeholder
                    // message in the Outbox. This code gets rid of these messages. It'd be preferable if the
                    // placeholder message was never created, though.
                    message.destroy()
                    continue
                }
                val failure = sendPendingMessage(accountId, localStore, localFolder, message, fp, backend) {
                    progress++
                    for (l in listeners) {
                        l.synchronizeMailboxProgress(accountId, outboxFolderId, progress, todo)
                    }
                }
                if (failure != null) {
                    lastFailure = failure
                }
            }

            if (lastFailure != null) {
                notificationController.showSendFailedNotification(accountId, lastFailure)
            }
        } catch (e: Exception) {
            logger.verbose(TAG, e) { "Failed to send pending messages" }
        } finally {
            if (lastFailure == null) {
                notificationController.clearSendFailedNotification(accountId)
            }
        }
    }

    /**
     * Returns the failure, or `null` when the message was sent or skipped because it is a draft.
     */
    private fun sendPendingMessage(
        accountId: AccountId, localStore: LocalStore, localFolder: LocalFolder, message: LocalMessage,
        fetchProfile: FetchProfile, backend: Backend, onSent: () -> Unit,
    ): Exception? {
        val outboxStateRepository = localStore.outboxStateRepository
        var lastFailure: Exception? = null
        try {
            val messageId = message.databaseId
            val outboxState = outboxStateRepository.getOutboxState(messageId)

            val sendState = outboxState.sendState
            if (sendState != SendState.READY) {
                logger.verbose(TAG) {
                    "Skipping sending message ${message.uid} (reason: ${sendState.databaseName} - " +
                        "${outboxState.sendError})"
                }

                if (sendState == SendState.RETRIES_EXCEEDED) {
                    lastFailure = MessagingException("Retries exceeded", true)
                } else {
                    lastFailure = MessagingException(outboxState.sendError, true)
                }
                return lastFailure
            }

            logger.info(TAG) { "Send count for message ${message.uid} is ${outboxState.numberOfSendAttempts}" }

            localFolder.fetch(listOf(message), fetchProfile, null)
            val failure = sendFetchedMessage(
                accountId, localStore, localFolder, message, outboxState, backend, onSent,
            )
            if (failure != null) {
                lastFailure = failure
            }
        } catch (e: Exception) {
            lastFailure = e

            logger.error(TAG, e) { "Failed to fetch message for sending" }
            notifySynchronizeMailboxFailed(accountId, localFolder, e)
        }

        return lastFailure
    }

    /**
     * Returns the failure, or `null` when the message was sent or skipped because it is a draft.
     */
    @Throws(MessagingException::class)
    private fun sendFetchedMessage(
        accountId: AccountId, localStore: LocalStore, localFolder: LocalFolder, message: LocalMessage,
        outboxState: OutboxState, backend: Backend, onSent: () -> Unit,
    ): Exception? {
        val outboxStateRepository = localStore.outboxStateRepository
        val messageId = message.databaseId
        var lastFailure: Exception? = null
        try {
            if (message.getHeader(K9.IDENTITY_HEADER).size > 0 || message.isSet(Flag.DRAFT)) {
                logger.verbose(TAG) {
                    "The user has set the Outbox and Drafts folder to the same thing. This message " +
                        "appears to be a draft, so K-9 will not send it"
                }
                return null
            }

            outboxStateRepository.incrementSendAttempts(messageId)
            message.setFlag(Flag.X_SEND_IN_PROGRESS, true)

            logger.info(TAG) { "Sending message with UID ${message.uid}" }
            backend.sendMessage(message)

            message.setFlag(Flag.X_SEND_IN_PROGRESS, false)
            message.setFlag(Flag.SEEN, true)
            onSent()
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
            } else if (outboxState.numberOfSendAttempts + 1 >= K9.MAX_SEND_ATTEMPTS) {
                outboxStateRepository.setSendAttemptsExceeded(messageId)
            }

            handleSendFailure(accountId, localFolder, message, e)
        } catch (e: Exception) {
            lastFailure = e

            handleSendFailure(accountId, localFolder, message, e)
        }

        return lastFailure
    }

    @Throws(MessagingException::class)
    private fun moveOrDeleteSentMessage(
        accountId: AccountId, localStore: LocalStore, message: LocalMessage,
    ) {
        val account = accountManager.findById(accountId)
        if (!account!!.hasSentFolder() || !account.isUploadSentMessages) {
            logger.info(TAG) { "Not uploading sent message; deleting local message" }
            message.destroy()
        } else {
            val sentFolderId = account.sentFolderId!!
            val sentFolder = localStore.getFolder(sentFolderId)
            sentFolder.open()
            val sentFolderServerId = sentFolder.serverId
            logger.info(TAG) { "Moving sent message to folder '$sentFolderServerId' ($sentFolderId)" }

            val messageStore = messageStoreManager.getMessageStore(accountId)
            val destinationMessageId = messageStore.moveMessage(message.databaseId, sentFolderId)

            logger.info(TAG) { "Moved sent message to folder '$sentFolderServerId' ($sentFolderId)" }

            if (!sentFolder.isLocalOnly) {
                val destinationUid = messageStore.getMessageServerId(destinationMessageId)
                if (destinationUid != null) {
                    val command = PendingAppend.create(sentFolderId, destinationUid)
                    queuePendingCommand(accountId, command)
                    processPendingCommands(accountId)
                }
            }
        }

        val outboxFolderId = outboxFolderManager
            .getOutboxFolderIdSync(
                accountId,
                true,
            )
        for (listener in listeners) {
            listener.folderStatusChanged(accountId, outboxFolderId)
        }
    }

    @Throws(MessagingException::class)
    private fun handleSendFailure(
        accountId: AccountId?, localFolder: LocalFolder, message: Message,
        exception: Exception,
    ) {
        logger.error(TAG, exception) { "Failed to send message" }
        message.setFlag(Flag.X_SEND_FAILED, true)

        notifySynchronizeMailboxFailed(accountId, localFolder, exception)
    }

    private fun notifySynchronizeMailboxFailed(
        accountId: AccountId?, localFolder: LocalFolder,
        exception: Exception,
    ) {
        val folderId = localFolder.databaseId
        val errorMessage = exception.rootCauseMessage
        for (listener in listeners) {
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
        messageReferences: List<MessageReference>, destFolderId: Long,
    ) {
        actOnMessageGroup(
            srcAccountId, srcFolderId, messageReferences,
        ) { accountId, _, messages ->
            suppressMessages(accountId, messages)
            putBackground("moveMessages", null) {
                moveOrCopyMessageSynchronous(
                    accountId,
                    srcFolderId,
                    messages,
                    destFolderId,
                    MoveOrCopyFlavor.MOVE,
                )
            }
        }
    }

    fun moveMessagesInThread(
        srcAccountId: AccountId, srcFolderId: Long,
        messageReferences: List<MessageReference>, destFolderId: Long,
    ) {
        actOnMessageGroup(
            srcAccountId, srcFolderId, messageReferences,
        ) { accountId, _, messages ->
            suppressMessages(accountId, messages)
            putBackground("moveMessagesInThread", null) {
                try {
                    val messagesInThreads = collectMessagesInThreads(accountId, messages)
                    moveOrCopyMessageSynchronous(
                        accountId, srcFolderId, messagesInThreads, destFolderId,
                        MoveOrCopyFlavor.MOVE,
                    )
                } catch (e: MessagingException) {
                    logger.error(TAG, e) { "Exception while moving messages" }
                }
            }
        }
    }

    fun moveMessage(accountId: AccountId, srcFolderId: Long, message: MessageReference, destFolderId: Long) {
        moveMessages(accountId, srcFolderId, listOf(message), destFolderId)
    }

    fun copyMessages(
        srcAccountId: AccountId,
        srcFolderId: Long,
        messageReferences: List<MessageReference>,
        destFolderId: Long,
    ) {
        actOnMessageGroup(
            srcAccountId, srcFolderId, messageReferences,
        ) { _, _, messages ->
            putBackground("copyMessages", null) {
                moveOrCopyMessageSynchronous(
                    srcAccountId,
                    srcFolderId,
                    messages,
                    destFolderId,
                    MoveOrCopyFlavor.COPY,
                )
            }
        }
    }

    fun copyMessagesInThread(
        srcAccountId: AccountId,
        srcFolderId: Long,
        messageReferences: List<MessageReference>,
        destFolderId: Long,
    ) {
        actOnMessageGroup(
            srcAccountId, srcFolderId, messageReferences,
        ) { accountId, _, messages ->
            putBackground("copyMessagesInThread", null) {
                try {
                    val messagesInThreads = collectMessagesInThreads(accountId, messages)
                    moveOrCopyMessageSynchronous(
                        accountId, srcFolderId, messagesInThreads, destFolderId,
                        MoveOrCopyFlavor.COPY,
                    )
                } catch (e: MessagingException) {
                    logger.error(TAG, e) { "Exception while copying messages" }
                }
            }
        }
    }

    fun copyMessage(accountId: AccountId, srcFolderId: Long, message: MessageReference, destFolderId: Long) {
        copyMessages(accountId, srcFolderId, listOf(message), destFolderId)
    }

    internal fun moveOrCopyMessageSynchronous(
        accountId: AccountId, srcFolderId: Long, inMessages: List<LocalMessage>,
        destFolderId: Long, operation: MoveOrCopyFlavor,
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
            val uids = LinkedList<String>()
            for (message in inMessages) {
                val uid = message.uid
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
                moveOrCopyLocalMessages(accountId, srcFolderId, destFolderId, messages, operation, unreadCountAffected)
            }

            processPendingCommands(accountId)
        } catch (me: MessagingException) {
            throw IllegalStateException("Error moving message", me)
        }
    }

    @Throws(MessagingException::class)
    private fun moveOrCopyLocalMessages(
        accountId: AccountId, srcFolderId: Long, destFolderId: Long, messages: List<LocalMessage>,
        operation: MoveOrCopyFlavor, unreadCountAffected: Boolean,
    ) {
        logger.info(TAG) {
            "moveOrCopyMessageSynchronous: source folder = $srcFolderId, ${messages.size} messages, " +
                "destination folder = $destFolderId, operation = ${operation.name}"
        }

        val messageStore = messageStoreManager.getMessageStore(accountId)

        val messageIds = ArrayList<Long>()
        val messageIdToUidMapping = HashMap<Long, String>()
        for (message in messages) {
            val messageId = message.databaseId
            messageIds.add(messageId)
            messageIdToUidMapping[messageId] = message.uid
        }

        val resultIdMapping: Map<Long, Long>?
        if (operation == MoveOrCopyFlavor.COPY) {
            resultIdMapping = messageStore.copyMessages(messageIds, destFolderId)

            if (unreadCountAffected) {
                // If this copy operation changes the unread count in the destination
                // folder, notify the listeners.
                for (l in listeners) {
                    l.folderStatusChanged(accountId, destFolderId)
                }
            }
        } else {
            resultIdMapping = messageStore.moveMessages(messageIds, destFolderId)

            unsuppressMessages(accountId, messages)

            if (unreadCountAffected) {
                // If this move operation changes the unread count, notify the listeners
                // that the unread count changed in both the source and destination folder.
                for (l in listeners) {
                    l.folderStatusChanged(accountId, srcFolderId)
                    l.folderStatusChanged(accountId, destFolderId)
                }
            }
        }

        val destinationMapping: Map<Long, String> =
            messageStore.getMessageServerIds(resultIdMapping.values)

        val uidMap = HashMap<String?, String?>()
        for ((sourceMessageId, destinationMessageId) in resultIdMapping) {
            val sourceUid = messageIdToUidMapping[sourceMessageId]
            val destinationUid = destinationMapping[destinationMessageId]
            uidMap[sourceUid] = destinationUid
        }

        queueMoveOrCopy(accountId, srcFolderId, destFolderId, operation, uidMap)
    }

    fun moveToDraftsFolder(accountId: AccountId, folderId: Long, messages: List<MessageReference>) {
        putBackground("moveToDrafts", null) { moveToDraftsFolderInBackground(accountId, folderId, messages) }
    }

    private fun moveToDraftsFolderInBackground(
        accountId: AccountId, folderId: Long,
        messages: List<MessageReference>,
    ) {
        for ((_, _, uid) in messages) {
            try {
                val message = loadMessage(accountId, folderId, uid)
                val draftMessageId = saveDraft(accountId, message, null, message.subject)

                val draftSavedSuccessfully = draftMessageId != null
                if (draftSavedSuccessfully) {
                    message.destroy()
                }

                for (listener in listeners) {
                    listener.folderStatusChanged(accountId, folderId)
                }
            } catch (e: MessagingException) {
                logger.error(TAG, e) { "Error loading message. Draft was not saved." }
            }
        }
    }

    fun archiveThreads(messages: List<MessageReference>) {
        archiveOperations.archiveThreads(messages)
    }

    fun archiveMessages(messages: List<MessageReference>) {
        archiveOperations.archiveMessages(messages)
    }

    fun archiveMessage(message: MessageReference) {
        archiveOperations.archiveMessage(message)
    }

    fun expunge(accountId: AccountId, folderId: Long) {
        putBackground("expunge", null) {
            queueExpunge(accountId, folderId)
            processPendingCommands(accountId)
        }
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
            logger.warn(TAG) { "Account not found $accountId" }
            return
        }
        val folderId = account.draftsFolderId
        if (folderId == null) {
            logger.warn(TAG) { "No Drafts folder configured. Can't delete draft." }
            return
        }

        val messageStore = messageStoreManager.getMessageStore(accountId)
        val messageServerId = messageStore.getMessageServerId(messageId)
        if (messageServerId != null) {
            val messageReference = MessageReference(accountId, folderId, messageServerId)
            deleteMessages(listOf(messageReference), skipTrashFolder)
        }
    }

    fun deleteThreads(messages: List<MessageReference>) {
        actOnMessagesGroupedByAccountAndFolder(
            messages,
        ) { accountId, messageFolder, accountMessages ->
            suppressMessages(accountId, accountMessages)
            putBackground("deleteThreads", null) {
                deleteThreadsSynchronous(
                    accountId,
                    messageFolder.databaseId,
                    accountMessages,
                    false,
                )
            }
        }
    }

    private fun deleteThreadsSynchronous(
        accountId: AccountId, folderId: Long, messages: List<LocalMessage>,
        skipTrashFolder: Boolean,
    ) {
        try {
            val messagesToDelete = collectMessagesInThreads(accountId, messages)
            deleteMessagesSynchronous(accountId, folderId, messagesToDelete, skipTrashFolder)
        } catch (e: MessagingException) {
            logger.error(TAG, e) { "Something went wrong while deleting threads" }
        }
    }

    @Throws(MessagingException::class)
    fun collectMessagesInThreads(accountId: AccountId, messages: List<LocalMessage>): List<LocalMessage> {
        val localStore = localStoreProvider.getInstance(accountId)

        val messagesInThreads = ArrayList<LocalMessage>()
        for (localMessage in messages) {
            val rootId = localMessage.rootId
            val threadId = if (rootId == -1L) localMessage.threadId else rootId

            val messagesInThread = localStore.getMessagesInThread(threadId)

            messagesInThreads.addAll(messagesInThread)
        }

        return messagesInThreads
    }

    fun deleteMessage(message: MessageReference) {
        deleteMessages(listOf(message), false)
    }

    fun deleteMessages(messages: List<MessageReference>) {
        deleteMessages(messages, false)
    }

    private fun deleteMessages(messages: List<MessageReference>, skipTrashFolder: Boolean) {
        actOnMessagesGroupedByAccountAndFolder(
            messages,
        ) { accountId, messageFolder, accountMessages ->
            suppressMessages(accountId, accountMessages)
            putBackground("deleteMessages", null) {
                deleteMessagesSynchronous(
                    accountId,
                    messageFolder.databaseId,
                    accountMessages,
                    skipTrashFolder,
                )
            }
        }
    }

    private fun deleteMessagesSynchronous(
        accountId: AccountId, folderId: Long, messages: List<LocalMessage>,
        skipTrashFolder: Boolean,
    ) {
        try {
            val localOnlyMessages = ArrayList<LocalMessage>()
            val syncedMessages = ArrayList<LocalMessage>()
            val syncedMessageUids = ArrayList<String>()
            for (message in messages) {
                notificationController.removeNewMailNotification(accountId, message.makeMessageReference())

                val uid = message.uid
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

            var uidMap: HashMap<String?, String?>? = null
            val account = accountManager.findById(accountId)
            if (account == null) {
                logger.warn(TAG) { "Account not found $accountId" }
                return
            }
            val trashFolderId = account.trashFolderId
            val doNotMoveToTrashFolder = skipTrashFolder ||
                localDeleteOperationDecider.isDeleteImmediately(accountId, folderId)

            var localTrashFolder: LocalFolder? = null
            if (doNotMoveToTrashFolder) {
                logger.debug(TAG) { "Not moving deleted messages to local Trash folder. Removing local copies." }

                if (localOnlyMessages.isNotEmpty()) {
                    localFolder.destroyMessages(localOnlyMessages)
                }
                if (syncedMessages.isNotEmpty()) {
                    localFolder.setFlags(syncedMessages, setOf(Flag.DELETED), true)
                }
            } else {
                logger.debug(TAG) { "Deleting messages in normal folder, moving" }
                localTrashFolder = localStore.getFolder(trashFolderId!!)

                val messageStore = messageStoreManager.getMessageStore(accountId)

                val messageIds = ArrayList<Long>()
                val messageIdToUidMapping = HashMap<Long, String>()
                for (message in messages) {
                    val messageId = message.databaseId
                    messageIds.add(messageId)
                    messageIdToUidMapping[messageId] = message.uid
                }

                val moveMessageIdMapping: Map<Long, Long> =
                    messageStore.moveMessages(messageIds, trashFolderId)

                val destinationMapping: Map<Long, String> =
                    messageStore.getMessageServerIds(moveMessageIdMapping.values)
                uidMap = HashMap()
                for ((sourceMessageId, destinationMessageId) in moveMessageIdMapping) {
                    val sourceUid = messageIdToUidMapping[sourceMessageId]
                    val destinationUid = destinationMapping[destinationMessageId]
                    uidMap[sourceUid] = destinationUid
                }

                if (account.isMarkMessageAsReadOnDelete) {
                    val destinationMessageIds = moveMessageIdMapping.values
                    messageStore.setFlag(destinationMessageIds, Flag.SEEN, true)
                }
            }

            for (l in listeners) {
                l.folderStatusChanged(accountId, folderId)
                if (localTrashFolder != null) {
                    l.folderStatusChanged(accountId, trashFolderId!!)
                }
            }

            logger.debug(TAG) { "Delete policy for account $accountId is ${account.deletePolicy}" }

            val outboxFolderId = outboxFolderManager
                .getOutboxFolderIdSync(
                    accountId,
                    true,
                )

            if (outboxFolderId != -1L && folderId == outboxFolderId && supportsUpload(accountId)) {
                for (destinationUid in uidMap!!.values) {
                    // If the message was in the Outbox, then it has been copied to local Trash, and has
                    // to be copied to remote trash
                    val command = PendingAppend.create(trashFolderId!!, destinationUid)
                    queuePendingCommand(accountId, command)
                }
                processPendingCommands(accountId)
            } else if (localFolder.isLocalOnly) {
                // Nothing to do on the remote side
            } else if (syncedMessageUids.isNotEmpty()) {
                if (account.deletePolicy == DeletePolicy.ON_DELETE) {
                    if (doNotMoveToTrashFolder || !backend.supportsTrashFolder) {
                        queueDelete(accountId, folderId, syncedMessageUids)
                    } else if (account.isMarkMessageAsReadOnDelete) {
                        queueMoveOrCopy(
                            accountId, folderId, trashFolderId!!,
                            MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ, uidMap,
                        )
                    } else {
                        queueMoveOrCopy(
                            accountId, folderId, trashFolderId!!,
                            MoveOrCopyFlavor.MOVE, uidMap,
                        )
                    }
                    processPendingCommands(accountId)
                } else if (account.deletePolicy == DeletePolicy.MARK_AS_READ) {
                    queueSetFlag(accountId, localFolder.databaseId, true, Flag.SEEN, syncedMessageUids)
                    processPendingCommands(accountId)
                } else {
                    logger.debug(TAG) { "Delete policy ${account.deletePolicy} prevents delete from server" }
                }
            }

            unsuppressMessages(accountId, messages)
        } catch (me: MessagingException) {
            throw IllegalStateException("Error deleting message from local store.", me)
        }
    }

    @Throws(MessagingException::class)
    fun processPendingEmptySpam(accountId: AccountId) {
        val account = accountManager.findById(accountId)
        if (account == null || !account.hasSpamFolder()) {
            return
        }

        val spamFolderId = account.spamFolderId ?: return

        val localStore = localStoreProvider.getInstance(accountId)
        val folder = localStore.getFolder(spamFolderId)
        folder.open()
        val spamFolderServerId = folder.serverId

        val backend = getBackend(accountId)
        backend.deleteAllMessages(spamFolderServerId)

        // Remove all messages marked as deleted
        folder.destroyDeletedMessages()

        compact(accountId)
    }

    fun emptySpam(accountId: AccountId, listener: MessagingListener?) {
        putBackground("emptySpam", listener) {
            try {
                val account = accountManager.findById(accountId) ?: return@putBackground
                val spamFolderId = account.spamFolderId
                if (spamFolderId == null) {
                    logger.warn(TAG) { "No Spam folder configured. Can't empty spam." }
                    return@putBackground
                }

                val localStore = localStoreProvider.getInstance(accountId)
                val localFolder = localStore.getFolder(spamFolderId)
                localFolder.open()

                localFolder.destroyLocalOnlyMessages()
                localFolder.setFlags(setOf(Flag.DELETED), true)

                for (l in listeners) {
                    l.folderStatusChanged(accountId, spamFolderId)
                }

                val command = PendingEmptySpam.create()
                queuePendingCommand(accountId, command)
                processPendingCommands(accountId)
            } catch (e: Exception) {
                logger.error(TAG, e) { "emptySpam failed" }
            }
        }
    }

    @Throws(MessagingException::class)
    fun processPendingEmptyTrash(accountId: AccountId) {
        val account = accountManager.findById(accountId)
        if (account == null || !account.hasTrashFolder()) {
            return
        }

        val trashFolderId = account.trashFolderId ?: return

        val localStore = localStoreProvider.getInstance(accountId)
        val folder = localStore.getFolder(trashFolderId)
        folder.open()
        val trashFolderServerId = folder.serverId

        val backend = getBackend(accountId)
        backend.deleteAllMessages(trashFolderServerId)

        // Remove all messages marked as deleted
        folder.destroyDeletedMessages()

        compact(accountId)
    }

    fun emptyTrash(accountId: AccountId, listener: MessagingListener?) {
        putBackground("emptyTrash", listener) {
            try {
                val account = accountManager.findById(accountId) ?: return@putBackground
                val trashFolderId = account.trashFolderId
                if (trashFolderId == null) {
                    logger.warn(TAG) { "No Trash folder configured. Can't empty trash." }
                    return@putBackground
                }

                val localStore = localStoreProvider.getInstance(accountId)
                val localFolder = localStore.getFolder(trashFolderId)
                localFolder.open()

                val isTrashLocalOnly = isTrashLocalOnly(accountId)
                if (isTrashLocalOnly) {
                    localFolder.clearAllMessages()
                } else {
                    localFolder.destroyLocalOnlyMessages()
                    localFolder.setFlags(setOf(Flag.DELETED), true)
                }

                for (l in listeners) {
                    l.folderStatusChanged(accountId, trashFolderId)
                }

                if (!isTrashLocalOnly) {
                    val command = PendingEmptyTrash.create()
                    queuePendingCommand(accountId, command)
                    processPendingCommands(accountId)
                }
            } catch (e: Exception) {
                logger.error(TAG, e) { "emptyTrash failed" }
            }
        }
    }

    fun clearFolder(accountId: AccountId, folderId: Long) {
        putBackground("clearFolder", null) { clearFolderSynchronous(accountId, folderId) }
    }

    @VisibleForTesting
    fun clearFolderSynchronous(accountId: AccountId, folderId: Long) {
        try {
            val localFolder = localStoreProvider.getInstance(accountId).getFolder(folderId)
            localFolder.open()
            localFolder.clearAllMessages()
        } catch (e: Exception) {
            logger.error(TAG, e) { "clearFolder failed" }
        }
    }

    /**
     * Find out whether the account type only supports a local Trash folder.
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
            },
        )

        logger.verbose(TAG) { "performPeriodicMailSync($accountId) about to await latch release" }

        try {
            latch.await()
            logger.verbose(TAG) { "performPeriodicMailSync($accountId) got latch release" }
        } catch (e: Exception) {
            logger.error(TAG, e) { "Interrupted while awaiting latch release" }
        }

        val success = !syncError.value
        if (success) {
            val now = System.currentTimeMillis()
            val updatedAccount = accountManager.updateSync(
                accountId,
            ) { account: LegacyAccount -> account.updateLastSyncTime(now) }
            if (updatedAccount == null) {
                logger.warn(TAG) { "Account not found $accountId" }
            } else {
                logger.verbose(TAG) { "Account $accountId successfully synced @ ${Date(now)}" }
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
        listener: MessagingListener?,
    ) {
        val wakeLock: WakeLock?
        if (useManualWakeLock) {
            val pm = DI.get(PowerManager::class.java)

            wakeLock = pm.newWakeLock("K9 MessagingController.checkMail")
            wakeLock.setReferenceCounted(false)
            wakeLock.acquire(K9.MANUAL_WAKE_LOCK_TIMEOUT.toLong())
        } else {
            wakeLock = null
        }

        for (l in getListeners(listener)) {
            l.checkMailStarted(context, accountId)
        }
        putBackground("checkMail", listener) {
            try {
                logger.info(TAG) { "Starting mail check" }

                val accountIds: MutableList<AccountId>
                if (accountId != null) {
                    accountIds = ArrayList(1)
                    accountIds.add(accountId)
                } else {
                    val accounts = accountManager.findAll()
                    accountIds = ArrayList(accounts.size)
                    for ((id) in accountManager.findAll()) {
                        accountIds.add(id)
                    }
                }

                for (synAccountId in accountIds) {
                    checkMailForAccount(synAccountId, ignoreLastCheckedTime, notify, listener)
                }
            } catch (e: Exception) {
                logger.error(TAG, e) { "Unable to synchronize mail" }
            }
            putBackground("finalize sync", null) {
                logger.info(TAG) { "Finished mail sync" }

                wakeLock?.release()
                for (l in getListeners(listener)) {
                    l.checkMailFinished(context, accountId)
                }
            }
        }
    }

    private fun checkMailForAccount(
        accountId: AccountId, ignoreLastCheckedTime: Boolean, notify: Boolean,
        listener: MessagingListener?,
    ) {
        logger.info(TAG) { "Synchronizing account $accountId" }

        val notificationState = NotificationState()

        sendPendingMessages(accountId, listener)

        refreshFolderListIfStale(accountId)

        try {
            val localStore = localStoreProvider.getInstance(accountId)
            for (folder in localStore.getPersonalNamespaces(false)) {
                folder.open()

                // Never sync a folder that isn't displayed or not enabled for sync
                if (!folder.isVisible || !folder.isSyncEnabled) {
                    continue
                }

                synchronizeFolder(accountId, folder, ignoreLastCheckedTime, notify, listener, notificationState)
            }
        } catch (e: MessagingException) {
            logger.error(TAG, e) { "Unable to synchronize account $accountId" }
        } finally {
            putBackground("clear notification flag for $accountId", null) {
                logger.verbose(TAG) { "Clearing notification flag for $accountId" }

                clearFetchingMailNotification(accountId)
            }
        }
    }

    private fun synchronizeFolder(
        accountId: AccountId, folder: LocalFolder, ignoreLastCheckedTime: Boolean,
        notify: Boolean, listener: MessagingListener?, notificationState: NotificationState,
    ) {
        putBackground("sync${folder.serverId}", null) {
            synchronizeFolderInBackground(
                accountId,
                folder,
                ignoreLastCheckedTime,
                notify,
                listener,
                notificationState,
            )
        }
    }

    private fun synchronizeFolderInBackground(
        accountId: AccountId, folder: LocalFolder,
        ignoreLastCheckedTime: Boolean,
        notify: Boolean, listener: MessagingListener?, notificationState: NotificationState,
    ) {
        logger.verbose(TAG) { "Folder ${folder.serverId} was last synced @ ${Date(folder.lastChecked)}" }

        if (!ignoreLastCheckedTime) {
            val lastCheckedTime = folder.lastChecked
            val now = System.currentTimeMillis()

            if (lastCheckedTime > now) {
                // The time this folder was last checked lies in the future. We better ignore this and sync now.
            } else {
                val account = accountManager.findById(accountId) ?: return
                val syncInterval = account.automaticCheckIntervalMinutes * 60L * 1000L
                val nextSyncTime = lastCheckedTime + syncInterval
                if (nextSyncTime > now) {
                    logger.verbose(TAG) {
                        "Not syncing folder ${folder.serverId}, previously synced @ ${Date(lastCheckedTime)} which " +
                            "would be too recent for the account sync interval"
                    }
                    return
                }
            }
        }

        try {
            showFetchingMailNotificationIfNecessary(accountId, folder)
            try {
                synchronizeMailboxSynchronous(accountId, folder.databaseId, notify, listener, notificationState)
            } finally {
                showEmptyFetchingMailNotificationIfNecessary(accountId)
            }
        } catch (e: Exception) {
            logger.error(TAG, e) { "Exception while processing folder $accountId:${folder.serverId}" }
        }
    }

    private fun showFetchingMailNotificationIfNecessary(accountId: AccountId, folder: LocalFolder) {
        val account = accountManager.findById(accountId) ?: return
        if (account.isNotifySync) {
            notificationController.showFetchingMailNotification(accountId, folder)
        }
    }

    private fun showEmptyFetchingMailNotificationIfNecessary(accountId: AccountId) {
        val account = accountManager.findById(accountId) ?: return
        if (account.isNotifySync) {
            notificationController.showEmptyFetchingMailNotification(accountId)
        }
    }

    private fun clearFetchingMailNotification(accountId: AccountId) {
        notificationController.clearFetchingMailNotification(accountId)
    }

    fun compact(accountId: AccountId) {
        putBackground("compact: $accountId", null) {
            try {
                val messageStore = messageStoreManager.getMessageStore(accountId)
                messageStore.compact()
            } catch (e: Exception) {
                logger.error(TAG, e) { "Failed to compact account $accountId" }
            }
        }
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
            return message.databaseId
        } else {
            logger.warn(TAG) { "MessagingController.getId() called without a LocalMessage" }
            return null
        }
    }

    init {
        this.outboxFolderManager = outboxFolderManager
        this.accountManager = accountManager

        controllerThread = Thread { runInBackground() }
        controllerThread.name = "MessagingController"
        controllerThread.start()
        addListener(memorizingMessagingListener)

        initializeControllerExtensions(controllerExtensions)

        draftOperations = DraftOperations(
            this, messageStoreManager, saveMessageDataCreator, localMessageUidPrefixProvider,
            accountManager,
        )
        notificationOperations =
            NotificationOperations(notificationController, accountManager, outboxFolderManager)
        archiveOperations = ArchiveOperations(this, accountManager, featureFlagProvider)
    }

    private class Command : Comparable<Command> {
        var runnable: Runnable? = null
        var listener: MessagingListener? = null
        var description: String? = null
        var isForegroundPriority: Boolean = false

        var sequence: Int = sequencing.getAndIncrement()

        override fun compareTo(other: Command): Int {
            return if (other.isForegroundPriority && !isForegroundPriority) {
                1
            } else if (!other.isForegroundPriority && isForegroundPriority) {
                -1
            } else {
                (sequence - other.sequence)
            }
        }
    }

    fun clearNotifications(search: LocalMessageSearch) {
        put("clearNotifications", null) {
            notificationOperations.clearNotifications(search)
        }
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
        val account = accountManager.findById(accountId) ?: return false

        val serverSettings = getServerSettings(account, incoming)

        return serverSettings.isMissingCredentials ||
            serverSettings.authenticationType == AuthType.XOAUTH2 && account.oAuthState == null
    }

    private fun getServerSettings(accountId: AccountId, incoming: Boolean): ServerSettings? {
        return accountManager.findById(accountId)?.let { account -> getServerSettings(account, incoming) }
    }

    private fun getServerSettings(account: LegacyAccount, incoming: Boolean): ServerSettings {
        return if (incoming) account.incomingServerSettings else account.outgoingServerSettings
    }

    private fun clearAuthenticationErrorNotification(
        accountId: AccountId, incoming: Boolean, clearOnlyForOAuthAccounts: Boolean,
    ) {
        if (featureFlagProvider.provide(GeneratedFeatureFlagKey.DISPLAY_IN_APP_NOTIFICATIONS).isEnabled()) {
            var shouldClear = true
            val serverSettings = getServerSettings(accountId, incoming)
            if (clearOnlyForOAuthAccounts && serverSettings!!.authenticationType != AuthType.XOAUTH2) {
                shouldClear = false
            }

            if (shouldClear) {
                val notification = createAuthenticationErrorNotification(
                    accountId, incoming,
                )
                notificationManager.dismiss(notification!!)
                    .onEach { outcome -> logger.verbose(TAG) { "notificationDismisser outcome = $outcome" } }
                    .launchIn(notificationScope)
            }
        }
    }

    internal fun actOnMessagesGroupedByAccountAndFolder(messages: List<MessageReference>, actor: MessageActor) {
        val accountMap = groupMessagesByAccountAndFolder(messages)

        for ((accountId, folderMap) in accountMap)
            for ((folderId, messageList) in folderMap) {
                actOnMessageGroup(accountId, folderId, messageList, actor)
            }
    }

    private fun groupMessagesByAccountAndFolder(
        messages: List<MessageReference>,
    ): Map<AccountId, Map<Long, List<MessageReference>>> {
        return messages
            .groupBy { it.accountId }
            .mapValues { (_, accountMessages) -> accountMessages.groupBy { it.folderId } }
    }

    private fun actOnMessageGroup(
        accountId: AccountId, folderId: Long, messageReferences: List<MessageReference>, actor: MessageActor,
    ) {
        try {
            val messageFolder = localStoreProvider.getInstance(accountId).getFolder(folderId)
            val localMessages = messageFolder.getMessagesByReference(messageReferences)
            actor.act(accountId, messageFolder, localMessages)
        } catch (e: MessagingException) {
            logger.error(TAG, e) { "Error loading account?!" }
        }
    }

    internal fun interface MessageActor {
        fun act(accountId: AccountId, messageFolder: LocalFolder, messages: List<LocalMessage>)
    }

    internal inner class ControllerSyncListener(
        private val accountId: AccountId,
        private val listener: MessagingListener?,
        private val suppressNotifications: Boolean,
        private val notificationState: NotificationState,
    ) : SyncListener {
        private val localStore = getLocalStoreOrThrow(accountId)
        var syncFailed: Boolean = false

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
            numNewMessages: Int,
        ) {
            for (messagingListener in getListeners(listener)) {
                messagingListener.synchronizeMailboxHeadersFinished(
                    accountId, folderServerId, totalMessagesInMailbox,
                    numNewMessages,
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
            isOldMessage: Boolean,
        ) {
            // Send a notification of this message

            val message = loadMessage(folderServerId, messageServerId)
            val localFolder = message.folder
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
                syncRemovedMessage(folderServerId, message.uid)
            } else {
                val localFolder = message.folder
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
                throw IllegalStateException("Could not load message ($folderServerId:$messageServerId)", e)
            }
        }
    }

    internal enum class MoveOrCopyFlavor {
        MOVE, COPY, MOVE_AND_MARK_AS_READ
    }

    companion object {
        val SYNC_FLAGS: Set<Flag> = EnumSet.of(Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED, Flag.FORWARDED)

        private const val FOLDER_LIST_STALENESS_THRESHOLD = 30 * 60 * 1000L

        private const val TAG = "MessagingController"
        private const val STOP_TIMEOUT_MILLIS = 1000L
        private const val PUT_COMMAND_RETRY_DELAY_MILLIS = 200L

        @JvmStatic
        fun getInstance(): MessagingController {
            return DI.get(MessagingController::class.java)
        }

        private fun getUidsFromMessages(messages: List<LocalMessage>): List<String> {
            val uids = ArrayList<String>(messages.size)
            for (i in messages.indices) {
                uids.add(messages[i].uid)
            }
            return uids
        }

        private val sequencing = AtomicInteger(0)
    }
}
