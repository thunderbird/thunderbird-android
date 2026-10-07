package com.fsck.k9.controller

import android.content.Context
import android.os.Process
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import app.k9mail.legacy.di.DI
import app.k9mail.legacy.mailstore.FolderDetailsAccessor
import app.k9mail.legacy.mailstore.MessageStore
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.message.controller.MessagingControllerMailChecker
import app.k9mail.legacy.message.controller.MessagingControllerRegistry
import app.k9mail.legacy.message.controller.MessagingListener
import app.k9mail.legacy.message.controller.SimpleMessagingListener
import com.fsck.k9.K9
import com.fsck.k9.K9.MAX_SEND_ATTEMPTS
import com.fsck.k9.Preferences
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
import com.fsck.k9.mailstore.SaveMessageDataCreator
import com.fsck.k9.mailstore.SendState
import com.fsck.k9.notification.NotificationController
import com.fsck.k9.notification.NotificationStrategy
import java.util.Collections
import java.util.EnumSet
import java.util.LinkedList
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.PriorityBlockingQueue
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.concurrent.thread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.exception.rootCauseMessage
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.folder.api.hasPendingMessagesSync
import net.thunderbird.feature.mail.message.list.LocalDeleteOperationDecider
import net.thunderbird.feature.mail.message.list.LocalMessageUidPrefixProvider
import net.thunderbird.feature.notification.api.NotificationManager
import net.thunderbird.feature.notification.api.content.AuthenticationErrorNotification
import net.thunderbird.feature.notification.api.dismisser.NotificationDismisser
import net.thunderbird.feature.notification.api.sender.NotificationSender
import net.thunderbird.feature.search.legacy.LocalMessageSearch

/**
 * Starts a long running (application) Thread that will run through commands that require remote mailbox access. This
 * class is used to serialize and prioritize these commands. Each method that will submit a command requires a
 * MessagingListener instance to be provided. It is expected that that listener has also been added as a registered
 * listener using addListener(). When a command is to be executed, if the listener that was provided with the command is
 * no longer registered the command is skipped. The design idea for the above is that when an Activity starts it
 * registers as a listener. When it is paused it removes itself. Thus, any commands that that activity submitted are
 * removed from the queue once the activity is no longer active.
 */
@Suppress(
    "LargeClass",
    "LongMethod",
    "MagicNumber",
    "TooManyFunctions",
    "ForbiddenComment",
    "CyclomaticComplexMethod",
    "TooGenericExceptionCaught",
    "TooGenericExceptionThrown",
    "LoopWithTooManyJumpStatements",
)
open class MessagingController(
    private val logger: Logger,
    private val context: Context,
    private val notificationController: NotificationController,
    private val notificationStrategy: NotificationStrategy,
    private val localStoreProvider: LocalStoreProvider,
    private val backendManager: BackendManager,
    private val preferences: Preferences,
    private val messageStoreManager: MessageStoreManager,
    private val saveMessageDataCreator: SaveMessageDataCreator,
    private val localDeleteOperationDecider: LocalDeleteOperationDecider,
    private val localMessageUidPrefixProvider: LocalMessageUidPrefixProvider,
    controllerExtensions: List<ControllerExtension>,
    private val featureFlagProvider: FeatureFlagProvider,
    private val syncDebugLogger: Logger,
    notificationManager: NotificationManager,
    private val outboxFolderManager: OutboxFolderManager,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MessagingControllerRegistry, MessagingControllerMailChecker {
    companion object {
        val SYNC_FLAGS: EnumSet<Flag> = EnumSet.of(Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED, Flag.FORWARDED)
        const val FOLDER_LIST_STALENESS_THRESHOLD = 30 * 60 * 1000L

        @OptIn(ExperimentalAtomicApi::class)
        private val sequencing = AtomicInt(0)

        @JvmStatic
        fun getInstance(@Suppress("unused") context: Context): MessagingController =
            DI.get(MessagingController::class.java)
    }

    private val controllerThread = thread(name = "MessagingController", start = true) {
        runInBackground()
    }

    private val queuedCommands: BlockingQueue<Command> = PriorityBlockingQueue()
    val listeners: Set<MessagingListener>
        field: MutableSet<MessagingListener> = CopyOnWriteArraySet()
    private val threadPool: ExecutorService = Executors.newCachedThreadPool()
    private val memorizingMessagingListener = MemorizingMessagingListener()
    private val notificationSender: NotificationSender = notificationManager
    private val notificationDismisser: NotificationDismisser = notificationManager

    @Volatile
    private var stopped = false

    init {
        addListener(memorizingMessagingListener)

        initializeControllerExtensions(controllerExtensions)
    }

    private val draftOperations: DraftOperations = DraftOperations(
        messagingController = this,
        messageStoreManager = messageStoreManager,
        saveMessageDataCreator = saveMessageDataCreator,
        localMessageUidPrefixProvider = localMessageUidPrefixProvider,
    )

    private val notificationOperations = NotificationOperations(
        notificationController = notificationController,
        accountManager = preferences,
        messageStoreManager = messageStoreManager,
    )

    private val archiveOperations = ArchiveOperations(
        messagingController = this,
        featureFlagProvider = featureFlagProvider,
    )

    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)

    private fun initializeControllerExtensions(controllerExtensions: List<ControllerExtension>) {
        if (controllerExtensions.isEmpty()) {
            return
        }

        val internals: ControllerInternals = object : ControllerInternals {
            override fun put(
                description: String,
                listener: MessagingListener?,
                runnable: Runnable,
            ) {
                this@MessagingController.put(description, listener, runnable)
            }

            override fun putBackground(
                description: String,
                listener: MessagingListener?,
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
        controllerThread.join(1000L)
    }

    private fun runInBackground() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        while (!stopped) {
            var commandDescription: String? = null
            try {
                val command: Command = queuedCommands.take()

                commandDescription = command.description

                logger.info {
                    "Running command '${command.description}', seq = ${command.sequence} (${
                        if (command.isForegroundPriority) "foreground" else "background"
                    } priority)"
                }

                command.runnable.run()

                logger.info { " Command '${command.description}' completed" }
            } catch (e: Exception) {
                logger.error(throwable = e) { "Error running command '$commandDescription'" }
            }
        }
    }

    private fun put(description: String, listener: MessagingListener?, block: suspend CoroutineScope.() -> Unit) {
        // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
        put(description, listener, runnable = { runBlocking(block = block) })
    }

    private fun put(description: String, listener: MessagingListener?, runnable: Runnable) {
        putCommand(queuedCommands, description, listener, runnable, true)
    }

    fun putBackground(description: String, listener: MessagingListener?, block: suspend CoroutineScope.() -> Unit) {
        // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
        putBackground(description, listener, runnable = { runBlocking(block = block) })
    }

    fun putBackground(description: String, listener: MessagingListener?, runnable: Runnable) {
        putCommand(queuedCommands, description, listener, runnable, false)
    }

    private fun putCommand(
        queue: BlockingQueue<Command>,
        description: String,
        listener: MessagingListener?,
        runnable: Runnable,
        isForeground: Boolean,
    ) {
        var retries = 10
        var e: Exception? = null
        while (retries-- > 0) {
            try {
                val command = Command(
                    description = description,
                    listener = listener,
                    runnable = runnable,
                    isForegroundPriority = isForeground,
                )
                queue.put(command)
                return
            } catch (ie: InterruptedException) {
                SystemClock.sleep(200)
                e = ie
            }
        }
        throw Error(e)
    }

    fun getBackend(account: LegacyAccountDto): Backend = backendManager.getBackend(account.id)

    fun getLocalStoreOrThrow(account: LegacyAccountDto): LocalStore =
        checkNotNull(localStoreProvider.getInstance(account)) {
            "Couldn't get LocalStore for account $account"
        }

    private suspend fun getFolderServerId(account: LegacyAccountDto, folderId: Long): String =
        withContext(ioDispatcher) {
            val messageStore: MessageStore = messageStoreManager.getMessageStore(account)
            checkNotNull(messageStore.getFolderServerId(folderId)) { "Folder not found (ID: $folderId)" }
        }

    private suspend fun getFolderId(account: LegacyAccountDto, folderServerId: String): Long =
        withContext(ioDispatcher) {
            val messageStore: MessageStore = messageStoreManager.getMessageStore(account)
            checkNotNull(messageStore.getFolderId(folderServerId)) {
                "Folder not found (server ID: $folderServerId)"
            }
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

    fun getListeners(listener: MessagingListener?): Set<MessagingListener> {
        if (listener == null) {
            return listeners
        }

        val listeners = HashSet(this.listeners)
        listeners.add(listener)
        return listeners
    }

    fun suppressMessages(account: LegacyAccountDto, messages: List<LocalMessage>) {
        val cache = MessageListCache.getCache(account.id)
        cache.hideMessages(messages)
    }

    private fun unsuppressMessages(account: LegacyAccountDto, messages: List<LocalMessage>) {
        val cache = MessageListCache.getCache(account.id)
        cache.unhideMessages(messages)
    }

    fun isMessageSuppressed(message: LocalMessage): Boolean {
        val messageId = message.databaseId
        val folderId = message.folder.databaseId

        val cache = MessageListCache.getCache(message.folder.accountId)
        return cache.isMessageHidden(messageId, folderId)
    }

    private fun setFlagInCache(
        account: LegacyAccountDto,
        messageIds: List<Long>,
        flag: Flag,
        newState: Boolean,
    ) {
        val cache = MessageListCache.getCache(account.id)
        cache.setFlagForMessages(messageIds, flag, newState)
    }

    private fun removeFlagFromCache(account: LegacyAccountDto, messageIds: List<Long>, flag: Flag) {
        val cache = MessageListCache.getCache(account.id)
        cache.removeFlagForMessages(messageIds, flag)
    }

    private fun setFlagForThreadsInCache(
        account: LegacyAccountDto,
        threadRootIds: List<Long>,
        flag: Flag,
        newState: Boolean,
    ) {
        val cache = MessageListCache.getCache(account.id)
        cache.setValueForThreads(threadRootIds, flag, newState)
    }

    private fun removeFlagForThreadsFromCache(account: LegacyAccountDto, messageIds: List<Long>, flag: Flag) {
        val cache = MessageListCache.getCache(account.id)
        cache.removeFlagForThreads(messageIds, flag)
    }

    fun refreshFolderList(account: LegacyAccountDto) {
        put(description = "refreshFolderList", listener = null) {
            refreshFolderListSynchronous(account)
        }
    }

    fun refreshFolderListBlocking(account: LegacyAccountDto) {
        val latch = CountDownLatch(1)
        putBackground(description = "refreshFolderListBlocking", listener = null) {
            try {
                refreshFolderListSynchronous(account)
            } finally {
                latch.countDown()
            }
        }

        try {
            latch.await()
        } catch (e: Exception) {
            logger.error(throwable = e) { "Interrupted while awaiting latch release" }
        }
    }

    suspend fun refreshFolderListSynchronous(account: LegacyAccountDto) = withContext(ioDispatcher) {
        try {
            if (isAuthenticationProblem(account, true)) {
                logger.debug { "Authentication will fail. Skip refreshing the folder list." }
                handleAuthenticationFailure(account, true)
                return@withContext
            }

            val backend = getBackend(account)
            val folderPathDelimiter = backend.refreshFolderList()
            if (!folderPathDelimiter.isNullOrEmpty() && (folderPathDelimiter != account.folderPathDelimiter)) {
                account.folderPathDelimiter = folderPathDelimiter
            }

            val now = System.currentTimeMillis()
            logger.debug { "Folder list successfully refreshed @ $now" }

            account.lastFolderListRefreshTime = now
            preferences.saveAccount(account)
        } catch (e: Exception) {
            logger.error(throwable = e) { "Could not refresh folder list for account $account" }
            handleException(account, e)
        }
    }

    fun searchRemoteMessages(
        accountId: AccountId,
        folderId: Long,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        listener: MessagingListener?,
    ): Future<*> {
        logger.info { "searchRemoteMessages (acct = $accountId, folderId = $folderId, query = $query)" }

        return threadPool.submit {
            // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
            runBlocking {
                searchRemoteMessagesSynchronous(accountId, folderId, query, requiredFlags, forbiddenFlags, listener)
            }
        }
    }

    @VisibleForTesting
    suspend fun searchRemoteMessagesSynchronous(
        accountId: AccountId,
        folderId: Long,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        listener: MessagingListener?,
    ) = withContext(ioDispatcher) {
        val account = preferences.getById(accountId)

        listener?.remoteSearchStarted(folderId)

        var extraResults: List<String> = emptyList()
        try {
            val localStore = localStoreProvider.getInstance(account!!)

            val localFolder = localStore.getFolder(folderId)
            if (!localFolder.exists()) {
                throw MessagingException("Folder not found")
            }

            localFolder.open()
            val folderServerId = localFolder.serverId

            val backend = getBackend(account)

            val performFullTextSearch = account.isRemoteSearchFullText
            var messageServerIds: List<String> = backend.search(
                folderServerId,
                query,
                requiredFlags,
                forbiddenFlags,
                performFullTextSearch,
            )

            logger.info { "Remote search got ${messageServerIds.size} results" }

            // There's no need to fetch messages already completely downloaded
            messageServerIds = localFolder.extractNewMessages(messageServerIds)

            listener?.remoteSearchServerQueryComplete(
                folderId,
                messageServerIds.size,
                account.remoteSearchNumResults,
            )

            val resultLimit = account.remoteSearchNumResults
            if (resultLimit > 0 && messageServerIds.size > resultLimit) {
                extraResults = messageServerIds.subList(resultLimit, messageServerIds.size)
                messageServerIds = messageServerIds.subList(0, resultLimit)
            }

            loadSearchResultsSynchronous(account, messageServerIds, localFolder)
        } catch (e: Exception) {
            if (Thread.currentThread().isInterrupted) {
                logger.info(throwable = e) { "Caught exception on aborted remote search; safe to ignore." }
            } else {
                logger.error(throwable = e) { "Could not complete remote search" }
                listener?.remoteSearchFailed(null, e.message)
                logger.error(throwable = e) { "Remote search failed for account $accountId, folder $folderId" }
            }
        } finally {
            listener?.remoteSearchFinished(folderId, 0, account!!.remoteSearchNumResults, extraResults)
        }
    }

    fun loadSearchResults(
        account: LegacyAccountDto,
        folderId: Long,
        messageServerIds: List<String>,
        listener: MessagingListener?,
    ) {
        threadPool.execute {
            listener?.enableProgressIndicator(true)
            try {
                val localStore = localStoreProvider.getInstance(account)
                val localFolder = localStore.getFolder(folderId)
                if (!localFolder.exists()) {
                    throw MessagingException("Folder not found")
                }

                localFolder.open()

                // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
                runBlocking {
                    loadSearchResultsSynchronous(account, messageServerIds, localFolder)
                }
            } catch (e: MessagingException) {
                logger.error(throwable = e) { "Exception in loadSearchResults" }
            } finally {
                listener?.enableProgressIndicator(false)
            }
        }
    }

    @Throws(MessagingException::class)
    private suspend fun loadSearchResultsSynchronous(
        account: LegacyAccountDto,
        messageServerIds: List<String>,
        localFolder: LocalFolder,
    ) = withContext(ioDispatcher) {
        val backend = getBackend(account)
        val folderServerId = localFolder.serverId

        for (messageServerId in messageServerIds) {
            val localMessage = localFolder.getMessage(messageServerId)

            if (localMessage == null) {
                backend.downloadMessageStructure(folderServerId, messageServerId)
            }
        }
    }

    fun loadMoreMessages(account: LegacyAccountDto, folderId: Long) {
        putBackground(description = "loadMoreMessages", listener = null) {
            loadMoreMessagesSynchronous(
                account,
                folderId,
            )
        }
    }

    suspend fun loadMoreMessagesSynchronous(account: LegacyAccountDto, folderId: Long) = withContext(ioDispatcher) {
        val messageStore: MessageStore = messageStoreManager.getMessageStore(account)
        val visibleLimit = messageStore.getFolder<Int?>(folderId, FolderDetailsAccessor::visibleLimit)
        if (visibleLimit == null) {
            logger.verbose { "loadMoreMessages($account, $folderId): Folder not found" }
            return@withContext
        }

        if (visibleLimit > 0) {
            val newVisibleLimit = visibleLimit + account.displayCount
            messageStore.setVisibleLimit(folderId, newVisibleLimit)
        }

        synchronizeMailboxSynchronous(account, folderId, false, null, NotificationState())
    }

    /**
     * Start background synchronization of the specified folder.
     */
    fun synchronizeMailbox(
        account: LegacyAccountDto,
        folderId: Long,
        notify: Boolean,
        listener: MessagingListener?,
    ) {
        putBackground(description = "synchronizeMailbox", listener = listener) {
            synchronizeMailboxSynchronous(
                account,
                folderId,
                notify,
                listener,
                NotificationState(),
            )
        }
    }

    suspend fun synchronizeMailboxBlocking(account: LegacyAccountDto, folderServerId: String) =
        withContext(ioDispatcher) {
            val folderId = getFolderId(account, folderServerId)

            val latch = CountDownLatch(1)
            putBackground(description = "synchronizeMailbox", listener = null) {
                try {
                    synchronizeMailboxSynchronous(account, folderId, true, null, NotificationState())
                } finally {
                    latch.countDown()
                }
            }

            try {
                latch.await()
            } catch (e: Exception) {
                logger.error(throwable = e) { "Interrupted while awaiting latch release" }
            }
        }

    private suspend fun synchronizeMailboxSynchronous(
        account: LegacyAccountDto,
        folderId: Long,
        notify: Boolean,
        listener: MessagingListener?,
        notificationState: NotificationState,
    ) = withContext(ioDispatcher) {
        refreshFolderListIfStale(account)

        val backend = getBackend(account)
        syncFolder(account, folderId, notify, listener, backend, notificationState)
    }

    private suspend fun refreshFolderListIfStale(account: LegacyAccountDto) {
        val lastFolderListRefresh = account.lastFolderListRefreshTime
        val now = System.currentTimeMillis()

        if (lastFolderListRefresh > now || lastFolderListRefresh + FOLDER_LIST_STALENESS_THRESHOLD <= now) {
            logger.debug { "Last folder list refresh @ $lastFolderListRefresh. Refreshing now…" }
            refreshFolderListSynchronous(account)
        } else {
            logger.debug { "Last folder list refresh @ $lastFolderListRefresh. Not refreshing now." }
        }
    }

    private suspend fun syncFolder(
        account: LegacyAccountDto,
        folderId: Long,
        notify: Boolean,
        listener: MessagingListener?,
        backend: Backend,
        notificationState: NotificationState,
    ) = withContext(ioDispatcher) {
        if (isAuthenticationProblem(account, true)) {
            logger.debug { "Authentication will fail. Skip synchronizing folder $folderId." }
            handleAuthenticationFailure(account, true)
            return@withContext
        }

        var commandException: Exception? = null
        try {
            processPendingCommandsSynchronous(account)
        } catch (e: Exception) {
            logger.error(throwable = e) { "Failure processing command, but allow message sync attempt" }
            commandException = e
        }

        val localFolder: LocalFolder
        try {
            val localStore = localStoreProvider.getInstance(account)
            localFolder = localStore.getFolder(folderId)
            localFolder.open()
        } catch (e: MessagingException) {
            syncDebugLogger.error("MessagingException", null) { e.message ?: "Unknown issue" }
            logger.error(throwable = e) { "syncFolder: Couldn't load local folder $folderId" }
            return@withContext
        }

        // We can't sync local folders
        if (localFolder.isLocalOnly) {
            return@withContext
        }

        val suppressNotifications: Boolean
        if (notify) {
            val messageStore: MessageStore = messageStoreManager.getMessageStore(account)
            val lastChecked = messageStore.getFolder(folderId, FolderDetailsAccessor::lastChecked)
            suppressNotifications = lastChecked == null
        } else {
            suppressNotifications = true
        }

        val folderServerId = localFolder.serverId
        val syncConfig: SyncConfig = createSyncConfig(account)
        val syncListener = ControllerSyncListener(account, listener, suppressNotifications, notificationState)

        backend.sync(folderServerId, syncConfig, syncListener)

        if (commandException != null && !syncListener.syncFailed) {
            val rootMessage = commandException.rootCauseMessage
            syncDebugLogger.error("MessagingException", null) { rootMessage ?: "Unknown issue" }
            logger.error { "Root cause failure in $account:$folderServerId was '$rootMessage'" }
            updateFolderStatus(account, folderId, rootMessage)
            listener?.synchronizeMailboxFailed(account, folderId, rootMessage)
        }
    }

    private fun createSyncConfig(account: LegacyAccountDto): SyncConfig = SyncConfig(
        expungePolicy = account.expungePolicy.toBackendExpungePolicy(),
        earliestPollDate = account.earliestPollDate,
        syncRemoteDeletions = account.isSyncRemoteDeletions,
        maximumAutoDownloadMessageSize = account.maximumAutoDownloadMessageSize,
        defaultVisibleLimit = AccountDefaultsProvider.DEFAULT_VISIBLE_LIMIT,
        syncFlags = SYNC_FLAGS,
    )

    private suspend fun updateFolderStatus(account: LegacyAccountDto, folderId: Long, status: String?) =
        withContext(ioDispatcher) {
            val messageStore: MessageStore = messageStoreManager.getMessageStore(account)
            messageStore.setStatus(folderId, status)
        }

    fun getMessageStore(account: LegacyAccountDto): MessageStore = messageStoreManager.getMessageStore(account)

    suspend fun handleAuthenticationFailure(account: LegacyAccountDto, incoming: Boolean) {
        if (account.shouldMigrateToOAuth) {
            migrateAccountToOAuth(account)
        }

        if (featureFlagProvider.provide(GeneratedFeatureFlagKey.DISPLAY_IN_APP_NOTIFICATIONS).isEnabled()) {
            logger.debug { "handleAuthenticationFailure: sending in-app notification" }
            val notification: AuthenticationErrorNotification = createAuthenticationErrorNotification(account, incoming)

            notificationSender
                .send(notification)
                .onEach { outcome -> logger.verbose { "notificationSender outcome = $outcome" } }
                .launchIn(scope = scope)
        }

        if (featureFlagProvider
                .provide(GeneratedFeatureFlagKey.USE_NOTIFICATION_SENDER_FOR_SYSTEM_NOTIFICATIONS)
                .isDisabled()
        ) {
            logger.debug { "handleAuthenticationFailure: sending system notification via old notification controller" }
            notificationController.showAuthenticationErrorNotification(account, incoming)
        }
    }

    private suspend fun createAuthenticationErrorNotification(
        account: LegacyAccountDto,
        incoming: Boolean,
    ): AuthenticationErrorNotification = AuthenticationErrorNotification(
        accountId = account.id,
        accountDisplayName = account.displayName,
        accountNumber = account.accountNumber,
        isIncomingServerError = incoming,
    )

    private suspend fun migrateAccountToOAuth(account: LegacyAccountDto) = withContext(ioDispatcher) {
        account.incomingServerSettings = account.incomingServerSettings.newAuthenticationType(AuthType.XOAUTH2)
        account.outgoingServerSettings = account.outgoingServerSettings.newAuthenticationType(AuthType.XOAUTH2)
        account.shouldMigrateToOAuth = false

        preferences.saveAccount(account)
    }

    suspend fun handleException(account: LegacyAccountDto, exception: Exception) {
        if (exception is AuthenticationFailedException) {
            handleAuthenticationFailure(account, true)
        } else {
            notifyUserIfCertificateProblem(account, exception, true)
        }
    }

    suspend fun queuePendingCommand(account: LegacyAccountDto, command: PendingCommand) = withContext(ioDispatcher) {
        try {
            val localStore = localStoreProvider.getInstance(account)
            localStore.addPendingCommand(command)
        } catch (e: Exception) {
            throw RuntimeException("Unable to enqueue pending command", e)
        }
    }

    fun processPendingCommands(account: LegacyAccountDto) {
        putBackground(description = "processPendingCommands", listener = null) {
            try {
                processPendingCommandsSynchronous(account)
            } catch (me: MessagingException) {
                logger.error(throwable = me) { "processPendingCommands" }

                /*
                * Ignore any exceptions from the commands. Commands will be processed
                * on the next round.
                */
            }
        }
    }

    @Throws(MessagingException::class)
    suspend fun processPendingCommandsSynchronous(account: LegacyAccountDto) = withContext(ioDispatcher) {
        val localStore = localStoreProvider.getInstance(account)
        val commands = localStore.pendingCommands

        var processingCommand: PendingCommand? = null
        try {
            for (command in commands) {
                processingCommand = command
                val commandName = command.getCommandName()
                logger.debug { "Processing pending command '$commandName'" }

                /*
                 * We specifically do not catch any exceptions here. If a command fails it is
                 * most likely due to a server or IO error and it must be retried before any
                 * other command processes. This maintains the order of the commands.
                 */
                try {
                    command.execute(this@MessagingController, account)

                    localStore.removePendingCommand(command)

                    logger.debug { "Done processing pending command '$commandName'" }
                } catch (me: MessagingException) {
                    if (me.isPermanentFailure) {
                        logger.error(throwable = me) {
                            "Failure of command '$commandName' was permanent, removing command from queue"
                        }
                        localStore.removePendingCommand(processingCommand)
                    } else {
                        throw me
                    }
                } catch (e: Exception) {
                    logger.error(throwable = e) {
                        "Unexpected exception with command '$commandName', removing command from queue"
                    }
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
            notifyUserIfCertificateProblem(account, me, true)
            logger.error(throwable = me) { "Could not process command '$processingCommand'" }
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
    suspend fun processPendingAppend(command: PendingAppend, account: LegacyAccountDto) = withContext(ioDispatcher) {
        val localStore = localStoreProvider.getInstance(account)
        val folderId = command.folderId
        val localFolder = localStore.getFolder(folderId)
        localFolder.open()

        val folderServerId = localFolder.serverId
        val uid = command.uid

        val localMessage = localFolder.getMessage(uid) ?: return@withContext

        if (!localMessage.uid.startsWith(localMessageUidPrefixProvider.get())) {
            //FIXME: This should never happen. Throw in debug builds.
            return@withContext
        }

        val backend = getBackend(account)

        if (localMessage.isSet(Flag.X_REMOTE_COPY_STARTED)) {
            logger.warn {
                "Local message with uid ${localMessage.uid} has flag ${Flag.X_REMOTE_COPY_STARTED} already set, " +
                    "checking for remote message with same message id"
            }

            val messageServerId = backend.findByMessageId(folderServerId, localMessage.getMessageId())
            if (messageServerId != null) {
                logger.warn {
                    "Local message has flag ${Flag.X_REMOTE_COPY_STARTED} already set, and there is a remote " +
                        "message with uid $messageServerId, assuming message was already copied and aborting this copy"
                }
                val oldUid = localMessage.uid
                localMessage.setUid(messageServerId)
                localFolder.changeUid(localMessage)

                for (l in listeners) {
                    l.messageUidChanged(account, folderId, oldUid, localMessage.uid)
                }

                return@withContext
            } else {
                logger.warn { "No remote message with message-id found, proceeding with append" }
            }
        }

        /*
         * If the message does not exist remotely we just upload it and then
         * update our local copy with the new uid.
         */
        val fp = FetchProfile()
        fp.add(FetchProfile.Item.BODY)
        localFolder.fetch(Collections.singletonList(localMessage), fp, null)
        val oldUid = localMessage.uid
        localMessage.setFlag(Flag.X_REMOTE_COPY_STARTED, true)

        val messageServerId = backend.uploadMessage(folderServerId, localMessage)

        if (messageServerId == null) {
            // We didn't get the server UID of the uploaded message. Remove the local message now. The uploaded
            // version will be downloaded during the next sync.
            localFolder.destroyMessages(Collections.singletonList(localMessage))
        } else {
            localMessage.setUid(messageServerId)
            localFolder.changeUid(localMessage)

            for (l in listeners) {
                l.messageUidChanged(account, folderId, oldUid, localMessage.uid)
            }
        }
    }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use processPendingAppendBlocking instead",
        replaceWith = ReplaceWith("processPendingAppend(command, account)"),
    )
    @Throws(MessagingException::class)
    fun processPendingAppendBlocking(command: PendingAppend, account: LegacyAccountDto) = runBlocking {
        processPendingAppend(command, account)
    }

    suspend fun processPendingReplace(pendingReplace: PendingReplace, account: LegacyAccountDto) =
        withContext(ioDispatcher) {
            draftOperations.processPendingReplace(pendingReplace, account)
        }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use processPendingReplace instead",
        replaceWith = ReplaceWith("processPendingReplace(pendingReplace, account)"),
    )
    fun processPendingReplaceBlocking(pendingReplace: PendingReplace, account: LegacyAccountDto) = runBlocking {
        processPendingReplace(pendingReplace, account)
    }

    private suspend fun queueMoveOrCopy(
        account: LegacyAccountDto,
        srcFolderId: Long,
        destFolderId: Long,
        operation: MoveOrCopyFlavor,
        uidMap: Map<String, String>,
    ) {
        val command = when (operation) {
            MoveOrCopyFlavor.MOVE -> PendingMoveOrCopy.create(srcFolderId, destFolderId, false, uidMap)
            MoveOrCopyFlavor.COPY -> PendingMoveOrCopy.create(srcFolderId, destFolderId, true, uidMap)
            MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ -> PendingMoveAndMarkAsRead.create(srcFolderId, destFolderId, uidMap)
        }
        queuePendingCommand(account, command)
    }

    @Throws(MessagingException::class)
    suspend fun processPendingMoveOrCopy(command: PendingMoveOrCopy, account: LegacyAccountDto) {
        val srcFolder = command.srcFolderId
        val destFolder = command.destFolderId
        val operation = if (command.isCopy) MoveOrCopyFlavor.COPY else MoveOrCopyFlavor.MOVE

        val newUidMap = command.newUidMap
        val uids = newUidMap?.keys?.toList() ?: command.uids

        processPendingMoveOrCopy(
            account = account,
            srcFolderId = srcFolder,
            destFolderId = destFolder,
            uids = uids,
            operation = operation,
            newUidMap = newUidMap,
        )
    }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use processPendingMoveOrCopyBlocking instead",
        replaceWith = ReplaceWith("processPendingMoveOrCopy(command, account)"),
    )
    @Throws(MessagingException::class)
    fun processPendingMoveOrCopyBlocking(command: PendingMoveOrCopy, account: LegacyAccountDto) = runBlocking {
        processPendingMoveOrCopy(command, account)
    }

    @Throws(MessagingException::class)
    suspend fun processPendingMoveAndRead(command: PendingMoveAndMarkAsRead, account: LegacyAccountDto) {
        val srcFolder = command.srcFolderId
        val destFolder = command.destFolderId
        val newUidMap = command.newUidMap
        val uids = newUidMap.keys.toList()

        processPendingMoveOrCopy(
            account = account,
            srcFolderId = srcFolder,
            destFolderId = destFolder,
            uids = uids,
            operation = MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ,
            newUidMap = newUidMap,
        )
    }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use processPendingMoveAndRead instead",
        replaceWith = ReplaceWith("processPendingMoveAndRead(command, account)"),
    )
    @Throws(MessagingException::class)
    fun processPendingMoveAndReadBlocking(command: PendingMoveAndMarkAsRead, account: LegacyAccountDto) = runBlocking {
        processPendingMoveAndRead(command, account)
    }

    @VisibleForTesting
    @Throws(MessagingException::class)
    suspend fun processPendingMoveOrCopy(
        account: LegacyAccountDto,
        srcFolderId: Long,
        destFolderId: Long,
        uids: List<String>,
        operation: MoveOrCopyFlavor,
        newUidMap: Map<String, String>,
    ) = withContext(ioDispatcher) {
        val localStore = localStoreProvider.getInstance(account)

        val localSourceFolder = localStore.getFolder(srcFolderId)
        localSourceFolder.open()
        val srcFolderServerId = localSourceFolder.serverId

        val localDestFolder = localStore.getFolder(destFolderId)
        localDestFolder.open()
        val destFolderServerId = localDestFolder.serverId

        val backend = getBackend(account)

        var remoteUidMap = when (operation) {
            MoveOrCopyFlavor.COPY -> backend.copyMessages(
                sourceFolderServerId = srcFolderServerId,
                targetFolderServerId = destFolderServerId,
                messageServerIds = uids,
            )

            MoveOrCopyFlavor.MOVE -> backend.moveMessages(
                sourceFolderServerId = srcFolderServerId,
                targetFolderServerId = destFolderServerId,
                messageServerIds = uids,
            )

            MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ -> backend.moveMessagesAndMarkAsRead(
                sourceFolderServerId = srcFolderServerId,
                targetFolderServerId = destFolderServerId,
                messageServerIds = uids,
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
            // If null, Local message no longer exists
            val localMessage = localDestFolder.getMessage(localUid) ?: continue

            if (newUid != null) {
                // Update local message with new server ID
                localMessage.setUid(newUid)
                localDestFolder.changeUid(localMessage)
                for (l in listeners) {
                    l.messageUidChanged(account, destFolderId, localUid, newUid)
                }
            } else {
                // New server ID wasn't provided. Remove local message.
                localMessage.destroy()
            }
        }
    }

    @Throws(MessagingException::class)
    suspend fun destroyPlaceholderMessages(localFolder: LocalFolder, uids: List<String>) = withContext(ioDispatcher) {
        for (uid in uids) {
            val placeholderMessage = localFolder.getMessage(uid) ?: continue

            if (placeholderMessage.isSet(Flag.DELETED)) {
                placeholderMessage.destroy()
            } else {
                logger.warn {
                    "Expected local message $uid in folder ${localFolder.serverId} to be a placeholder, but " +
                        "DELETE flag wasn't set"
                }

                if (BuildConfig.DEBUG) {
                    throw AssertionError("Placeholder message must have the DELETED flag set")
                }
            }
        }
    }

    private suspend fun queueSetFlag(
        account: LegacyAccountDto,
        folderId: Long,
        newState: Boolean,
        flag: Flag?,
        uids: List<String>,
    ) {
        val command: PendingCommand = PendingSetFlag.create(folderId, newState, flag, uids)
        queuePendingCommand(account, command)
    }

    /**
     * Processes a pending mark read or unread command.
     */
    @Throws(MessagingException::class)
    suspend fun processPendingSetFlag(command: PendingSetFlag, account: LegacyAccountDto) = withContext(ioDispatcher) {
        val backend = getBackend(account)
        val folderServerId = getFolderServerId(account, command.folderId)
        backend.setFlag(folderServerId, command.uids, command.flag, command.newState)
    }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use processPendingSetFlag instead",
        replaceWith = ReplaceWith("processPendingSetFlag(command, account)"),
    )
    @Throws(MessagingException::class)
    fun processPendingSetFlagBlocking(command: PendingSetFlag, account: LegacyAccountDto) = runBlocking {
        processPendingSetFlag(command, account)
    }

    private suspend fun queueDelete(account: LegacyAccountDto, folderId: Long, uids: List<String>) {
        val command: PendingCommand = PendingDelete.create(folderId, uids)
        queuePendingCommand(account, command)
    }

    @Throws(MessagingException::class)
    suspend fun processPendingDelete(command: PendingDelete, account: LegacyAccountDto) = withContext(ioDispatcher) {
        val folderId = command.folderId
        val uids = command.uids

        val backend = getBackend(account)
        val folderServerId = getFolderServerId(account, folderId)
        backend.deleteMessages(folderServerId, uids)

        val localStore = localStoreProvider.getInstance(account)
        val localFolder = localStore.getFolder(folderId)
        localFolder.open()
        destroyPlaceholderMessages(localFolder, uids)
    }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use processPendingDeleteBlocking instead",
        replaceWith = ReplaceWith("processPendingDelete(command, account)"),
    )
    fun processPendingDeleteBlocking(command: PendingDelete, account: LegacyAccountDto) =
        runBlocking { processPendingDelete(command, account) }

    private suspend fun queueExpunge(account: LegacyAccountDto, folderId: Long) {
        val command: PendingCommand = PendingExpunge.create(folderId)
        queuePendingCommand(account, command)
    }

    @Throws(MessagingException::class)
    suspend fun processPendingExpunge(command: PendingExpunge, account: LegacyAccountDto) = withContext(ioDispatcher) {
        val backend = getBackend(account)
        val folderServerId = getFolderServerId(account, command.folderId)
        backend.expunge(folderServerId)
    }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    fun processPendingExpungeBlocking(command: PendingExpunge, account: LegacyAccountDto) = runBlocking {
        processPendingExpunge(command, account)
    }

    @Throws(MessagingException::class)
    suspend fun processPendingMarkAllAsRead(command: PendingMarkAllAsRead, account: LegacyAccountDto) =
        withContext(ioDispatcher) {
            val folderId = command.folderId
            val localStore = localStoreProvider.getInstance(account)
            val localFolder = localStore.getFolder(folderId)

            localFolder.open()
            val folderServerId = localFolder.serverId

            logger.info { "Marking all messages in $account:$folderServerId as read" }

            // TODO: Make this one database UPDATE operation
            val messages = localFolder.getMessages(false)
            for (message in messages) {
                if (!message.isSet(Flag.SEEN)) {
                    message.setFlag(Flag.SEEN, true)
                }
            }

            for (l in listeners) {
                l.folderStatusChanged(account, folderId)
            }

            val backend = getBackend(account)
            if (backend.supportsFlags) {
                backend.markAllAsRead(folderServerId)
            }
        }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use processPendingMarkAllAsReadBlocking instead",
        replaceWith = ReplaceWith("processPendingMarkAllAsRead(command, account)"),
    )
    @Throws(MessagingException::class)
    fun processPendingMarkAllAsReadBlocking(command: PendingMarkAllAsRead, account: LegacyAccountDto) = runBlocking {
        processPendingMarkAllAsRead(command, account)
    }

    suspend fun markAllMessagesRead(account: LegacyAccountDto, folderId: Long) {
        val command: PendingCommand = PendingMarkAllAsRead.create(folderId)
        queuePendingCommand(account, command)
        processPendingCommands(account)
    }

    fun setFlag(account: LegacyAccountDto, messageIds: List<Long>, flag: Flag, newState: Boolean) {
        setFlagInCache(account, messageIds, flag, newState)

        putBackground(description = "setFlag", listener = null) {
            setFlagSynchronous(account, messageIds, flag, newState, false)
        }
    }

    fun setFlagForThreads(account: LegacyAccountDto, threadRootIds: List<Long>, flag: Flag, newState: Boolean) {
        setFlagForThreadsInCache(account, threadRootIds, flag, newState)

        putBackground(description = "setFlagForThreads", listener = null) {
            setFlagSynchronous(account, threadRootIds, flag, newState, true)
        }
    }

    @Throws(MessagingException::class)
    private suspend fun setFlagSynchronous(
        account: LegacyAccountDto,
        ids: List<Long>,
        flag: Flag,
        newState: Boolean,
        threadedList: Boolean,
    ) = withContext(ioDispatcher) {
        val localStore = try {
            localStoreProvider.getInstance(account)
        } catch (e: MessagingException) {
            logger.error(throwable = e) { "Couldn't get LocalStore instance" }
            return@withContext
        }

        // Update affected messages in the database. This should be as fast as possible so the UI
        // can be updated with the new state.
        try {
            if (threadedList) {
                localStore.setFlagForThreads(ids, flag, newState)
                removeFlagForThreadsFromCache(account, ids, flag)
            } else {
                localStore.setFlag(ids, flag, newState)
                removeFlagFromCache(account, ids, flag)
            }
        } catch (e: MessagingException) {
            logger.error(throwable = e) { "Couldn't set flags in local database" }
        }

        // Read folder ID and UID of messages from the database
        val folderMap: MutableMap<Long, List<String>>
        try {
            folderMap = localStore.getFolderIdsAndUids(ids, threadedList)
        } catch (e: MessagingException) {
            logger.error(throwable = e) { "Couldn't get folder name and UID of messages" }
            return@withContext
        }

        val accountSupportsFlags: Boolean = supportsFlags(account)

        // Loop over all folders
        for ((folderId, uids) in folderMap) {
            // Notify listeners of changed folder status
            for (l in listeners) {
                l.folderStatusChanged(account, folderId)
            }

            if (flag == Flag.SEEN && newState) {
                cancelNotificationsForMessages(account, folderId, uids)
            }

            if (accountSupportsFlags) {
                val localFolder = localStore.getFolder(folderId)
                try {
                    localFolder.open()
                    if (!localFolder.isLocalOnly) {
                        // Send flag change to server
                        queueSetFlag(account, folderId, newState, flag, uids)
                        processPendingCommands(account)
                    }
                } catch (e: MessagingException) {
                    logger.error(throwable = e) { "Couldn't open folder. Account: $account, folder ID: $folderId" }
                }
            }
        }
    }

    private fun cancelNotificationsForMessages(account: LegacyAccountDto, folderId: Long, uids: List<String>) {
        for (uid in uids) {
            val messageReference = MessageReference(account.id, folderId, uid)
            notificationController.removeNewMailNotification(account, messageReference)
        }
    }

    /**
     * Set or remove a flag for a set of messages in a specific folder.
     *
     *
     * The [com.fsck.k9.mail.Message] objects passed in are updated to reflect the new flag state.
     *
     */
    @Throws(RuntimeException::class)
    suspend fun setFlag(
        account: LegacyAccountDto,
        folderId: Long,
        messages: List<LocalMessage>,
        flag: Flag?,
        newState: Boolean,
    ) {
        // TODO: Put this into the background, but right now some callers depend on the message
        //       objects being modified right after this method returns.
        try {
            val localFolder = withContext(ioDispatcher) {
                val localStore = localStoreProvider.getInstance(account)
                val localFolder = localStore.getFolder(folderId)
                localFolder.open()

                // Update the messages in the local store
                localFolder.setFlags(messages, setOf(flag), newState)
                localFolder
            }

            for (l in listeners) {
                l.folderStatusChanged(account, folderId)
            }

            // Handle the remote side
            if (supportsFlags(account) && !localFolder.isLocalOnly) {
                val uids = getUidsFromMessages(messages)
                queueSetFlag(account, folderId, newState, flag, uids)
                processPendingCommands(account)
            }
        } catch (me: MessagingException) {
            throw RuntimeException(me)
        }
    }

    /**
     * Set or remove a flag for a message referenced by message UID.
     */
    @Throws(RuntimeException::class)
    suspend fun setFlag(account: LegacyAccountDto, folderId: Long, uid: String?, flag: Flag?, newState: Boolean) =
        withContext(ioDispatcher) {
            try {
                val localStore = localStoreProvider.getInstance(account)
                val localFolder = localStore.getFolder(folderId)
                localFolder.open()

                val message = localFolder.getMessage(uid)
                if (message != null) {
                    setFlag(account, folderId, listOf(message), flag, newState)
                }
            } catch (me: MessagingException) {
                throw RuntimeException(me)
            }
        }

    fun setFlagBlocking(account: LegacyAccountDto, folderId: Long, uid: String?, flag: Flag?, newState: Boolean) {
        // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
        runBlocking { setFlag(account, folderId, uid, flag, newState) }
    }

    fun loadMessageRemotePartial(
        account: LegacyAccountDto, folderId: Long, uid: String,
        listener: MessagingListener?,
    ) {
        put(description = "loadMessageRemotePartial", listener = listener) {
            loadMessageRemoteSynchronous(account, folderId, uid, listener, true)
        }
    }

    //TODO: Fix the callback mess. See GH-782
    fun loadMessageRemote(account: LegacyAccountDto, folderId: Long, uid: String, listener: MessagingListener?) {
        put(description = "loadMessageRemote", listener = listener) {
            loadMessageRemoteSynchronous(account, folderId, uid, listener, false)
        }
    }

    private suspend fun loadMessageRemoteSynchronous(
        account: LegacyAccountDto,
        folderId: Long,
        messageServerId: String,
        listener: MessagingListener?,
        loadPartialFromSearch: Boolean,
    ) = withContext(ioDispatcher) {
        try {
            require(!messageServerId.startsWith(localMessageUidPrefixProvider.get())) {
                "Must not be called with a local UID"
            }

            val messageStore: MessageStore = messageStoreManager.getMessageStore(account)

            val folderServerId: String = checkNotNull(messageStore.getFolderServerId(folderId)) {
                "Folder not found (ID: $folderId)"
            }

            val backend = getBackend(account)

            if (loadPartialFromSearch) {
                val syncConfig = createSyncConfig(account)
                backend.downloadMessage(syncConfig, folderServerId, messageServerId)
            } else {
                backend.downloadCompleteMessage(folderServerId, messageServerId)
            }

            for (l in getListeners(listener)) {
                l.loadMessageRemoteFinished(account, folderId, messageServerId)
            }
        } catch (e: Exception) {
            for (l in getListeners(listener)) {
                l.loadMessageRemoteFailed(account, folderId, messageServerId, e)
            }
            notifyUserIfCertificateProblem(account, e, true)
            logger.error(throwable = e) { "Error while loading remote message" }
            syncDebugLogger.error("MessagingException", null) { "Error while loading remote message" }
        }
    }

    @Throws(MessagingException::class)
    suspend fun loadMessage(account: LegacyAccountDto, folderId: Long, uid: String?): LocalMessage =
        withContext(ioDispatcher) {
            val localStore = localStoreProvider.getInstance(account)
            val localFolder = localStore.getFolder(folderId)
            localFolder.open()

            val message = localFolder.getMessage(uid)
            require(message != null && message.databaseId != 0L) {
                val folderName = localFolder.name
                "Message not found: folder=$folderName, uid=$uid"
            }

            val fp = FetchProfile()
            fp.add(FetchProfile.Item.BODY)
            localFolder.fetch(listOf(message), fp, null)

            message
        }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use loadMessage instead",
        replaceWith = ReplaceWith("loadMessage(account, folderId, uid)"),
    )
    @Throws(MessagingException::class)
    fun loadMessageBlocking(account: LegacyAccountDto, folderId: Long, uid: String?): LocalMessage = runBlocking {
        loadMessage(account, folderId, uid)
    }

    @Throws(MessagingException::class)
    suspend fun loadMessageMetadata(account: LegacyAccountDto, folderId: Long, uid: String?): LocalMessage =
        withContext(ioDispatcher) {
            val localStore = localStoreProvider.getInstance(account)
            val localFolder = localStore.getFolder(folderId)
            localFolder.open()

            val message = localFolder.getMessage(uid)
            require(message != null && message.databaseId != 0L) {
                val folderName = localFolder.name
                "Message not found: folder=$folderName, uid=$uid"
            }

            val fp = FetchProfile()
            fp.add(FetchProfile.Item.ENVELOPE)
            localFolder.fetch(listOf(message), fp, null)
            message
        }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use loadMessageMetadata instead",
        replaceWith = ReplaceWith("loadMessageMetadata(account, folderId, uid)"),
    )
    @Throws(MessagingException::class)
    fun loadMessageMetadataBlocking(account: LegacyAccountDto, folderId: Long, uid: String?): LocalMessage =
        runBlocking {
            loadMessageMetadata(account, folderId, uid)
        }

    fun markMessageAsOpened(account: LegacyAccountDto, message: LocalMessage) {
        threadPool.execute {
            notificationController.removeNewMailNotification(
                account = account,
                messageReference = message.makeMessageReference(),
            )
        }

        if (message.isSet(Flag.SEEN)) {
            // Nothing to do if the message is already marked as read
            return
        }

        val markMessageAsRead = account.isMarkMessageAsReadOnView
        if (markMessageAsRead) {
            // Mark the message itself as read right away
            try {
                message.setFlagInternal(Flag.SEEN, true)
            } catch (e: MessagingException) {
                logger.error(throwable = e) { "Error while marking message as read" }
            }

            // Also mark the message as read in the cache
            val messageIds = listOf(message.databaseId)
            setFlagInCache(account = account, messageIds = messageIds, flag = Flag.SEEN, newState = true)
        }

        putBackground(description = "markMessageAsOpened", listener = null) {
            markMessageAsOpenedBlocking(account, message, markMessageAsRead)
        }
    }

    private suspend fun markMessageAsOpenedBlocking(
        account: LegacyAccountDto,
        message: LocalMessage,
        markMessageAsRead: Boolean,
    ) {
        if (markMessageAsRead) {
            markMessageAsRead(account, message)
        } else {
            // Marking a message as read will automatically mark it as "not new". But if we don't mark the message
            // as read on opening, we have to manually mark it as "not new".
            markMessageAsNotNew(account, message)
        }
    }

    private suspend fun markMessageAsRead(account: LegacyAccountDto, message: LocalMessage) {
        val messageIds = listOf(message.databaseId)
        setFlagSynchronous(account = account, ids = messageIds, flag = Flag.SEEN, newState = true, threadedList = false)
    }

    private suspend fun markMessageAsNotNew(account: LegacyAccountDto, message: LocalMessage) =
        withContext(ioDispatcher) {
            val messageStore: MessageStore = messageStoreManager.getMessageStore(account)
            val folderId = message.folder.databaseId
            val messageServerId = message.uid
            messageStore.setNewMessageState(folderId, messageServerId, false)
        }

    fun clearNewMessages(account: LegacyAccountDto) {
        put(description = "clearNewMessages", listener = null) { clearNewMessagesBlocking(account) }
    }

    private suspend fun clearNewMessagesBlocking(account: LegacyAccountDto) = withContext(ioDispatcher) {
        val messageStore: MessageStore = messageStoreManager.getMessageStore(account)
        messageStore.clearNewMessageState()
    }

    fun loadAttachment(account: LegacyAccountDto, message: LocalMessage, part: Part, listener: MessagingListener?) {
        put(description = "loadAttachment", listener = listener) {
            try {
                val folderServerId = message.folder.serverId

                val localStore = localStoreProvider.getInstance(account)
                val localFolder = localStore.getFolder(folderServerId)

                val bodyFactory = ProgressBodyFactory { progress ->
                    for (listener in listeners) {
                        listener.updateProgress(progress)
                    }
                }

                val backend = getBackend(account)
                backend.fetchPart(folderServerId, message.uid, part, bodyFactory)

                localFolder.addPartToMessage(message, part)

                for (l in getListeners(listener)) {
                    l.loadAttachmentFinished(account, message, part)
                }
            } catch (me: MessagingException) {
                logger.verbose(throwable = me) { "Exception loading attachment" }

                for (l in getListeners(listener)) {
                    l.loadAttachmentFailed(account, message, part, me.message)
                }
                notifyUserIfCertificateProblem(account, me, true)
            }
        }
    }

    /**
     * Stores the given message in the Outbox and starts a sendPendingMessages command to attempt to send the message.
     */
    suspend fun sendMessage(
        account: LegacyAccountDto,
        message: Message,
        plaintextSubject: String?,
        listener: MessagingListener?,
    ) = withContext(ioDispatcher) {
        try {
            val outboxFolderId = outboxFolderManager.getOutboxFolderId(accountId = account.id, createIfMissing = true)

            message.setFlag(Flag.SEEN, true)

            val messageStore: MessageStore = messageStoreManager.getMessageStore(account)
            val messageData = saveMessageDataCreator.createSaveMessageData(
                message, MessageDownloadState.FULL, plaintextSubject,
            )
            val messageId = messageStore.saveLocalMessage(outboxFolderId, messageData, null)

            val localStore = localStoreProvider.getInstance(account)
            val outboxStateRepository = localStore.outboxStateRepository
            outboxStateRepository.initializeOutboxState(messageId)

            sendPendingMessages(account, listener)
        } catch (e: Exception) {
            logger.error(throwable = e) { "Error sending message" }
        }
    }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use sendMessage instead",
        replaceWith = ReplaceWith("sendMessage(account, message, plaintextSubject, listener)"),
    )
    fun sendMessageBlocking(
        account: LegacyAccountDto,
        message: Message,
        plaintextSubject: String?,
        listener: MessagingListener?,
    ) = runBlocking {
        sendMessage(account, message, plaintextSubject, listener)
    }

    /**
     * Attempt to send any messages that are sitting in the Outbox.
     */
    fun sendPendingMessages(account: LegacyAccountDto, listener: MessagingListener?) {
        putBackground(description = "sendPendingMessages", listener = listener) {
            if (outboxFolderManager.hasPendingMessagesSync(account.id.toString())) {
                showSendingNotificationIfNecessary(account)

                try {
                    sendPendingMessagesSynchronous(account)
                } finally {
                    clearSendingNotificationIfNecessary(account)
                }
            }
        }
    }

    private fun showSendingNotificationIfNecessary(account: LegacyAccountDto) {
        if (account.isNotifySync) {
            notificationController.showSendingNotification(account)
        }
    }

    private fun clearSendingNotificationIfNecessary(account: LegacyAccountDto) {
        if (account.isNotifySync) {
            notificationController.clearSendingNotification(account)
        }
    }

    @Throws(MessagingException::class)
    fun sendMessageBlocking(account: LegacyAccountDto, message: Message) {
        val backend = getBackend(account)
        backend.sendMessage(message)
    }

    /**
     * Attempt to send any messages that are sitting in the Outbox.
     */
    @VisibleForTesting
    protected suspend fun sendPendingMessagesSynchronous(account: LegacyAccountDto) = withContext(ioDispatcher) {
        var lastFailure: Exception? = null
        try {
            if (isAuthenticationProblem(account, false)) {
                logger.debug { "Authentication will fail. Skip sending messages." }
                handleAuthenticationFailure(account, false)
                return@withContext
            }

            val localStore = localStoreProvider.getInstance(account)
            val outboxStateRepository = localStore.outboxStateRepository
            val outboxFolderId = outboxFolderManager
                .getOutboxFolderId(accountId = account.id, createIfMissing = true)
            val localFolder = localStore.getFolder(outboxFolderId)
            if (!localFolder.exists()) {
                logger.warn { "Outbox does not exist" }
                return@withContext
            }

            localFolder.open()

            val localMessages = localFolder.messages
            var progress = 0
            val todo = localMessages.size
            for (l in listeners) {
                l.synchronizeMailboxProgress(account, outboxFolderId, progress, todo)
            }
            /*
             * The profile we will use to pull all of the content
             * for a given local message into memory for sending.
             */
            val fp = FetchProfile()
            fp.add(FetchProfile.Item.ENVELOPE)
            fp.add(FetchProfile.Item.BODY)

            logger.info { "Scanning Outbox folder for messages to send" }

            val backend = getBackend(account)

            for (message in localMessages) {
                if (message.isSet(Flag.DELETED)) {
                    //FIXME: When uploading a message to the remote Sent folder the move code creates a placeholder
                    // message in the Outbox. This code gets rid of these messages. It'd be preferable if the
                    // placeholder message was never created, though.
                    message.destroy()
                    continue
                }
                try {
                    val messageId = message.databaseId
                    val outboxState = outboxStateRepository.getOutboxState(messageId)

                    val sendState = outboxState.sendState
                    if (sendState != SendState.READY) {
                        logger.verbose {
                            "Skipping sending message ${message.uid} (reason: ${sendState.databaseName} - ${
                                outboxState.sendError
                            })"
                        }

                        lastFailure = if (sendState == SendState.RETRIES_EXCEEDED) {
                            MessagingException("Retries exceeded", true)
                        } else {
                            MessagingException(outboxState.sendError, true)
                        }
                        continue
                    }

                    logger.info {
                        "Send count for message ${message.uid} is ${outboxState.numberOfSendAttempts}"
                    }

                    localFolder.fetch(listOf(message), fp, null)
                    try {
                        if (message.getHeader(K9.IDENTITY_HEADER).isNotEmpty() || message.isSet(Flag.DRAFT)) {
                            logger.verbose {
                                "The user has set the Outbox and Drafts folder to the same thing. " +
                                    "This message appears to be a draft, so K-9 will not send it"
                            }
                            continue
                        }

                        outboxStateRepository.incrementSendAttempts(messageId)
                        message.setFlag(Flag.X_SEND_IN_PROGRESS, true)

                        logger.info { "Sending message with UID ${message.uid}" }
                        backend.sendMessage(message)

                        message.setFlag(Flag.X_SEND_IN_PROGRESS, false)
                        message.setFlag(Flag.SEEN, true)
                        progress++
                        for (l in listeners) {
                            l.synchronizeMailboxProgress(account, outboxFolderId, progress, todo)
                        }
                        moveOrDeleteSentMessage(account, localStore, message)

                        outboxStateRepository.removeOutboxState(messageId)
                    } catch (e: AuthenticationFailedException) {
                        outboxStateRepository.decrementSendAttempts(messageId)
                        lastFailure = e

                        handleAuthenticationFailure(account, false)
                        handleSendFailure(account, localFolder, message, e)
                    } catch (e: CertificateValidationException) {
                        outboxStateRepository.decrementSendAttempts(messageId)
                        lastFailure = e

                        notifyUserIfCertificateProblem(account, e, false)
                        handleSendFailure(account, localFolder, message, e)
                    } catch (e: MessagingException) {
                        lastFailure = e

                        if (e.isPermanentFailure) {
                            val errorMessage = e.message ?: "Permanent Failure. Unknown reason."
                            outboxStateRepository.setSendAttemptError(messageId, errorMessage)
                        } else if (outboxState.numberOfSendAttempts + 1 >= MAX_SEND_ATTEMPTS) {
                            outboxStateRepository.setSendAttemptsExceeded(messageId)
                        }

                        handleSendFailure(account, localFolder, message, e)
                    } catch (e: Exception) {
                        lastFailure = e

                        handleSendFailure(account, localFolder, message, e)
                    }
                } catch (e: Exception) {
                    lastFailure = e

                    logger.error(throwable = e) { "Failed to fetch message for sending" }
                    notifySynchronizeMailboxFailed(account, localFolder, e)
                }
            }

            if (lastFailure != null) {
                notificationController.showSendFailedNotification(account, lastFailure)
            }
        } catch (e: Exception) {
            logger.verbose(throwable = e) { "Failed to send pending messages" }
        } finally {
            if (lastFailure == null) {
                notificationController.clearSendFailedNotification(account)
            }
        }
    }

    @Throws(MessagingException::class)
    private suspend fun moveOrDeleteSentMessage(
        account: LegacyAccountDto,
        localStore: LocalStore,
        message: LocalMessage,
    ) = withContext(ioDispatcher) {
        if (!account.hasSentFolder() || !account.isUploadSentMessages) {
            logger.info { "Not uploading sent message; deleting local message" }
            message.destroy()
        } else {
            val sentFolderId = requireNotNull(account.sentFolderId) { "Folder id can't be null" }
            val sentFolder = localStore.getFolder(sentFolderId)
            sentFolder.open()
            val sentFolderServerId = sentFolder.serverId
            logger.info { "Moving sent message to folder '$sentFolderServerId' ($sentFolderId)" }

            val messageStore: MessageStore = messageStoreManager.getMessageStore(account)
            val destinationMessageId = messageStore.moveMessage(message.databaseId, sentFolderId)

            logger.info { "Moved sent message to folder '$sentFolderServerId' ($sentFolderId)" }

            if (!sentFolder.isLocalOnly) {
                val destinationUid = messageStore.getMessageServerId(destinationMessageId)
                if (destinationUid != null) {
                    val command: PendingCommand = PendingAppend.create(sentFolderId, destinationUid)
                    queuePendingCommand(account, command)
                    processPendingCommands(account)
                }
            }
        }

        val outboxFolderId =
            outboxFolderManager.getOutboxFolderId(accountId = account.id, createIfMissing = true)
        for (listener in listeners) {
            listener.folderStatusChanged(account, outboxFolderId)
        }
    }

    @Throws(MessagingException::class)
    private fun handleSendFailure(
        account: LegacyAccountDto,
        localFolder: LocalFolder,
        message: Message,
        exception: Exception,
    ) {
        logger.error(throwable = exception) { "Failed to send message" }
        message.setFlag(Flag.X_SEND_FAILED, true)

        notifySynchronizeMailboxFailed(account, localFolder, exception)
    }

    private fun notifySynchronizeMailboxFailed(
        account: LegacyAccountDto,
        localFolder: LocalFolder,
        exception: Exception,
    ) {
        val folderId = localFolder.databaseId
        val errorMessage = exception.rootCauseMessage
        for (listener in listeners) {
            listener.synchronizeMailboxFailed(account, folderId, errorMessage)
        }
    }

    fun isMoveCapable(messageReference: MessageReference): Boolean =
        !messageReference.uid.startsWith(localMessageUidPrefixProvider.get())

    fun isCopyCapable(message: MessageReference): Boolean = isMoveCapable(message)

    fun isMoveCapable(account: LegacyAccountDto): Boolean = getBackend(account).supportsMove

    fun isCopyCapable(account: LegacyAccountDto): Boolean = getBackend(account).supportsCopy

    fun isPushCapable(account: LegacyAccountDto): Boolean = getBackend(account).isPushCapable

    fun supportsFlags(account: LegacyAccountDto): Boolean = getBackend(account).supportsFlags

    fun supportsExpunge(account: LegacyAccountDto): Boolean = getBackend(account).supportsExpunge

    fun supportsSearchByDate(account: LegacyAccountDto): Boolean = getBackend(account).supportsSearchByDate

    fun supportsUpload(account: LegacyAccountDto): Boolean = getBackend(account).supportsUpload

    fun supportsFolderSubscriptions(account: LegacyAccountDto): Boolean =
        getBackend(account).supportsFolderSubscriptions

    suspend fun moveMessages(
        srcAccount: LegacyAccountDto,
        srcFolderId: Long,
        messageReferences: List<MessageReference?>,
        destFolderId: Long,
    ) {
        actOnMessageGroup(srcAccount, srcFolderId, messageReferences) { account, _, messages ->
            suppressMessages(account, messages)
            putBackground(description = "moveMessages", listener = null) {
                moveOrCopyMessageSynchronous(
                    account = account,
                    srcFolderId = srcFolderId,
                    inMessages = messages,
                    destFolderId = destFolderId,
                    operation = MoveOrCopyFlavor.MOVE,
                )
            }
        }
    }

    suspend fun moveMessagesInThread(
        srcAccount: LegacyAccountDto,
        srcFolderId: Long,
        messageReferences: List<MessageReference>,
        destFolderId: Long,
    ) {
        actOnMessageGroup(srcAccount, srcFolderId, messageReferences) { account, _, messages ->
            suppressMessages(account, messages)
            putBackground(description = "moveMessagesInThread", listener = null) {
                try {
                    val messagesInThreads = collectMessagesInThreads(account, messages)
                    moveOrCopyMessageSynchronous(
                        account = account,
                        srcFolderId = srcFolderId,
                        inMessages = messagesInThreads,
                        destFolderId = destFolderId,
                        operation = MoveOrCopyFlavor.MOVE,
                    )
                } catch (e: MessagingException) {
                    logger.error(throwable = e) { "Exception while moving messages" }
                }
            }
        }
    }

    suspend fun moveMessage(
        account: LegacyAccountDto,
        srcFolderId: Long,
        message: MessageReference?,
        destFolderId: Long,
    ) {
        moveMessages(account, srcFolderId, listOf(message), destFolderId)
    }

    suspend fun copyMessages(
        srcAccount: LegacyAccountDto,
        srcFolderId: Long,
        messageReferences: List<MessageReference?>,
        destFolderId: Long,
    ) {
        actOnMessageGroup(srcAccount, srcFolderId, messageReferences) { _, _, messages ->
            putBackground(description = "copyMessages", listener = null) {
                moveOrCopyMessageSynchronous(
                    account = srcAccount,
                    srcFolderId = srcFolderId,
                    inMessages = messages,
                    destFolderId = destFolderId,
                    operation = MoveOrCopyFlavor.COPY,
                )
            }
        }
    }

    suspend fun copyMessagesInThread(
        srcAccount: LegacyAccountDto,
        srcFolderId: Long,
        messageReferences: List<MessageReference>,
        destFolderId: Long,
    ) {
        actOnMessageGroup(srcAccount, srcFolderId, messageReferences) { account, _, messages ->
            putBackground(description = "copyMessagesInThread", listener = null) {
                try {
                    val messagesInThreads = collectMessagesInThreads(account, messages)
                    moveOrCopyMessageSynchronous(
                        account = account,
                        srcFolderId = srcFolderId,
                        inMessages = messagesInThreads,
                        destFolderId = destFolderId,
                        operation = MoveOrCopyFlavor.COPY,
                    )
                } catch (e: MessagingException) {
                    logger.error(throwable = e) { "Exception while copying messages" }
                }
            }
        }
    }

    suspend fun copyMessage(
        account: LegacyAccountDto,
        srcFolderId: Long,
        message: MessageReference?,
        destFolderId: Long,
    ) {
        copyMessages(account, srcFolderId, listOf(message), destFolderId)
    }

    @Throws(RuntimeException::class)
    suspend fun moveOrCopyMessageSynchronous(
        account: LegacyAccountDto,
        srcFolderId: Long,
        inMessages: List<LocalMessage>,
        destFolderId: Long,
        operation: MoveOrCopyFlavor,
    ) = withContext(ioDispatcher) {
        try {
            val localStore = localStoreProvider.getInstance(account)
            if (operation == MoveOrCopyFlavor.MOVE && !isMoveCapable(account)) {
                return@withContext
            }
            if (operation == MoveOrCopyFlavor.COPY && !isCopyCapable(account)) {
                return@withContext
            }

            val localSrcFolder = localStore.getFolder(srcFolderId)
            localSrcFolder.open()

            val localDestFolder = localStore.getFolder(destFolderId)
            localDestFolder.open()

            var unreadCountAffected = false
            val uids = mutableListOf<String>()
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
            if (messages.isNotEmpty()) {
                logger.info {
                    "moveOrCopyMessageSynchronous: source folder = $srcFolderId, ${messages.size} messages, " +
                        "destination folder = $destFolderId, operation = ${operation.name}"
                }

                val messageStore: MessageStore = messageStoreManager.getMessageStore(account)

                val messageIds = mutableListOf<Long>()
                val messageIdToUidMapping = mutableMapOf<Long, String>()
                for (message in messages) {
                    val messageId = message.databaseId
                    messageIds.add(messageId)
                    messageIdToUidMapping[messageId] = message.uid
                }

                val resultIdMapping: Map<Long, Long>
                if (operation == MoveOrCopyFlavor.COPY) {
                    resultIdMapping = messageStore.copyMessages(messageIds, destFolderId)

                    if (unreadCountAffected) {
                        // If this copy operation changes the unread count in the destination
                        // folder, notify the listeners.
                        for (l in listeners) {
                            l.folderStatusChanged(account, destFolderId)
                        }
                    }
                } else {
                    resultIdMapping = messageStore.moveMessages(messageIds, destFolderId)

                    unsuppressMessages(account, messages)

                    if (unreadCountAffected) {
                        // If this move operation changes the unread count, notify the listeners
                        // that the unread count changed in both the source and destination folder.
                        for (l in listeners) {
                            l.folderStatusChanged(account, srcFolderId)
                            l.folderStatusChanged(account, destFolderId)
                        }
                    }
                }

                val destinationMapping = messageStore.getMessageServerIds(resultIdMapping.values)

                val uidMap = mutableMapOf<String, String>()
                for ((sourceMessageId, destinationMessageId) in resultIdMapping) {
                    val sourceUid = messageIdToUidMapping.getValue(sourceMessageId)
                    val destinationUid = destinationMapping.getValue(destinationMessageId)
                    uidMap[sourceUid] = destinationUid
                }

                queueMoveOrCopy(
                    account = account,
                    srcFolderId = localSrcFolder.databaseId,
                    destFolderId = localDestFolder.databaseId,
                    operation = operation,
                    uidMap = uidMap,
                )
            }

            processPendingCommands(account)
        } catch (me: MessagingException) {
            throw RuntimeException("Error moving message", me)
        }
    }

    fun moveToDraftsFolder(account: LegacyAccountDto, folderId: Long, messages: List<MessageReference>) {
        putBackground(description = "moveToDrafts", listener = null) {
            moveToDraftsFolderInBackground(account, folderId, messages)
        }
    }

    private suspend fun moveToDraftsFolderInBackground(
        account: LegacyAccountDto,
        folderId: Long,
        messages: List<MessageReference>,
    ) = withContext(ioDispatcher) {
        for ((_, _, uid) in messages) {
            try {
                val message: Message = loadMessage(account, folderId, uid)
                val draftMessageId: Long? = saveDraft(account, message, null, message.getSubject())

                val draftSavedSuccessfully = draftMessageId != null
                if (draftSavedSuccessfully) {
                    message.destroy()
                }

                for (listener in listeners) {
                    listener.folderStatusChanged(account, folderId)
                }
            } catch (e: MessagingException) {
                logger.error(throwable = e) { "Error loading message. Draft was not saved." }
            }
        }
    }

    suspend fun archiveThreads(messages: List<MessageReference>) = withContext(ioDispatcher) {
        archiveOperations.archiveThreads(messages)
    }

    suspend fun archiveMessages(messages: List<MessageReference>) = withContext(ioDispatcher) {
        archiveOperations.archiveMessages(messages)
    }

    suspend fun archiveMessage(message: MessageReference) = withContext(ioDispatcher) {
        archiveOperations.archiveMessage(message)
    }

    fun expunge(account: LegacyAccountDto, folderId: Long) {
        putBackground(description = "expunge", listener = null) {
            queueExpunge(account, folderId)
            processPendingCommands(account)
        }
    }

    suspend fun deleteDraftSkippingTrashFolder(account: LegacyAccountDto, messageId: Long) {
        deleteDraft(account, messageId, true)
    }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use deleteDraft instead",
        replaceWith = ReplaceWith("deleteDraft(account, messageId)"),
    )
    fun deleteDraftSkippingTrashFolderBlocking(account: LegacyAccountDto, messageId: Long) = runBlocking {
        deleteDraftSkippingTrashFolder(account, messageId)
    }

    suspend fun deleteDraft(account: LegacyAccountDto, messageId: Long) {
        deleteDraft(account, messageId, false)
    }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use deleteDraft instead",
        replaceWith = ReplaceWith("deleteDraft(account, messageId)"),
    )
    fun deleteDraftBlocking(account: LegacyAccountDto, messageId: Long) = runBlocking {
        deleteDraft(account, messageId)
    }

    private suspend fun deleteDraft(account: LegacyAccountDto, messageId: Long, skipTrashFolder: Boolean) =
        withContext(ioDispatcher) {
            val folderId = account.draftsFolderId
            if (folderId == null) {
                logger.warn { "No Drafts folder configured. Can't delete draft." }
                return@withContext
            }

            val messageStore: MessageStore = messageStoreManager.getMessageStore(account)
            val messageServerId = messageStore.getMessageServerId(messageId)
            if (messageServerId != null) {
                val messageReference = MessageReference(account.id, folderId, messageServerId)
                deleteMessages(listOf(messageReference), skipTrashFolder)
            }
        }

    suspend fun deleteThreads(messages: List<MessageReference>) {
        actOnMessagesGroupedByAccountAndFolder(messages) { account, messageFolder, accountMessages ->
            suppressMessages(account, accountMessages)
            putBackground(description = "deleteThreads", listener = null) {
                deleteThreadsSynchronous(
                    account = account,
                    folderId = messageFolder.databaseId,
                    messages = accountMessages,
                    skipTrashFolder = false,
                )
            }
        }
    }

    private suspend fun deleteThreadsSynchronous(
        account: LegacyAccountDto,
        folderId: Long,
        messages: List<LocalMessage>,
        skipTrashFolder: Boolean,
    ) {
        try {
            val messagesToDelete = collectMessagesInThreads(account, messages)
            deleteMessagesSynchronous(account, folderId, messagesToDelete, skipTrashFolder)
        } catch (e: MessagingException) {
            logger.error(throwable = e) { "Something went wrong while deleting threads" }
        }
    }

    @Throws(MessagingException::class)
    suspend fun collectMessagesInThreads(account: LegacyAccountDto, messages: List<LocalMessage>): List<LocalMessage> =
        withContext(ioDispatcher) {
            val localStore = localStoreProvider.getInstance(account)

            val messagesInThreads = mutableListOf<LocalMessage>()
            for (localMessage in messages) {
                val rootId = localMessage.rootId
                val threadId = if (rootId == -1L) localMessage.threadId else rootId

                val messagesInThread = localStore.getMessagesInThread(threadId)

                messagesInThreads.addAll(messagesInThread)
            }

            messagesInThreads
        }

    suspend fun deleteMessage(message: MessageReference?) {
        deleteMessages(listOf(element = message), false)
    }

    suspend fun deleteMessages(messages: List<MessageReference?>) {
        deleteMessages(messages = messages, skipTrashFolder = false)
    }

    private suspend fun deleteMessages(messages: List<MessageReference?>, skipTrashFolder: Boolean) {
        actOnMessagesGroupedByAccountAndFolder(messages) { account, messageFolder, accountMessages ->
            suppressMessages(account, accountMessages)
            putBackground(description = "deleteMessages", listener = null) {
                deleteMessagesSynchronous(account, messageFolder.databaseId, accountMessages, skipTrashFolder)
            }
        }
    }

    private suspend fun deleteMessagesSynchronous(
        account: LegacyAccountDto,
        folderId: Long,
        messages: List<LocalMessage>,
        skipTrashFolder: Boolean,
    ) = withContext(ioDispatcher) {
        try {
            val localOnlyMessages = mutableListOf<LocalMessage>()
            val syncedMessages = mutableListOf<LocalMessage>()
            val syncedMessageUids = mutableListOf<String>()
            for (message in messages) {
                notificationController.removeNewMailNotification(account, message.makeMessageReference())

                val uid = message.uid
                if (uid.startsWith(localMessageUidPrefixProvider.get())) {
                    localOnlyMessages.add(message)
                } else {
                    syncedMessages.add(message)
                    syncedMessageUids.add(uid)
                }
            }

            val backend = getBackend(account)

            val localStore = localStoreProvider.getInstance(account)
            val localFolder = localStore.getFolder(folderId)
            localFolder.open()

            var uidMap: MutableMap<String, String>? = null
            val trashFolderId = account.trashFolderId
            val doNotMoveToTrashFolder = skipTrashFolder ||
                localDeleteOperationDecider.isDeleteImmediately(account, folderId)

            var localTrashFolder: LocalFolder? = null
            if (doNotMoveToTrashFolder) {
                logger.debug { "Not moving deleted messages to local Trash folder. Removing local copies." }

                if (localOnlyMessages.isNotEmpty()) {
                    localFolder.destroyMessages(localOnlyMessages)
                }
                if (syncedMessages.isNotEmpty()) {
                    localFolder.setFlags(syncedMessages, setOf(Flag.DELETED), true)
                }
            } else {
                logger.debug { "Deleting messages in normal folder, moving" }
                checkNotNull(trashFolderId) { "Trash folder ID is required to move deleted messages" }
                localTrashFolder = localStore.getFolder(trashFolderId)

                val messageStore: MessageStore = messageStoreManager.getMessageStore(account)

                val messageIds = mutableListOf<Long>()
                val messageIdToUidMapping = mutableMapOf<Long, String>()
                for (message in messages) {
                    val messageId = message.databaseId
                    messageIds.add(messageId)
                    messageIdToUidMapping[messageId] = message.uid
                }

                val moveMessageIdMapping = messageStore.moveMessages(messageIds, trashFolderId)

                val destinationMapping = messageStore.getMessageServerIds(moveMessageIdMapping.values)
                uidMap = mutableMapOf()
                for ((sourceMessageId, destinationMessageId) in moveMessageIdMapping) {
                    val sourceUid = messageIdToUidMapping.getValue(sourceMessageId)
                    val destinationUid = destinationMapping.getValue(destinationMessageId)
                    uidMap[sourceUid] = destinationUid
                }

                if (account.isMarkMessageAsReadOnDelete) {
                    val destinationMessageIds = moveMessageIdMapping.values
                    messageStore.setFlag(destinationMessageIds, Flag.SEEN, true)
                }
            }

            for (l in listeners) {
                l.folderStatusChanged(account, folderId)
                if (localTrashFolder != null && trashFolderId != null) {
                    l.folderStatusChanged(account, trashFolderId)
                }
            }
            logger.debug { "Delete policy for account $account is ${account.deletePolicy}" }

            val outboxFolderId = outboxFolderManager.getOutboxFolderId(accountId = account.id, createIfMissing = true)

            when {
                outboxFolderId != -1L && folderId == outboxFolderId && supportsUpload(account) -> {
                    for (destinationUid in checkNotNull(uidMap).values) {
                        // If the message was in the Outbox, then it has been copied to local Trash, and has
                        // to be copied to remote trash
                        val command: PendingCommand = PendingAppend.create(checkNotNull(trashFolderId), destinationUid)
                        queuePendingCommand(account, command)
                    }
                    processPendingCommands(account)
                }

                localFolder.isLocalOnly -> {
                    // Nothing to do on the remote side
                }

                syncedMessageUids.isNotEmpty() -> {
                    when (account.deletePolicy) {
                        DeletePolicy.ON_DELETE if (doNotMoveToTrashFolder || !backend.supportsTrashFolder) -> {
                            queueDelete(account, folderId, syncedMessageUids)
                            processPendingCommands(account)
                        }

                        DeletePolicy.ON_DELETE if account.isMarkMessageAsReadOnDelete -> {
                            queueMoveOrCopy(
                                account = account,
                                srcFolderId = folderId,
                                destFolderId = checkNotNull(trashFolderId),
                                operation = MoveOrCopyFlavor.MOVE_AND_MARK_AS_READ,
                                uidMap = checkNotNull(uidMap),
                            )
                            processPendingCommands(account)
                        }

                        DeletePolicy.ON_DELETE -> {
                            queueMoveOrCopy(
                                account = account,
                                srcFolderId = folderId,
                                destFolderId = checkNotNull(trashFolderId),
                                operation = MoveOrCopyFlavor.MOVE,
                                uidMap = checkNotNull(uidMap),
                            )
                            processPendingCommands(account)
                        }

                        DeletePolicy.MARK_AS_READ -> {
                            queueSetFlag(account, localFolder.databaseId, true, Flag.SEEN, syncedMessageUids)
                            processPendingCommands(account)
                        }

                        else -> {
                            logger.debug { "Delete policy ${account.deletePolicy} prevents delete from server" }
                        }
                    }
                }
            }

            unsuppressMessages(account, messages)
        } catch (me: MessagingException) {
            throw RuntimeException("Error deleting message from local store.", me)
        }
    }

    private fun getUidsFromMessages(messages: List<LocalMessage>): List<String> = messages.map { it.uid }

    @Throws(MessagingException::class)
    suspend fun processPendingEmptySpam(account: LegacyAccountDto) = withContext(ioDispatcher) {
        if (!account.hasSpamFolder()) {
            return@withContext
        }

        val spamFolderId = checkNotNull(account.spamFolderId)
        val localStore = localStoreProvider.getInstance(account)
        val folder = localStore.getFolder(spamFolderId)
        folder.open()
        val spamFolderServerId = folder.serverId

        val backend = getBackend(account)
        backend.deleteAllMessages(spamFolderServerId)

        // Remove all messages marked as deleted
        folder.destroyDeletedMessages()

        compact(account)
    }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use processPendingEmptySpam(account) instead",
        replaceWith = ReplaceWith("processPendingEmptySpam(account)"),
    )
    @Throws(MessagingException::class)
    fun processPendingEmptySpamBlocking(account: LegacyAccountDto) = runBlocking { processPendingEmptySpam(account) }

    fun emptySpam(account: LegacyAccountDto, listener: MessagingListener?) {
        putBackground(description = "emptySpam", listener = listener) {
            try {
                val spamFolderId = account.spamFolderId
                if (spamFolderId == null) {
                    logger.warn { "No Spam folder configured. Can't empty spam." }
                    return@putBackground
                }

                val localStore = localStoreProvider.getInstance(account)
                val localFolder = localStore.getFolder(spamFolderId)
                localFolder.open()

                localFolder.destroyLocalOnlyMessages()
                localFolder.setFlags(setOf(Flag.DELETED), true)

                for (l in listeners) {
                    l.folderStatusChanged(account, spamFolderId)
                }

                val command: PendingCommand = PendingEmptySpam.create()
                queuePendingCommand(account, command)
                processPendingCommands(account)
            } catch (e: Exception) {
                logger.error(throwable = e) { "emptySpam failed" }
            }
        }
    }

    @Throws(MessagingException::class)
    suspend fun processPendingEmptyTrash(account: LegacyAccountDto) = withContext(ioDispatcher) {
        if (!account.hasTrashFolder()) {
            return@withContext
        }

        val trashFolderId = checkNotNull(account.trashFolderId)
        val localStore = localStoreProvider.getInstance(account)
        val folder = localStore.getFolder(trashFolderId)
        folder.open()
        val trashFolderServerId = folder.serverId

        val backend = getBackend(account)
        backend.deleteAllMessages(trashFolderServerId)

        // Remove all messages marked as deleted
        folder.destroyDeletedMessages()

        compact(account)
    }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use processPendingEmptyTrash(account) instead",
        replaceWith = ReplaceWith("processPendingEmptyTrash(account)"),
    )
    @Throws(MessagingException::class)
    fun processPendingEmptyTrashBlocking(account: LegacyAccountDto) = runBlocking { processPendingEmptyTrash(account) }

    fun emptyTrash(account: LegacyAccountDto, listener: MessagingListener?) {
        putBackground(description = "emptyTrash", listener = listener) {
            try {
                val trashFolderId = account.trashFolderId
                if (trashFolderId == null) {
                    logger.warn { "No Trash folder configured. Can't empty trash." }
                    return@putBackground
                }

                val localStore = localStoreProvider.getInstance(account)
                val localFolder = localStore.getFolder(trashFolderId)
                localFolder.open()

                val isTrashLocalOnly = isTrashLocalOnly(account)
                if (isTrashLocalOnly) {
                    localFolder.clearAllMessages()
                } else {
                    localFolder.destroyLocalOnlyMessages()
                    localFolder.setFlags(setOf(Flag.DELETED), true)
                }

                for (l in listeners) {
                    l.folderStatusChanged(account, trashFolderId)
                }

                if (!isTrashLocalOnly) {
                    val command: PendingCommand = PendingEmptyTrash.create()
                    queuePendingCommand(account, command)
                    processPendingCommands(account)
                }
            } catch (e: Exception) {
                logger.error(throwable = e) { "emptyTrash failed" }
            }
        }
    }

    fun clearFolder(account: LegacyAccountDto, folderId: Long) {
        putBackground(description = "clearFolder", listener = null) {
            clearFolderSynchronous(account, folderId)
        }
    }

    @VisibleForTesting
    protected suspend fun clearFolderSynchronous(account: LegacyAccountDto, folderId: Long) =
        withContext(ioDispatcher) {
            try {
                val localFolder = localStoreProvider.getInstance(account).getFolder(folderId)
                localFolder.open()
                localFolder.clearAllMessages()
            } catch (e: Exception) {
                logger.error(throwable = e) { "clearFolder failed" }
            }
        }

    /**
     * Find out whether the account type only supports a local Trash folder.
     *
     * Note: Currently this is only the case for POP3 accounts.
     *
     * @param account The account to check.
     * @return `true` if the account only has a local Trash folder that is not synchronized with a folder on the
     * server. `false` otherwise.
     */
    private fun isTrashLocalOnly(account: LegacyAccountDto): Boolean {
        val backend = getBackend(account)
        return !backend.supportsTrashFolder
    }

    fun performPeriodicMailSync(account: LegacyAccountDto): Boolean {
        val latch = CountDownLatch(1)
        val syncError = MutableBoolean(false)
        checkMail(
            account = account,
            ignoreLastCheckedTime = false,
            useManualWakeLock = false,
            notify = true,
            listener = object : SimpleMessagingListener() {
                override fun checkMailFinished(context: Context?, account: LegacyAccountDto?) {
                    latch.countDown()
                }

                override fun synchronizeMailboxFailed(account: LegacyAccountDto?, folderId: Long, message: String?) {
                    syncError.value = true
                }
            },
        )

        logger.verbose { "performPeriodicMailSync($account) about to await latch release" }

        try {
            latch.await()
            logger.verbose { "performPeriodicMailSync($account) got latch release" }
        } catch (e: Exception) {
            logger.error(throwable = e) { "Interrupted while awaiting latch release" }
        }

        val success = !syncError.value
        if (success) {
            val now = System.currentTimeMillis()
            logger.verbose { "Account $account successfully synced @ $now" }
            account.lastSyncTime = now
            preferences.saveAccount(account)
        }

        return success
    }

    /**
     * Checks mail for one or multiple accounts. If account is null all accounts are checked.
     */
    override fun checkMail(
        account: LegacyAccountDto?,
        ignoreLastCheckedTime: Boolean,
        useManualWakeLock: Boolean,
        notify: Boolean,
        listener: MessagingListener?,
    ) {
        val wakeLock: WakeLock?
        if (useManualWakeLock) {
            val pm: PowerManager = DI.get(PowerManager::class.java)

            wakeLock = pm.newWakeLock("K9 MessagingController.checkMail")
            wakeLock.setReferenceCounted(false)
            wakeLock.acquire(K9.MANUAL_WAKE_LOCK_TIMEOUT.toLong())
        } else {
            wakeLock = null
        }

        for (l in getListeners(listener)) {
            l.checkMailStarted(context, account)
        }

        putBackground(description = "checkMail", listener = listener) {
            try {
                logger.info { "Starting mail check" }

                val accounts: List<LegacyAccountDto> = if (account != null) {
                    listOf(account)
                } else {
                    preferences.getAccounts()
                }

                for (accountToCheck in accounts) {
                    checkMailForAccount(accountToCheck, ignoreLastCheckedTime, notify, listener)
                }
            } catch (e: Exception) {
                logger.error(throwable = e) { "Unable to synchronize mail" }
            }

            putBackground(description = "finalize sync", listener = null) {
                logger.info { "Finished mail sync" }

                wakeLock?.release()

                for (l in getListeners(listener)) {
                    l.checkMailFinished(context, account)
                }
            }
        }
    }

    private suspend fun checkMailForAccount(
        account: LegacyAccountDto,
        ignoreLastCheckedTime: Boolean,
        notify: Boolean,
        listener: MessagingListener?,
    ) = withContext(ioDispatcher) {
        logger.info { "Synchronizing account $account" }

        val notificationState = NotificationState()

        sendPendingMessages(account, listener)

        refreshFolderListIfStale(account)

        try {
            val localStore = localStoreProvider.getInstance(account)
            for (folder in localStore.getPersonalNamespaces(false)) {
                folder.open()

                if (!folder.isVisible) {
                    // Never sync a folder that isn't displayed
                    continue
                }

                if (!folder.isSyncEnabled) {
                    // Do not sync folders that are not enabled for sync.
                    continue
                }

                synchronizeFolder(account, folder, ignoreLastCheckedTime, notify, listener, notificationState)
            }
        } catch (e: MessagingException) {
            logger.error(throwable = e) { "Unable to synchronize account $account" }
        } finally {
            putBackground(description = "clear notification flag for $account", listener = null) {
                logger.verbose { "Clearing notification flag for $account" }

                clearFetchingMailNotification(account)
            }
        }
    }

    private fun synchronizeFolder(
        account: LegacyAccountDto,
        folder: LocalFolder,
        ignoreLastCheckedTime: Boolean,
        notify: Boolean,
        listener: MessagingListener?,
        notificationState: NotificationState,
    ) {
        putBackground(description = "sync${folder.serverId}", listener = null) {
            synchronizeFolderInBackground(
                account = account,
                folder = folder,
                ignoreLastCheckedTime = ignoreLastCheckedTime,
                notify = notify,
                listener = listener,
                notificationState = notificationState,
            )
        }
    }

    private suspend fun synchronizeFolderInBackground(
        account: LegacyAccountDto,
        folder: LocalFolder,
        ignoreLastCheckedTime: Boolean,
        notify: Boolean,
        listener: MessagingListener?,
        notificationState: NotificationState,
    ) {
        logger.verbose { "Folder ${folder.serverId} was last synced @ ${folder.lastChecked}" }

        if (!ignoreLastCheckedTime) {
            val lastCheckedTime = folder.lastChecked
            val now = System.currentTimeMillis()

            // If the time this folder was last checked lies in the future, we better ignore this and sync now.
            if (lastCheckedTime <= now) {
                val syncInterval = account.automaticCheckIntervalMinutes * 60L * 1000L
                val nextSyncTime = lastCheckedTime + syncInterval
                if (nextSyncTime > now) {
                    logger.verbose {
                        "Not syncing folder ${folder.serverId}, previously synced @ $lastCheckedTime which would " +
                            "be too recent for the account sync interval"
                    }
                    return
                }
            }
        }

        try {
            showFetchingMailNotificationIfNecessary(account, folder)
            try {
                synchronizeMailboxSynchronous(account, folder.databaseId, notify, listener, notificationState)
            } finally {
                showEmptyFetchingMailNotificationIfNecessary(account)
            }
        } catch (e: Exception) {
            logger.error(throwable = e) { "Exception while processing folder $account:${folder.serverId}" }
        }
    }

    private fun showFetchingMailNotificationIfNecessary(account: LegacyAccountDto, folder: LocalFolder) {
        if (account.isNotifySync) {
            notificationController.showFetchingMailNotification(account, folder)
        }
    }

    private fun showEmptyFetchingMailNotificationIfNecessary(account: LegacyAccountDto) {
        if (account.isNotifySync) {
            notificationController.showEmptyFetchingMailNotification(account)
        }
    }

    private fun clearFetchingMailNotification(account: LegacyAccountDto) {
        notificationController.clearFetchingMailNotification(account)
    }

    fun compact(account: LegacyAccountDto) {
        putBackground(description = "compact:$account", listener = null) {
            try {
                val messageStore: MessageStore = messageStoreManager.getMessageStore(account)
                messageStore.compact()
            } catch (e: Exception) {
                logger.error(throwable = e) { "Failed to compact account $account" }
            }
        }
    }

    fun deleteAccount(account: LegacyAccountDto) {
        notificationController.clearNewMailNotifications(account, false)
        memorizingMessagingListener.removeAccount(account)
    }

    /**
     * Save a draft message.
     */
    suspend fun saveDraft(
        account: LegacyAccountDto,
        message: Message,
        existingDraftId: Long?,
        plaintextSubject: String?,
    ): Long? = withContext(ioDispatcher) {
        draftOperations.saveDraft(account, message, existingDraftId, plaintextSubject)
    }

    // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
    @Deprecated(
        message = "Java compat method. Use saveDraft(account, message, existingDraftId, plaintextSubject) instead.",
        replaceWith = ReplaceWith(
            expression = "saveDraft(account, message, existingDraftId, plaintextSubject)",
        ),
    )
    fun saveDraftBlocking(
        account: LegacyAccountDto,
        message: Message,
        existingDraftId: Long?,
        plaintextSubject: String?,
    ): Long? = runBlocking { saveDraft(account, message, existingDraftId, plaintextSubject) }

    fun getId(message: Message?): Long? {
        return if (message is LocalMessage) {
            message.databaseId
        } else {
            logger.warn { "MessagingController.getId() called without a LocalMessage" }
            null
        }
    }

    fun clearNotifications(search: LocalMessageSearch) {
        put(description = "clearNotifications", listener = null) {
            notificationOperations.clearNotifications(search)
        }
    }

    fun cancelNotificationsForAccount(account: LegacyAccountDto) {
        notificationController.clearNewMailNotifications(account, true)
    }

    fun cancelNotificationForMessage(account: LegacyAccountDto, messageReference: MessageReference) {
        notificationController.removeNewMailNotification(account, messageReference)
    }

    @Deprecated("Use the notification API instead")
    fun clearCertificateErrorNotifications(account: LegacyAccountDto, incoming: Boolean) {
        notificationController.clearCertificateErrorNotifications(account, incoming)
    }

    fun notifyUserIfCertificateProblem(account: LegacyAccountDto, exception: Exception?, incoming: Boolean) {
        if (exception is CertificateValidationException) {
            notificationController.showCertificateErrorNotification(account, incoming)
        }
    }

    suspend fun checkAuthenticationProblem(account: LegacyAccountDto) {
        // checking incoming server configuration
        if (isAuthenticationProblem(account, true)) {
            handleAuthenticationFailure(account, true)
            return
        } else {
            clearAuthenticationErrorNotification(account, incoming = true, clearOnlyForOAuthAccounts = true)
        }

        // checking outgoing server configuration
        if (isAuthenticationProblem(account, false)) {
            handleAuthenticationFailure(account, false)
        } else {
            clearAuthenticationErrorNotification(account, incoming = false, clearOnlyForOAuthAccounts = true)
        }
    }

    private fun isAuthenticationProblem(account: LegacyAccountDto, incoming: Boolean): Boolean {
        val serverSettings = getServerSettings(account, incoming)

        return serverSettings.isMissingCredentials ||
            serverSettings.authenticationType == AuthType.XOAUTH2 && account.oAuthState == null
    }

    private fun getServerSettings(account: LegacyAccountDto, incoming: Boolean): ServerSettings {
        return if (incoming) account.incomingServerSettings else account.outgoingServerSettings
    }

    private suspend fun clearAuthenticationErrorNotification(
        account: LegacyAccountDto,
        incoming: Boolean,
        clearOnlyForOAuthAccounts: Boolean,
    ) {
        if (featureFlagProvider.provide(GeneratedFeatureFlagKey.DISPLAY_IN_APP_NOTIFICATIONS).isEnabled()) {
            val serverSettings = getServerSettings(account, incoming)
            val shouldClear = !clearOnlyForOAuthAccounts || serverSettings.authenticationType == AuthType.XOAUTH2

            if (shouldClear) {
                val notification = createAuthenticationErrorNotification(account, incoming)
                notificationDismisser
                    .dismiss(notification)
                    .onEach { outcome -> logger.verbose { "notificationDismisser outcome = $outcome" } }
                    .launchIn(scope = scope)
            }
        }
    }

    suspend fun actOnMessagesGroupedByAccountAndFolder(messages: List<MessageReference?>, actor: MessageActor) {
        val accountMap = groupMessagesByAccountAndFolder(messages)

        for ((accountId, folderMap) in accountMap) {
            val account = checkNotNull(preferences.getById(accountId)) { "Account not found (ID: $accountId)" }

            for ((folderId, messageList) in folderMap) {
                actOnMessageGroup(account, folderId, messageList, actor)
            }
        }
    }

    private fun groupMessagesByAccountAndFolder(
        messages: List<MessageReference?>,
    ): Map<AccountId, Map<Long, List<MessageReference>>> {
        val accountMap = mutableMapOf<AccountId, MutableMap<Long, MutableList<MessageReference>>>()

        for (message in messages) {
            if (message == null) {
                continue
            }

            val folderMap = accountMap.getOrPut(message.accountId) { mutableMapOf() }
            val messageList = folderMap.getOrPut(message.folderId) { LinkedList() }
            messageList.add(message)
        }

        return accountMap
    }

    private suspend fun actOnMessageGroup(
        account: LegacyAccountDto,
        folderId: Long,
        messageReferences: List<MessageReference?>,
        actor: MessageActor,
    ) = withContext(ioDispatcher) {
        try {
            val messageFolder = localStoreProvider.getInstance(account).getFolder(folderId)
            val localMessages = messageFolder.getMessagesByReference(messageReferences)
            actor.act(account, messageFolder, localMessages)
        } catch (e: MessagingException) {
            logger.error(throwable = e) { "Error loading account?!" }
        }
    }

    fun interface MessageActor {
        fun act(account: LegacyAccountDto, messageFolder: LocalFolder, messages: List<LocalMessage>)
    }

    private data class Command(
        val runnable: Runnable,
        val listener: MessagingListener?,
        val description: String,
        val isForegroundPriority: Boolean,
    ) : Comparable<Command> {
        @OptIn(ExperimentalAtomicApi::class)
        val sequence: Int = sequencing.fetchAndIncrement()

        override fun compareTo(other: Command): Int = when {
            other.isForegroundPriority && !isForegroundPriority -> 1
            !other.isForegroundPriority && isForegroundPriority -> -1
            else -> sequence - other.sequence
        }
    }

    internal inner class ControllerSyncListener(
        private val account: LegacyAccountDto,
        private val listener: MessagingListener?,
        private val suppressNotifications: Boolean,
        private val notificationState: NotificationState,
    ) : SyncListener {
        private val localStore: LocalStore = getLocalStoreOrThrow(account)

        var syncFailed = false
            private set

        override suspend fun syncStarted(folderServerId: String) = withContext(ioDispatcher) {
            val folderId = getFolderId(account, folderServerId)
            for (messagingListener in getListeners(listener)) {
                messagingListener.synchronizeMailboxStarted(account, folderId)
            }
        }

        override fun syncAuthenticationSuccess() {
            // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
            runBlocking {
                clearAuthenticationErrorNotification(account, incoming = true, clearOnlyForOAuthAccounts = false)
            }
            notificationController.clearAuthenticationErrorNotification(account, true)
        }

        override fun syncHeadersStarted(folderServerId: String) {
            for (messagingListener in getListeners(listener)) {
                messagingListener.synchronizeMailboxHeadersStarted(account, folderServerId)
            }
        }

        override fun syncHeadersProgress(folderServerId: String, completed: Int, total: Int) {
            for (messagingListener in getListeners(listener)) {
                messagingListener.synchronizeMailboxHeadersProgress(account, folderServerId, completed, total)
            }
        }

        override fun syncHeadersFinished(folderServerId: String, totalMessagesInMailbox: Int, numNewMessages: Int) {
            for (messagingListener in getListeners(listener)) {
                messagingListener.synchronizeMailboxHeadersFinished(
                    account,
                    folderServerId,
                    totalMessagesInMailbox,
                    numNewMessages,
                )
            }
        }

        override suspend fun syncProgress(folderServerId: String, completed: Int, total: Int) =
            withContext(ioDispatcher) {
                val folderId = getFolderId(account, folderServerId)
                for (messagingListener in getListeners(listener)) {
                    messagingListener.synchronizeMailboxProgress(account, folderId, completed, total)
                }
            }

        override suspend fun syncNewMessage(folderServerId: String, messageServerId: String, isOldMessage: Boolean) =
            withContext(ioDispatcher) {
                // Send a notification of this message
                val message = loadMessage(folderServerId, messageServerId)
                val localFolder = message.folder
                if (!suppressNotifications &&
                    notificationStrategy.shouldNotifyForMessage(account, localFolder, message, isOldMessage)
                ) {
                    // Notify with the localMessage so that we don't have to recalculate the content preview.
                    val silent = notificationState.wasNotified
                    notificationController.addNewMailNotification(account, message, silent)
                    notificationState.wasNotified = true
                }

                if (!message.isSet(Flag.SEEN)) {
                    for (messagingListener in getListeners(listener)) {
                        messagingListener.synchronizeMailboxNewMessage(account, folderServerId, message)
                    }
                }
            }

        override suspend fun syncRemovedMessage(folderServerId: String, messageServerId: String) =
            withContext(ioDispatcher) {
                for (messagingListener in getListeners(listener)) {
                    messagingListener.synchronizeMailboxRemovedMessage(account, folderServerId, messageServerId)
                }

                val folderId = getFolderId(account, folderServerId)
                val messageReference = MessageReference(account.id, folderId, messageServerId)
                notificationController.removeNewMailNotification(account, messageReference)
            }

        override suspend fun syncFlagChanged(folderServerId: String, messageServerId: String) =
            withContext(ioDispatcher) {
                var shouldBeNotifiedOf = false
                val message = loadMessage(folderServerId, messageServerId)
                if (message.isSet(Flag.DELETED) || isMessageSuppressed(message)) {
                    syncRemovedMessage(folderServerId, message.uid)
                } else {
                    val localFolder = message.folder
                    if (notificationStrategy.shouldNotifyForMessage(account, localFolder, message, false)) {
                        shouldBeNotifiedOf = true
                    }
                }

                // we're only interested in messages that need removing
                if (!shouldBeNotifiedOf) {
                    val messageReference = message.makeMessageReference()
                    notificationController.removeNewMailNotification(account, messageReference)
                }
            }

        override suspend fun syncFinished(folderServerId: String) = withContext(ioDispatcher) {
            val folderId = getFolderId(account, folderServerId)
            for (messagingListener in getListeners(listener)) {
                messagingListener.synchronizeMailboxFinished(account, folderId)
            }
        }

        override suspend fun syncFailed(folderServerId: String, message: String, exception: Exception?) =
            withContext(ioDispatcher) {
                syncFailed = true

                if (exception is AuthenticationFailedException) {
                    handleAuthenticationFailure(account, true)
                } else {
                    notifyUserIfCertificateProblem(account, exception, true)
                }

                val folderId = getFolderId(account, folderServerId)
                for (messagingListener in getListeners(listener)) {
                    messagingListener.synchronizeMailboxFailed(account, folderId, message)
                }
            }

        override suspend fun folderStatusChanged(folderServerId: String) = withContext(ioDispatcher) {
            val folderId = getFolderId(account, folderServerId)
            for (messagingListener in getListeners(listener)) {
                messagingListener.folderStatusChanged(account, folderId)
            }
        }

        private suspend fun loadMessage(folderServerId: String, messageServerId: String): LocalMessage =
            withContext(ioDispatcher) {
                try {
                    val localFolder = localStore.getFolder(folderServerId)
                    localFolder.open()
                    checkNotNull(localFolder.getMessage(messageServerId)) {
                        "Message not found ($folderServerId:$messageServerId)"
                    }
                } catch (e: MessagingException) {
                    throw RuntimeException("Couldn't load message ($folderServerId:$messageServerId)", e)
                }
            }
    }

    enum class MoveOrCopyFlavor {
        MOVE,
        COPY,
        MOVE_AND_MARK_AS_READ,
    }
}
