package com.fsck.k9.controller;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.fsck.k9.Account;
import com.fsck.k9.Account.DeletePolicy;
import com.fsck.k9.Account.Expunge;
import com.fsck.k9.AccountStats;
import com.fsck.k9.CoreResourceProvider;
import com.fsck.k9.controller.ControllerExtension.ControllerInternals;
import com.fsck.k9.core.BuildConfig;
import com.fsck.k9.DI;
import com.fsck.k9.K9;
import com.fsck.k9.K9.Intents;
import com.fsck.k9.Preferences;
import com.fsck.k9.backend.BackendManager;
import com.fsck.k9.backend.api.Backend;
import com.fsck.k9.backend.api.SyncConfig;
import com.fsck.k9.backend.api.SyncListener;
import com.fsck.k9.cache.EmailProviderCache;
import com.fsck.k9.controller.MessagingControllerCommands.PendingAppend;
import com.fsck.k9.controller.MessagingControllerCommands.PendingCommand;
import com.fsck.k9.controller.MessagingControllerCommands.PendingEmptyTrash;
import com.fsck.k9.controller.MessagingControllerCommands.PendingExpunge;
import com.fsck.k9.controller.MessagingControllerCommands.PendingMarkAllAsRead;
import com.fsck.k9.controller.MessagingControllerCommands.PendingMoveOrCopy;
import com.fsck.k9.controller.MessagingControllerCommands.PendingSetFlag;
import com.fsck.k9.controller.ProgressBodyFactory.ProgressListener;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.FetchProfile.Item;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.Pusher;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.UnavailableStorageException;
import com.fsck.k9.notification.NotificationController;
import com.fsck.k9.power.TracingPowerManager;
import com.fsck.k9.power.TracingPowerManager.TracingWakeLock;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.fsck.k9.search.SearchSpecification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import timber.log.Timber;

import static com.fsck.k9.K9.MAX_SEND_ATTEMPTS;
import static com.fsck.k9.helper.ExceptionHelper.getRootCauseMessage;
import static com.fsck.k9.mail.Flag.X_REMOTE_COPY_STARTED;


/**
 * Starts a long running (application) Thread that will run through commands
 * that require remote mailbox access. This class is used to serialize and
 * prioritize these commands. Each method that will submit a command requires a
 * MessagingListener instance to be provided. It is expected that that listener
 * has also been added as a registered listener using addListener(). When a
 * command is to be executed, if the listener that was provided with the command
 * is no longer registered the command is skipped. The design idea for the above
 * is that when an Activity starts it registers as a listener. When it is paused
 * it removes itself. Thus, any commands that that activity submitted are
 * removed from the queue once the activity is no longer active.
 */
@SuppressWarnings("unchecked") // TODO change architecture to actually work with generics
public class MessagingController {
    public static final long INVALID_MESSAGE_ID = -1;

    public static final Set<Flag> SYNC_FLAGS = EnumSet.of(Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED, Flag.FORWARDED);


    private final Context context;
    private final Contacts contacts;
    private final NotificationController notificationController;
    private final BackendManager backendManager;

    private final Thread controllerThread;

    private final BlockingQueue<Command> queuedCommands = new PriorityBlockingQueue<>();
    private final Set<MessagingListener> listeners = new CopyOnWriteArraySet<>();
    private final ConcurrentHashMap<String, AtomicInteger> sendCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Account, Pusher> pushers = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final MemorizingMessagingListener memorizingMessagingListener = new MemorizingMessagingListener();
    private final AccountStatsCollector accountStatsCollector;
    private final CoreResourceProvider resourceProvider;


    private MessagingListener checkMailListener = null;
    private volatile boolean stopped = false;


    public static MessagingController getInstance(Context context) {
        return DI.get(MessagingController.class);
    }


    MessagingController(Context context, NotificationController notificationController, Contacts contacts,
            AccountStatsCollector accountStatsCollector, CoreResourceProvider resourceProvider,
            BackendManager backendManager, List<ControllerExtension> controllerExtensions) {
        this.context = context;
        this.notificationController = notificationController;
        this.contacts = contacts;
        this.accountStatsCollector = accountStatsCollector;
        this.resourceProvider = resourceProvider;
        this.backendManager = backendManager;

        controllerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runInBackground();
            }
        });
        controllerThread.setName("MessagingController");
        controllerThread.start();
        addListener(memorizingMessagingListener);

        initializeControllerExtensions(controllerExtensions);
    }

    private void initializeControllerExtensions(List<ControllerExtension> controllerExtensions) {
        if (controllerExtensions.isEmpty()) {
            return;
        }

        ControllerInternals internals = new ControllerInternals() {
            @Override
            public void put(@NotNull String description, @Nullable MessagingListener listener,
                    @NotNull Runnable runnable) {
                MessagingController.this.put(description, listener, runnable);
            }

            @Override
            public void putBackground(@NotNull String description, @Nullable MessagingListener listener,
                    @NotNull Runnable runnable) {
                MessagingController.this.putBackground(description, listener, runnable);
            }
        };

        for (ControllerExtension extension : controllerExtensions) {
            extension.init(this, backendManager, internals);
        }
    }

    @VisibleForTesting
    void stop() throws InterruptedException {
        stopped = true;
        controllerThread.interrupt();
        controllerThread.join(1000L);
    }

    private void runInBackground() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (!stopped) {
            String commandDescription = null;
            try {
                final Command command = queuedCommands.take();

                if (command != null) {
                    commandDescription = command.description;

                    Timber.i("Running command '%s', seq = %s (%s priority)",
                            command.description,
                            command.sequence,
                            command.isForegroundPriority ? "foreground" : "background");

                    try {
                        command.runnable.run();
                    } catch (UnavailableAccountException e) {
                        // retry later
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    sleep(30 * 1000);
                                    queuedCommands.put(command);
                                } catch (InterruptedException e) {
                                    Timber.e("Interrupted while putting a pending command for an unavailable account " +
                                            "back into the queue. THIS SHOULD NEVER HAPPEN.");
                                }
                            }
                        }.start();
                    }

                    Timber.i(" Command '%s' completed", command.description);
                }
            } catch (Exception e) {
                Timber.e(e, "Error running command '%s'", commandDescription);
            }
        }
    }

    private void put(String description, MessagingListener listener, Runnable runnable) {
        putCommand(queuedCommands, description, listener, runnable, true);
    }

    private void putBackground(String description, MessagingListener listener, Runnable runnable) {
        putCommand(queuedCommands, description, listener, runnable, false);
    }

    private void putCommand(BlockingQueue<Command> queue, String description, MessagingListener listener,
            Runnable runnable, boolean isForeground) {
        int retries = 10;
        Exception e = null;
        while (retries-- > 0) {
            try {
                Command command = new Command();
                command.listener = listener;
                command.runnable = runnable;
                command.description = description;
                command.isForegroundPriority = isForeground;
                queue.put(command);
                return;
            } catch (InterruptedException ie) {
                SystemClock.sleep(200);
                e = ie;
            }
        }
        throw new Error(e);
    }

    private Backend getBackend(Account account) {
        return backendManager.getBackend(account);
    }

    private LocalStore getLocalStoreOrThrow(Account account) {
        try {
            return account.getLocalStore();
        } catch (MessagingException e) {
            throw new IllegalStateException("Couldn't get LocalStore for account " + account.getDescription());
        }
    }

    public void addListener(MessagingListener listener) {
        listeners.add(listener);
        refreshListener(listener);
    }

    public void refreshListener(MessagingListener listener) {
        if (listener != null) {
            memorizingMessagingListener.refreshOther(listener);
        }
    }

    public void removeListener(MessagingListener listener) {
        listeners.remove(listener);
    }

    public Set<MessagingListener> getListeners() {
        return listeners;
    }


    public Set<MessagingListener> getListeners(MessagingListener listener) {
        if (listener == null) {
            return listeners;
        }

        Set<MessagingListener> listeners = new HashSet<>(this.listeners);
        listeners.add(listener);
        return listeners;

    }


    private void suppressMessages(Account account, List<LocalMessage> messages) {
        EmailProviderCache cache = EmailProviderCache.getCache(account.getUuid(), context);
        cache.hideMessages(messages);
    }

    private void unsuppressMessages(Account account, List<? extends Message> messages) {
        EmailProviderCache cache = EmailProviderCache.getCache(account.getUuid(), context);
        cache.unhideMessages(messages);
    }

    public boolean isMessageSuppressed(LocalMessage message) {
        long messageId = message.getDatabaseId();
        long folderId = message.getFolder().getDatabaseId();

        EmailProviderCache cache = EmailProviderCache.getCache(message.getFolder().getAccountUuid(), context);
        return cache.isMessageHidden(messageId, folderId);
    }

    private void setFlagInCache(final Account account, final List<Long> messageIds,
            final Flag flag, final boolean newState) {

        EmailProviderCache cache = EmailProviderCache.getCache(account.getUuid(), context);
        String columnName = LocalStore.getColumnNameForFlag(flag);
        String value = Integer.toString((newState) ? 1 : 0);
        cache.setValueForMessages(messageIds, columnName, value);
    }

    private void removeFlagFromCache(final Account account, final List<Long> messageIds,
            final Flag flag) {

        EmailProviderCache cache = EmailProviderCache.getCache(account.getUuid(), context);
        String columnName = LocalStore.getColumnNameForFlag(flag);
        cache.removeValueForMessages(messageIds, columnName);
    }

    private void setFlagForThreadsInCache(final Account account, final List<Long> threadRootIds,
            final Flag flag, final boolean newState) {

        EmailProviderCache cache = EmailProviderCache.getCache(account.getUuid(), context);
        String columnName = LocalStore.getColumnNameForFlag(flag);
        String value = Integer.toString((newState) ? 1 : 0);
        cache.setValueForThreads(threadRootIds, columnName, value);
    }

    private void removeFlagForThreadsFromCache(final Account account, final List<Long> messageIds,
            final Flag flag) {

        EmailProviderCache cache = EmailProviderCache.getCache(account.getUuid(), context);
        String columnName = LocalStore.getColumnNameForFlag(flag);
        cache.removeValueForThreads(messageIds, columnName);
    }


    /**
     * Lists folders that are available locally and remotely. This method calls
     * listFoldersCallback for local folders before it returns, and then for
     * remote folders at some later point. If there are no local folders
     * includeRemote is forced by this method. This method should be called from
     * a Thread as it may take several seconds to list the local folders.
     * TODO this needs to cache the remote folder list
     */
    public void listFolders(final Account account, final boolean refreshRemote, final MessagingListener listener) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                listFoldersSynchronous(account, refreshRemote, listener);
            }
        });
    }

    /**
     * Lists folders that are available locally and remotely. This method calls
     * listFoldersCallback for local folders before it returns, and then for
     * remote folders at some later point. If there are no local folders
     * includeRemote is forced by this method. This method is called in the
     * foreground.
     * TODO this needs to cache the remote folder list
     */
    public void listFoldersSynchronous(final Account account, final boolean refreshRemote,
            final MessagingListener listener) {
        for (MessagingListener l : getListeners(listener)) {
            l.listFoldersStarted(account);
        }
        List<LocalFolder> localFolders = null;
        if (!account.isAvailable(context)) {
            Timber.i("not listing folders of unavailable account");
        } else {
            try {
                LocalStore localStore = account.getLocalStore();
                localFolders = localStore.getPersonalNamespaces(false);

                if (refreshRemote || localFolders.isEmpty()) {
                    doRefreshRemote(account, listener);
                    return;
                }

                for (MessagingListener l : getListeners(listener)) {
                    l.listFolders(account, localFolders);
                }
            } catch (Exception e) {
                for (MessagingListener l : getListeners(listener)) {
                    l.listFoldersFailed(account, e.getMessage());
                }

                Timber.e(e);
                return;
            } finally {
                if (localFolders != null) {
                    for (Folder localFolder : localFolders) {
                        closeFolder(localFolder);
                    }
                }
            }
        }

        for (MessagingListener l : getListeners(listener)) {
            l.listFoldersFinished(account);
        }
    }

    private void doRefreshRemote(final Account account, final MessagingListener listener) {
        put("doRefreshRemote", listener, new Runnable() {
            @Override
            public void run() {
                refreshRemoteSynchronous(account, listener);
            }
        });
    }

    @VisibleForTesting
    void refreshRemoteSynchronous(final Account account, final MessagingListener listener) {
        List<LocalFolder> localFolders = null;
        try {
            Backend backend = getBackend(account);
            backend.refreshFolderList();

            LocalStore localStore = account.getLocalStore();
            localFolders = localStore.getPersonalNamespaces(false);

            for (MessagingListener l : getListeners(listener)) {
                l.listFolders(account, localFolders);
            }
            for (MessagingListener l : getListeners(listener)) {
                l.listFoldersFinished(account);
            }
        } catch (Exception e) {
            Timber.e(e);
            for (MessagingListener l : getListeners(listener)) {
                l.listFoldersFailed(account, "");
            }
        } finally {
            if (localFolders != null) {
                for (Folder localFolder : localFolders) {
                    closeFolder(localFolder);
                }
            }
        }
    }

    /**
     * Find all messages in any local account which match the query 'query'
     */
    public void searchLocalMessages(final LocalSearch search, final MessagingListener listener) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                searchLocalMessagesSynchronous(search, listener);
            }
        });
    }

    @VisibleForTesting
    void searchLocalMessagesSynchronous(final LocalSearch search, final MessagingListener listener) {
        final AccountStats stats = new AccountStats();
        final Set<String> uuidSet = new HashSet<>(Arrays.asList(search.getAccountUuids()));
        List<Account> accounts = Preferences.getPreferences(context).getAccounts();
        boolean allAccounts = uuidSet.contains(SearchSpecification.ALL_ACCOUNTS);

        // for every account we want to search do the query in the localstore
        for (final Account account : accounts) {

            if (!allAccounts && !uuidSet.contains(account.getUuid())) {
                continue;
            }

            // Collecting statistics of the search result
            MessageRetrievalListener<LocalMessage> retrievalListener = new MessageRetrievalListener<LocalMessage>() {
                @Override
                public void messageStarted(String message, int number, int ofTotal) {
                }

                @Override
                public void messagesFinished(int number) {
                }

                @Override
                public void messageFinished(LocalMessage message, int number, int ofTotal) {
                    if (!isMessageSuppressed(message)) {
                        List<LocalMessage> messages = new ArrayList<>();

                        messages.add(message);
                        stats.unreadMessageCount += (!message.isSet(Flag.SEEN)) ? 1 : 0;
                        stats.flaggedMessageCount += (message.isSet(Flag.FLAGGED)) ? 1 : 0;
                        if (listener != null) {
                            listener.listLocalMessagesAddMessages(account, null, messages);
                        }
                    }
                }
            };

            // build and do the query in the localstore
            try {
                LocalStore localStore = account.getLocalStore();
                localStore.searchForMessages(retrievalListener, search);
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        // publish the total search statistics
        if (listener != null) {
            listener.searchStats(stats);
        }
    }

    public Future<?> searchRemoteMessages(final String acctUuid, final String folderServerId, final String query,
            final Set<Flag> requiredFlags, final Set<Flag> forbiddenFlags, final MessagingListener listener) {
        Timber.i("searchRemoteMessages (acct = %s, folderServerId = %s, query = %s)", acctUuid, folderServerId, query);

        return threadPool.submit(new Runnable() {
            @Override
            public void run() {
                searchRemoteMessagesSynchronous(acctUuid, folderServerId, query, requiredFlags, forbiddenFlags,
                        listener);
            }
        });
    }

    @VisibleForTesting
    void searchRemoteMessagesSynchronous(final String acctUuid, final String folderServerId, final String query,
            final Set<Flag> requiredFlags, final Set<Flag> forbiddenFlags, final MessagingListener listener) {
        final Account acct = Preferences.getPreferences(context).getAccount(acctUuid);

        if (listener != null) {
            listener.remoteSearchStarted(folderServerId);
        }

        List<String> extraResults = new ArrayList<>();
        try {
            LocalStore localStore = acct.getLocalStore();

            LocalFolder localFolder = localStore.getFolder(folderServerId);
            if (localFolder == null) {
                throw new MessagingException("Folder not found");
            }

            Backend backend = getBackend(acct);

            List<String> messageServerIds = backend.search(folderServerId, query, requiredFlags, forbiddenFlags);

            Timber.i("Remote search got %d results", messageServerIds.size());

            // There's no need to fetch messages already completely downloaded
            messageServerIds = localFolder.extractNewMessages(messageServerIds);

            if (listener != null) {
                listener.remoteSearchServerQueryComplete(folderServerId, messageServerIds.size(),
                        acct.getRemoteSearchNumResults());
            }

            int resultLimit = acct.getRemoteSearchNumResults();
            if (resultLimit > 0 && messageServerIds.size() > resultLimit) {
                extraResults = messageServerIds.subList(resultLimit, messageServerIds.size());
                messageServerIds = messageServerIds.subList(0, resultLimit);
            }

            loadSearchResultsSynchronous(acct, messageServerIds, localFolder);
        } catch (Exception e) {
            if (Thread.currentThread().isInterrupted()) {
                Timber.i(e, "Caught exception on aborted remote search; safe to ignore.");
            } else {
                Timber.e(e, "Could not complete remote search");
                if (listener != null) {
                    listener.remoteSearchFailed(null, e.getMessage());
                }
                Timber.e(e);
            }
        } finally {
            if (listener != null) {
                listener.remoteSearchFinished(folderServerId, 0, acct.getRemoteSearchNumResults(), extraResults);
            }
        }

    }

    public void loadSearchResults(final Account account, final String folderServerId,
            final List<String> messageServerIds, final MessagingListener listener) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.enableProgressIndicator(true);
                }
                try {
                    LocalStore localStore = account.getLocalStore();

                    if (localStore == null) {
                        throw new MessagingException("Could not get store");
                    }

                    LocalFolder localFolder = localStore.getFolder(folderServerId);
                    if (localFolder == null) {
                        throw new MessagingException("Folder not found");
                    }

                    loadSearchResultsSynchronous(account, messageServerIds, localFolder);
                } catch (MessagingException e) {
                    Timber.e(e, "Exception in loadSearchResults");
                } finally {
                    if (listener != null) {
                        listener.enableProgressIndicator(false);
                    }
                }
            }
        });
    }

    private void loadSearchResultsSynchronous(Account account, List<String> messageServerIds, LocalFolder localFolder)
            throws MessagingException {

        FetchProfile fetchProfile = new FetchProfile();
        fetchProfile.add(FetchProfile.Item.FLAGS);
        fetchProfile.add(FetchProfile.Item.ENVELOPE);
        fetchProfile.add(FetchProfile.Item.STRUCTURE);

        Backend backend = getBackend(account);
        String folderServerId = localFolder.getServerId();

        for (String messageServerId : messageServerIds) {
            LocalMessage localMessage = localFolder.getMessage(messageServerId);

            if (localMessage == null) {
                Message message = backend.fetchMessage(folderServerId, messageServerId, fetchProfile);
                localFolder.appendMessages(Collections.singletonList(message));
            }
        }
    }


    public void loadMoreMessages(Account account, String folder, MessagingListener listener) {
        try {
            LocalStore localStore = account.getLocalStore();
            LocalFolder localFolder = localStore.getFolder(folder);
            if (localFolder.getVisibleLimit() > 0) {
                localFolder.setVisibleLimit(localFolder.getVisibleLimit() + account.getDisplayCount());
            }
            synchronizeMailbox(account, folder, listener, null);
        } catch (MessagingException me) {
            throw new RuntimeException("Unable to set visible limit on folder", me);
        }
    }

    /**
     * Start background synchronization of the specified folder.
     */
    public void synchronizeMailbox(final Account account, final String folder, final MessagingListener listener,
            final Folder providedRemoteFolder) {
        putBackground("synchronizeMailbox", listener, new Runnable() {
            @Override
            public void run() {
                synchronizeMailboxSynchronous(account, folder, listener, providedRemoteFolder);
            }
        });
    }

    /**
     * Start foreground synchronization of the specified folder. This is generally only called
     * by synchronizeMailbox.
     * <p>
     * TODO Break this method up into smaller chunks.
     */
    @VisibleForTesting
    void synchronizeMailboxSynchronous(final Account account, final String folder, final MessagingListener listener,
            Folder providedRemoteFolder) {
        Backend remoteMessageStore = getBackend(account);
        syncFolder(account, folder, listener, providedRemoteFolder, remoteMessageStore);
    }

    private void syncFolder(Account account, String folder, MessagingListener listener, Folder providedRemoteFolder,
            Backend remoteMessageStore) {

        Exception commandException = null;
        try {
            processPendingCommandsSynchronous(account);
        } catch (Exception e) {
            Timber.e(e, "Failure processing command, but allow message sync attempt");
            commandException = e;
        }

        // We don't ever sync the Outbox
        if (folder.equals(account.getOutboxFolder())) {
            return;
        }

        SyncConfig syncConfig = createSyncConfig(account);

        ControllerSyncListener syncListener = new ControllerSyncListener(account, listener);
        remoteMessageStore.sync(folder, syncConfig, syncListener, providedRemoteFolder);

        if (commandException != null && !syncListener.syncFailed) {
            String rootMessage = getRootCauseMessage(commandException);
            Timber.e("Root cause failure in %s:%s was '%s'", account.getDescription(), folder, rootMessage);
            updateFolderStatus(account, folder, rootMessage);
            listener.synchronizeMailboxFailed(account, folder, rootMessage);
        }
    }

    private SyncConfig createSyncConfig(Account account) {
        return new SyncConfig(
                    account.getExpungePolicy().toBackendExpungePolicy(),
                    account.getEarliestPollDate(),
                    account.isSyncRemoteDeletions(),
                    account.getMaximumAutoDownloadMessageSize(),
                    K9.DEFAULT_VISIBLE_LIMIT,
                    SYNC_FLAGS);
    }

    private void updateFolderStatus(Account account, String folderServerId, String status) {
        try {
            LocalStore localStore = account.getLocalStore();
            LocalFolder localFolder = localStore.getFolder(folderServerId);
            localFolder.setStatus(status);
        } catch (MessagingException e) {
            Timber.w(e, "Couldn't update folder status for folder %s", folderServerId);
        }
    }

    public void handleAuthenticationFailure(Account account, boolean incoming) {
        notificationController.showAuthenticationErrorNotification(account, incoming);
    }

    private static void closeFolder(Folder f) {
        if (f != null) {
            f.close();
        }
    }

    private void queuePendingCommand(Account account, PendingCommand command) {
        try {
            LocalStore localStore = account.getLocalStore();
            localStore.addPendingCommand(command);
        } catch (Exception e) {
            throw new RuntimeException("Unable to enqueue pending command", e);
        }
    }

    private void processPendingCommands(final Account account) {
        putBackground("processPendingCommands", null, new Runnable() {
            @Override
            public void run() {
                try {
                    processPendingCommandsSynchronous(account);
                } catch (UnavailableStorageException e) {
                    Timber.i("Failed to process pending command because storage is not available - " +
                            "trying again later.");
                    throw new UnavailableAccountException(e);
                } catch (MessagingException me) {
                    Timber.e(me, "processPendingCommands");

                    /*
                     * Ignore any exceptions from the commands. Commands will be processed
                     * on the next round.
                     */
                }
            }
        });
    }

    public void processPendingCommandsSynchronous(Account account) throws MessagingException {
        LocalStore localStore = account.getLocalStore();
        List<PendingCommand> commands = localStore.getPendingCommands();

        int progress = 0;
        int todo = commands.size();
        if (todo == 0) {
            return;
        }

        for (MessagingListener l : getListeners()) {
            l.pendingCommandsProcessing(account);
            l.synchronizeMailboxProgress(account, null, progress, todo);
        }

        PendingCommand processingCommand = null;
        try {
            for (PendingCommand command : commands) {
                processingCommand = command;
                Timber.d("Processing pending command '%s'", command);

                for (MessagingListener l : getListeners()) {
                    l.pendingCommandStarted(account, command.getCommandName());
                }
                /*
                 * We specifically do not catch any exceptions here. If a command fails it is
                 * most likely due to a server or IO error and it must be retried before any
                 * other command processes. This maintains the order of the commands.
                 */
                try {
                    command.execute(this, account);

                    localStore.removePendingCommand(command);

                    Timber.d("Done processing pending command '%s'", command);
                } catch (MessagingException me) {
                    if (me.isPermanentFailure()) {
                        Timber.e("Failure of command '%s' was permanent, removing command from queue", command);
                        localStore.removePendingCommand(processingCommand);
                    } else {
                        throw me;
                    }
                } finally {
                    progress++;
                    for (MessagingListener l : getListeners()) {
                        l.synchronizeMailboxProgress(account, null, progress, todo);
                        l.pendingCommandCompleted(account, command.getCommandName());
                    }
                }
            }
        } catch (MessagingException me) {
            notifyUserIfCertificateProblem(account, me, true);
            Timber.e(me, "Could not process command '%s'", processingCommand);
            throw me;
        } finally {
            for (MessagingListener l : getListeners()) {
                l.pendingCommandsFinished(account);
            }
        }
    }

    /**
     * Process a pending append message command. This command uploads a local message to the
     * server, first checking to be sure that the server message is not newer than
     * the local message. Once the local message is successfully processed it is deleted so
     * that the server message will be synchronized down without an additional copy being
     * created.
     * TODO update the local message UID instead of deleting it
     */
    void processPendingAppend(PendingAppend command, Account account) throws MessagingException {
        LocalFolder localFolder = null;
        try {

            String folder = command.folder;
            String uid = command.uid;

            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folder);
            LocalMessage localMessage = localFolder.getMessage(uid);

            if (localMessage == null) {
                return;
            }

            if (!localMessage.getUid().startsWith(K9.LOCAL_UID_PREFIX)) {
                //FIXME: This should never happen. Throw in debug builds.
                return;
            }

            Backend backend = getBackend(account);

            if (localMessage.isSet(Flag.X_REMOTE_COPY_STARTED)) {
                Timber.w("Local message with uid %s has flag %s  already set, checking for remote message with " +
                        "same message id", localMessage.getUid(), X_REMOTE_COPY_STARTED);

                String messageServerId = backend.findByMessageId(folder, localMessage.getMessageId());
                if (messageServerId != null) {
                    Timber.w("Local message has flag %s already set, and there is a remote message with uid %s, " +
                            "assuming message was already copied and aborting this copy",
                            X_REMOTE_COPY_STARTED, messageServerId);

                    String oldUid = localMessage.getUid();
                    localMessage.setUid(messageServerId);
                    localFolder.changeUid(localMessage);

                    for (MessagingListener l : getListeners()) {
                        l.messageUidChanged(account, folder, oldUid, localMessage.getUid());
                    }

                    return;
                } else {
                    Timber.w("No remote message with message-id found, proceeding with append");
                }
            }

            /*
             * If the message does not exist remotely we just upload it and then
             * update our local copy with the new uid.
             */
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.BODY);
            localFolder.fetch(Collections.singletonList(localMessage), fp, null);
            String oldUid = localMessage.getUid();
            localMessage.setFlag(Flag.X_REMOTE_COPY_STARTED, true);

            String messageServerId = backend.uploadMessage(folder, localMessage);

            if (messageServerId == null) {
                // We didn't get the server UID of the uploaded message. Remove the local message now. The uploaded
                // version will be downloaded during the next sync.
                localFolder.destroyMessages(Collections.singletonList(localMessage));
            } else {
                localMessage.setUid(messageServerId);
                localFolder.changeUid(localMessage);

                for (MessagingListener l : getListeners()) {
                    l.messageUidChanged(account, folder, oldUid, localMessage.getUid());
                }
            }
        } finally {
            closeFolder(localFolder);
        }
    }

    private void queueMoveOrCopy(Account account, String srcFolder, String destFolder, boolean isCopy,
            List<String> uids) {
        PendingCommand command = PendingMoveOrCopy.create(srcFolder, destFolder, isCopy, uids);
        queuePendingCommand(account, command);
    }

    private void queueMoveOrCopy(Account account, String srcFolder, String destFolder,
            boolean isCopy, List<String> uids, Map<String, String> uidMap) {
        if (uidMap == null || uidMap.isEmpty()) {
            queueMoveOrCopy(account, srcFolder, destFolder, isCopy, uids);
        } else {
            PendingCommand command = PendingMoveOrCopy.create(srcFolder, destFolder, isCopy, uidMap);
            queuePendingCommand(account, command);
        }
    }

    void processPendingMoveOrCopy(PendingMoveOrCopy command, Account account) throws MessagingException {
        String srcFolder = command.srcFolder;
        String destFolder = command.destFolder;
        boolean isCopy = command.isCopy;

        Map<String, String> newUidMap = command.newUidMap;
        List<String> uids = newUidMap != null ? new ArrayList<>(newUidMap.keySet()) : command.uids;

        processPendingMoveOrCopy(account, srcFolder, destFolder, uids, isCopy, newUidMap);
    }

    @VisibleForTesting
    void processPendingMoveOrCopy(Account account, String srcFolder, String destFolder, List<String> uids,
            boolean isCopy, Map<String, String> newUidMap) throws MessagingException {
        LocalFolder localDestFolder;

        LocalStore localStore = account.getLocalStore();
        localDestFolder = localStore.getFolder(destFolder);

        Backend backend = getBackend(account);

        Map<String, String> remoteUidMap;
        if (isCopy) {
            remoteUidMap = backend.copyMessages(srcFolder, destFolder, uids);
        } else {
            remoteUidMap = backend.moveMessages(srcFolder, destFolder, uids);
        }

        if (!isCopy && backend.getSupportsExpunge() && account.getExpungePolicy() == Expunge.EXPUNGE_IMMEDIATELY) {
            Timber.i("processingPendingMoveOrCopy expunging folder %s:%s", account.getDescription(), srcFolder);
            backend.expungeMessages(srcFolder, uids);
        }

        /*
         * This next part is used to bring the local UIDs of the local destination folder
         * upto speed with the remote UIDs of remote destination folder.
         */
        if (newUidMap != null && remoteUidMap != null && !remoteUidMap.isEmpty()) {
            Timber.i("processingPendingMoveOrCopy: changing local uids of %d messages", remoteUidMap.size());
            for (Entry<String, String> entry : remoteUidMap.entrySet()) {
                String remoteSrcUid = entry.getKey();
                String newUid = entry.getValue();
                String localDestUid = newUidMap.get(remoteSrcUid);
                if (localDestUid == null) {
                    continue;
                }

                Message localDestMessage = localDestFolder.getMessage(localDestUid);
                if (localDestMessage != null) {
                    localDestMessage.setUid(newUid);
                    localDestFolder.changeUid((LocalMessage) localDestMessage);
                    for (MessagingListener l : getListeners()) {
                        l.messageUidChanged(account, destFolder, localDestUid, newUid);
                    }
                }
            }
        }
    }

    private void queueSetFlag(final Account account, final String folderServerId,
            final boolean newState, final Flag flag, final List<String> uids) {
        putBackground("queueSetFlag " + account.getDescription() + ":" + folderServerId, null, new Runnable() {
            @Override
            public void run() {
                PendingCommand command = PendingSetFlag.create(folderServerId, newState, flag, uids);
                queuePendingCommand(account, command);
                processPendingCommands(account);
            }
        });
    }

    /**
     * Processes a pending mark read or unread command.
     */
    void processPendingSetFlag(PendingSetFlag command, Account account) throws MessagingException {
        Backend backend = getBackend(account);
        backend.setFlag(command.folder, command.uids, command.flag, command.newState);
    }

    private void queueExpunge(final Account account, final String folderServerId) {
        putBackground("queueExpunge " + account.getDescription() + ":" + folderServerId, null, new Runnable() {
            @Override
            public void run() {
                PendingCommand command = PendingExpunge.create(folderServerId);
                queuePendingCommand(account, command);
                processPendingCommands(account);
            }
        });
    }

    void processPendingExpunge(PendingExpunge command, Account account) throws MessagingException {
        Backend backend = getBackend(account);
        backend.expunge(command.folder);
    }

    void processPendingMarkAllAsRead(PendingMarkAllAsRead command, Account account) throws MessagingException {
        String folder = command.folder;
        LocalFolder localFolder = null;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folder);
            localFolder.open(Folder.OPEN_MODE_RW);
            List<? extends Message> messages = localFolder.getMessages(null, false);
            for (Message message : messages) {
                if (!message.isSet(Flag.SEEN)) {
                    message.setFlag(Flag.SEEN, true);
                }
            }

            for (MessagingListener l : getListeners()) {
                l.folderStatusChanged(account, folder, 0);
            }
        } finally {
            closeFolder(localFolder);
        }

        Backend backend = getBackend(account);
        if (backend.getSupportsSeenFlag()) {
            backend.markAllAsRead(folder);
        }
    }

    public void markAllMessagesRead(final Account account, final String folder) {
        Timber.i("Marking all messages in %s:%s as read", account.getDescription(), folder);

        PendingCommand command = PendingMarkAllAsRead.create(folder);
        queuePendingCommand(account, command);
        processPendingCommands(account);
    }

    public void setFlag(final Account account, final List<Long> messageIds, final Flag flag,
            final boolean newState) {

        setFlagInCache(account, messageIds, flag, newState);

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                setFlagSynchronous(account, messageIds, flag, newState, false);
            }
        });
    }

    public void setFlagForThreads(final Account account, final List<Long> threadRootIds,
            final Flag flag, final boolean newState) {

        setFlagForThreadsInCache(account, threadRootIds, flag, newState);

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                setFlagSynchronous(account, threadRootIds, flag, newState, true);
            }
        });
    }

    private void setFlagSynchronous(final Account account, final List<Long> ids,
            final Flag flag, final boolean newState, final boolean threadedList) {

        LocalStore localStore;
        try {
            localStore = account.getLocalStore();
        } catch (MessagingException e) {
            Timber.e(e, "Couldn't get LocalStore instance");
            return;
        }

        // Update affected messages in the database. This should be as fast as possible so the UI
        // can be updated with the new state.
        try {
            if (threadedList) {
                localStore.setFlagForThreads(ids, flag, newState);
                removeFlagForThreadsFromCache(account, ids, flag);
            } else {
                localStore.setFlag(ids, flag, newState);
                removeFlagFromCache(account, ids, flag);
            }
        } catch (MessagingException e) {
            Timber.e(e, "Couldn't set flags in local database");
        }

        // Read folder name and UID of messages from the database
        Map<String, List<String>> folderMap;
        try {
            folderMap = localStore.getFoldersAndUids(ids, threadedList);
        } catch (MessagingException e) {
            Timber.e(e, "Couldn't get folder name and UID of messages");
            return;
        }

        // Loop over all folders
        for (Entry<String, List<String>> entry : folderMap.entrySet()) {
            String folderServerId = entry.getKey();

            // Notify listeners of changed folder status
            LocalFolder localFolder = localStore.getFolder(folderServerId);
            try {
                int unreadMessageCount = localFolder.getUnreadMessageCount();
                for (MessagingListener l : getListeners()) {
                    l.folderStatusChanged(account, folderServerId, unreadMessageCount);
                }
            } catch (MessagingException e) {
                Timber.w(e, "Couldn't get unread count for folder: %s", folderServerId);
            }

            // TODO: Skip the remote part for all local-only folders

            // Send flag change to server
            queueSetFlag(account, folderServerId, newState, flag, entry.getValue());
            processPendingCommands(account);
        }
    }

    /**
     * Set or remove a flag for a set of messages in a specific folder.
     * <p>
     * <p>
     * The {@link Message} objects passed in are updated to reflect the new flag state.
     * </p>
     *
     * @param account
     *         The account the folder containing the messages belongs to.
     * @param folderServerId
     *         The server ID of the folder.
     * @param messages
     *         The messages to change the flag for.
     * @param flag
     *         The flag to change.
     * @param newState
     *         {@code true}, if the flag should be set. {@code false} if it should be removed.
     */
    public void setFlag(Account account, String folderServerId, List<? extends Message> messages, Flag flag,
            boolean newState) {
        // TODO: Put this into the background, but right now some callers depend on the message
        //       objects being modified right after this method returns.
        Folder localFolder = null;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folderServerId);
            localFolder.open(Folder.OPEN_MODE_RW);

            // Allows for re-allowing sending of messages that could not be sent
            if (flag == Flag.FLAGGED && !newState &&
                    account.getOutboxFolder().equals(folderServerId)) {
                for (Message message : messages) {
                    String uid = message.getUid();
                    if (uid != null) {
                        sendCount.remove(uid);
                    }
                }
            }

            // Update the messages in the local store
            localFolder.setFlags(messages, Collections.singleton(flag), newState);

            int unreadMessageCount = localFolder.getUnreadMessageCount();
            for (MessagingListener l : getListeners()) {
                l.folderStatusChanged(account, folderServerId, unreadMessageCount);
            }


            /*
             * Handle the remote side
             */

            // TODO: Skip the remote part for all local-only folders

            List<String> uids = getUidsFromMessages(messages);
            queueSetFlag(account, folderServerId, newState, flag, uids);
            processPendingCommands(account);
        } catch (MessagingException me) {
            throw new RuntimeException(me);
        } finally {
            closeFolder(localFolder);
        }
    }

    /**
     * Set or remove a flag for a message referenced by message UID.
     *
     * @param account
     *         The account the folder containing the message belongs to.
     * @param folderServerId
     *         The server ID of the folder.
     * @param uid
     *         The UID of the message to change the flag for.
     * @param flag
     *         The flag to change.
     * @param newState
     *         {@code true}, if the flag should be set. {@code false} if it should be removed.
     */
    public void setFlag(Account account, String folderServerId, String uid, Flag flag,
            boolean newState) {
        Folder localFolder = null;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folderServerId);
            localFolder.open(Folder.OPEN_MODE_RW);

            Message message = localFolder.getMessage(uid);
            if (message != null) {
                setFlag(account, folderServerId, Collections.singletonList(message), flag, newState);
            }
        } catch (MessagingException me) {
            throw new RuntimeException(me);
        } finally {
            closeFolder(localFolder);
        }
    }

    public void clearAllPending(final Account account) {
        try {
            Timber.w("Clearing pending commands!");
            LocalStore localStore = account.getLocalStore();
            localStore.removePendingCommands();
        } catch (MessagingException me) {
            Timber.e(me, "Unable to clear pending command");
        }
    }

    public void loadMessageRemotePartial(final Account account, final String folder,
            final String uid, final MessagingListener listener) {
        put("loadMessageRemotePartial", listener, new Runnable() {
            @Override
            public void run() {
                loadMessageRemoteSynchronous(account, folder, uid, listener, true);
            }
        });
    }

    //TODO: Fix the callback mess. See GH-782
    public void loadMessageRemote(final Account account, final String folder,
            final String uid, final MessagingListener listener) {
        put("loadMessageRemote", listener, new Runnable() {
            @Override
            public void run() {
                loadMessageRemoteSynchronous(account, folder, uid, listener, false);
            }
        });
    }

    private boolean loadMessageRemoteSynchronous(final Account account, final String folder,
            final String uid, final MessagingListener listener, final boolean loadPartialFromSearch) {
        LocalFolder localFolder = null;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folder);
            localFolder.open(Folder.OPEN_MODE_RW);

            LocalMessage message = localFolder.getMessage(uid);

            if (uid.startsWith(K9.LOCAL_UID_PREFIX)) {
                Timber.w("Message has local UID so cannot download fully.");
                // ASH move toast
                android.widget.Toast.makeText(context,
                        "Message has local UID so cannot download fully",
                        android.widget.Toast.LENGTH_LONG).show();
                // TODO: Using X_DOWNLOADED_FULL is wrong because it's only a partial message. But
                // one we can't download completely. Maybe add a new flag; X_PARTIAL_MESSAGE ?
                message.setFlag(Flag.X_DOWNLOADED_FULL, true);
                message.setFlag(Flag.X_DOWNLOADED_PARTIAL, false);
            } else {
                Backend backend = getBackend(account);

                if (loadPartialFromSearch) {
                    SyncConfig syncConfig = createSyncConfig(account);
                    backend.downloadMessage(syncConfig, folder, uid);
                } else {
                    FetchProfile fp = new FetchProfile();
                    fp.add(FetchProfile.Item.BODY);
                    fp.add(FetchProfile.Item.FLAGS);
                    Message remoteMessage = backend.fetchMessage(folder, uid, fp);
                    localFolder.appendMessages(Collections.singletonList(remoteMessage));
                }

                message = localFolder.getMessage(uid);

                if (!loadPartialFromSearch) {
                    message.setFlag(Flag.X_DOWNLOADED_FULL, true);
                }
            }

            // now that we have the full message, refresh the headers
            for (MessagingListener l : getListeners(listener)) {
                l.loadMessageRemoteFinished(account, folder, uid);
            }

            return true;
        } catch (Exception e) {
            for (MessagingListener l : getListeners(listener)) {
                l.loadMessageRemoteFailed(account, folder, uid, e);
            }
            notifyUserIfCertificateProblem(account, e, true);
            Timber.e(e, "Error while loading remote message");
            return false;
        } finally {
            closeFolder(localFolder);
        }
    }

    public LocalMessage loadMessage(Account account, String folderServerId, String uid) throws MessagingException {
        LocalStore localStore = account.getLocalStore();
        LocalFolder localFolder = localStore.getFolder(folderServerId);
        localFolder.open(Folder.OPEN_MODE_RW);

        LocalMessage message = localFolder.getMessage(uid);
        if (message == null || message.getDatabaseId() == 0) {
            throw new IllegalArgumentException("Message not found: folder=" + folderServerId + ", uid=" + uid);
        }

        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY);
        localFolder.fetch(Collections.singletonList(message), fp, null);
        localFolder.close();

        notificationController.removeNewMailNotification(account, message.makeMessageReference());
        markMessageAsReadOnView(account, message);

        return message;
    }

    public LocalMessage loadMessageMetadata(Account account, String folderServerId, String uid) throws MessagingException {
        LocalStore localStore = account.getLocalStore();
        LocalFolder localFolder = localStore.getFolder(folderServerId);
        localFolder.open(Folder.OPEN_MODE_RW);

        LocalMessage message = localFolder.getMessage(uid);
        if (message == null || message.getDatabaseId() == 0) {
            throw new IllegalArgumentException("Message not found: folder=" + folderServerId + ", uid=" + uid);
        }

        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.ENVELOPE);
        localFolder.fetch(Collections.singletonList(message), fp, null);
        localFolder.close();

        return message;
    }

    private void markMessageAsReadOnView(Account account, LocalMessage message)
            throws MessagingException {

        if (account.isMarkMessageAsReadOnView() && !message.isSet(Flag.SEEN)) {
            List<Long> messageIds = Collections.singletonList(message.getDatabaseId());
            setFlag(account, messageIds, Flag.SEEN, true);

            message.setFlagInternal(Flag.SEEN, true);
        }
    }

    public void loadAttachment(final Account account, final LocalMessage message, final Part part,
            final MessagingListener listener) {

        put("loadAttachment", listener, new Runnable() {
            @Override
            public void run() {
                LocalFolder localFolder = null;
                try {
                    String folderServerId = message.getFolder().getServerId();

                    LocalStore localStore = account.getLocalStore();
                    localFolder = localStore.getFolder(folderServerId);

                    ProgressBodyFactory bodyFactory = new ProgressBodyFactory(new ProgressListener() {
                        @Override
                        public void updateProgress(int progress) {
                            for (MessagingListener listener : getListeners()) {
                                listener.updateProgress(progress);
                            }
                        }
                    });

                    Backend backend = getBackend(account);
                    backend.fetchPart(folderServerId, message.getUid(), part, bodyFactory);

                    localFolder.addPartToMessage(message, part);

                    for (MessagingListener l : getListeners(listener)) {
                        l.loadAttachmentFinished(account, message, part);
                    }
                } catch (MessagingException me) {
                    Timber.v(me, "Exception loading attachment");

                    for (MessagingListener l : getListeners(listener)) {
                        l.loadAttachmentFailed(account, message, part, me.getMessage());
                    }
                    notifyUserIfCertificateProblem(account, me, true);
                } finally {
                    closeFolder(localFolder);
                }
            }
        });
    }

    /**
     * Stores the given message in the Outbox and starts a sendPendingMessages command to
     * attempt to send the message.
     */
    public void sendMessage(final Account account,
            final Message message,
            MessagingListener listener) {
        try {
            LocalStore localStore = account.getLocalStore();
            LocalFolder localFolder = localStore.getFolder(account.getOutboxFolder());
            localFolder.open(Folder.OPEN_MODE_RW);
            localFolder.appendMessages(Collections.singletonList(message));
            Message localMessage = localFolder.getMessage(message.getUid());
            localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);
            localFolder.close();
            sendPendingMessages(account, listener);
        } catch (Exception e) {
            /*
            for (MessagingListener l : getListeners())
            {
                // TODO general failed
            }
            */
            Timber.e(e, "Error sending message");

        }
    }

    public void sendMessageBlocking(Account account, Message message) throws MessagingException {
        Backend backend = getBackend(account);
        backend.sendMessage(message);
    }

    public void sendPendingMessages(MessagingListener listener) {
        final Preferences prefs = Preferences.getPreferences(context);
        for (Account account : prefs.getAvailableAccounts()) {
            sendPendingMessages(account, listener);
        }
    }


    /**
     * Attempt to send any messages that are sitting in the Outbox.
     */
    public void sendPendingMessages(final Account account,
            MessagingListener listener) {
        putBackground("sendPendingMessages", listener, new Runnable() {
            @Override
            public void run() {
                if (!account.isAvailable(context)) {
                    throw new UnavailableAccountException();
                }
                if (messagesPendingSend(account)) {

                    showSendingNotificationIfNecessary(account);

                    try {
                        sendPendingMessagesSynchronous(account);
                    } finally {
                        clearSendingNotificationIfNecessary(account);
                    }
                }
            }
        });
    }

    private void showSendingNotificationIfNecessary(Account account) {
        if (account.isShowOngoing()) {
            notificationController.showSendingNotification(account);
        }
    }

    private void clearSendingNotificationIfNecessary(Account account) {
        if (account.isShowOngoing()) {
            notificationController.clearSendingNotification(account);
        }
    }

    private boolean messagesPendingSend(final Account account) {
        Folder localFolder = null;
        try {
            localFolder = account.getLocalStore().getFolder(
                    account.getOutboxFolder());
            if (!localFolder.exists()) {
                return false;
            }

            localFolder.open(Folder.OPEN_MODE_RW);

            if (localFolder.getMessageCount() > 0) {
                return true;
            }
        } catch (Exception e) {
            Timber.e(e, "Exception while checking for unsent messages");
        } finally {
            closeFolder(localFolder);
        }
        return false;
    }

    /**
     * Attempt to send any messages that are sitting in the Outbox.
     */
    @VisibleForTesting
    protected void sendPendingMessagesSynchronous(final Account account) {
        LocalFolder localFolder = null;
        Exception lastFailure = null;
        boolean wasPermanentFailure = false;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(
                    account.getOutboxFolder());
            if (!localFolder.exists()) {
                Timber.v("Outbox does not exist");
                return;
            }
            for (MessagingListener l : getListeners()) {
                l.sendPendingMessagesStarted(account);
            }
            localFolder.open(Folder.OPEN_MODE_RW);

            List<LocalMessage> localMessages = localFolder.getMessages(null);
            int progress = 0;
            int todo = localMessages.size();
            for (MessagingListener l : getListeners()) {
                l.synchronizeMailboxProgress(account, account.getSentFolder(), progress, todo);
            }
            /*
             * The profile we will use to pull all of the content
             * for a given local message into memory for sending.
             */
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.BODY);

            Timber.i("Scanning folder '%s' (%d) for messages to send",
                    account.getOutboxFolder(), localFolder.getDatabaseId());

            Backend backend = getBackend(account);

            for (LocalMessage message : localMessages) {
                if (message.isSet(Flag.DELETED)) {
                    message.destroy();
                    continue;
                }
                try {
                    AtomicInteger count = new AtomicInteger(0);
                    AtomicInteger oldCount = sendCount.putIfAbsent(message.getUid(), count);
                    if (oldCount != null) {
                        count = oldCount;
                    }

                    Timber.i("Send count for message %s is %d", message.getUid(), count.get());

                    if (count.incrementAndGet() > K9.MAX_SEND_ATTEMPTS) {
                        Timber.e("Send count for message %s can't be delivered after %d attempts. " +
                                "Giving up until the user restarts the device", message.getUid(), MAX_SEND_ATTEMPTS);
                        notificationController.showSendFailedNotification(account,
                                new MessagingException(message.getSubject()));
                        continue;
                    }

                    localFolder.fetch(Collections.singletonList(message), fp, null);
                    try {
                        if (message.getHeader(K9.IDENTITY_HEADER).length > 0) {
                            Timber.v("The user has set the Outbox and Drafts folder to the same thing. " +
                                    "This message appears to be a draft, so K-9 will not send it");
                            continue;
                        }

                        message.setFlag(Flag.X_SEND_IN_PROGRESS, true);

                        Timber.i("Sending message with UID %s", message.getUid());
                        backend.sendMessage(message);

                        message.setFlag(Flag.X_SEND_IN_PROGRESS, false);
                        message.setFlag(Flag.SEEN, true);
                        progress++;
                        for (MessagingListener l : getListeners()) {
                            l.synchronizeMailboxProgress(account, account.getSentFolder(), progress, todo);
                        }
                        moveOrDeleteSentMessage(account, localStore, localFolder, message);
                    } catch (AuthenticationFailedException e) {
                        lastFailure = e;
                        wasPermanentFailure = false;

                        handleAuthenticationFailure(account, false);
                        handleSendFailure(account, localStore, localFolder, message, e, wasPermanentFailure);
                    } catch (CertificateValidationException e) {
                        lastFailure = e;
                        wasPermanentFailure = false;

                        notifyUserIfCertificateProblem(account, e, false);
                        handleSendFailure(account, localStore, localFolder, message, e, wasPermanentFailure);
                    } catch (MessagingException e) {
                        lastFailure = e;
                        wasPermanentFailure = e.isPermanentFailure();

                        handleSendFailure(account, localStore, localFolder, message, e, wasPermanentFailure);
                    } catch (Exception e) {
                        lastFailure = e;
                        wasPermanentFailure = true;

                        handleSendFailure(account, localStore, localFolder, message, e, wasPermanentFailure);
                    }
                } catch (Exception e) {
                    lastFailure = e;
                    wasPermanentFailure = false;
                    Timber.e(e, "Failed to fetch message for sending");
                    notifySynchronizeMailboxFailed(account, localFolder, e);
                }
            }

            for (MessagingListener l : getListeners()) {
                l.sendPendingMessagesCompleted(account);
            }

            if (lastFailure != null) {
                if (wasPermanentFailure) {
                    notificationController.showSendFailedNotification(account, lastFailure);
                } else {
                    notificationController.showSendFailedNotification(account, lastFailure);
                }
            }
        } catch (UnavailableStorageException e) {
            Timber.i("Failed to send pending messages because storage is not available - trying again later.");
            throw new UnavailableAccountException(e);
        } catch (Exception e) {
            Timber.v(e, "Failed to send pending messages");

            for (MessagingListener l : getListeners()) {
                l.sendPendingMessagesFailed(account);
            }
        } finally {
            if (lastFailure == null) {
                notificationController.clearSendFailedNotification(account);
            }
            closeFolder(localFolder);
        }
    }

    private void moveOrDeleteSentMessage(Account account, LocalStore localStore,
            LocalFolder localFolder, LocalMessage message) throws MessagingException {
        if (!account.hasSentFolder() || !account.isUploadSentMessages()) {
            Timber.i("Not uploading sent message; deleting local message");
            message.setFlag(Flag.DELETED, true);
        } else {
            LocalFolder localSentFolder = localStore.getFolder(account.getSentFolder());
            Timber.i("Moving sent message to folder '%s' (%d)", account.getSentFolder(), localSentFolder.getDatabaseId());

            localFolder.moveMessages(Collections.singletonList(message), localSentFolder);

            Timber.i("Moved sent message to folder '%s' (%d)", account.getSentFolder(), localSentFolder.getDatabaseId());

            PendingCommand command = PendingAppend.create(localSentFolder.getServerId(), message.getUid());
            queuePendingCommand(account, command);
            processPendingCommands(account);
        }
    }

    private void handleSendFailure(Account account, LocalStore localStore, Folder localFolder, Message message,
            Exception exception, boolean permanentFailure) throws MessagingException {

        Timber.e(exception, "Failed to send message");

        if (permanentFailure) {
            moveMessageToDraftsFolder(account, localFolder, localStore, message);
        }

        message.setFlag(Flag.X_SEND_FAILED, true);

        notifySynchronizeMailboxFailed(account, localFolder, exception);
    }

    private void moveMessageToDraftsFolder(Account account, Folder localFolder, LocalStore localStore, Message message)
            throws MessagingException {
        if (!account.hasDraftsFolder()) {
            Timber.d("Can't move message to Drafts folder. No Drafts folder configured.");
            return;
        }

        LocalFolder draftsFolder = localStore.getFolder(account.getDraftsFolder());
        localFolder.moveMessages(Collections.singletonList(message), draftsFolder);
    }

    private void notifySynchronizeMailboxFailed(Account account, Folder localFolder, Exception exception) {
        String folderServerId = localFolder.getServerId();
        String errorMessage = getRootCauseMessage(exception);
        for (MessagingListener listener : getListeners()) {
            listener.synchronizeMailboxFailed(account, folderServerId, errorMessage);
        }
    }

    public void getAccountStats(final Context context, final Account account,
            final MessagingListener listener) {

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    AccountStats stats = getAccountStats(account);
                    listener.accountStatusChanged(account, stats);
                } catch (MessagingException me) {
                    Timber.e(me, "Count not get unread count for account %s", account.getDescription());
                }

            }
        });
    }

    public AccountStats getAccountStats(Account account) throws MessagingException {
        return accountStatsCollector.getStats(account);
    }

    public void getSearchAccountStats(final SearchAccount searchAccount, final MessagingListener listener) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                getSearchAccountStatsSynchronous(searchAccount, listener);
            }
        });
    }

    public AccountStats getSearchAccountStatsSynchronous(SearchAccount searchAccount, MessagingListener listener) {
        AccountStats stats = accountStatsCollector.getSearchAccountStats(searchAccount);

        if (listener != null) {
            listener.accountStatusChanged(searchAccount, stats);
        }

        return stats;
    }

    public void getFolderUnreadMessageCount(final Account account, final String folderServerId,
            final MessagingListener l) {
        Runnable unreadRunnable = new Runnable() {
            @Override
            public void run() {

                int unreadMessageCount = 0;
                try {
                    unreadMessageCount = getFolderUnreadMessageCount(account, folderServerId);
                } catch (MessagingException me) {
                    Timber.e(me, "Count not get unread count for account %s", account.getDescription());
                }
                l.folderStatusChanged(account, folderServerId, unreadMessageCount);
            }
        };


        put("getFolderUnread:" + account.getDescription() + ":" + folderServerId, l, unreadRunnable);
    }

    public int getFolderUnreadMessageCount(Account account, String folderServerId) throws MessagingException {
        LocalStore localStore = account.getLocalStore();
        Folder localFolder = localStore.getFolder(folderServerId);
        return localFolder.getUnreadMessageCount();
    }

    public boolean isMoveCapable(MessageReference messageReference) {
        return !messageReference.getUid().startsWith(K9.LOCAL_UID_PREFIX);
    }

    public boolean isCopyCapable(MessageReference message) {
        return isMoveCapable(message);
    }

    public boolean isMoveCapable(final Account account) {
        return getBackend(account).getSupportsMove();
    }

    public boolean isCopyCapable(final Account account) {
        return getBackend(account).getSupportsCopy();
    }

    public boolean isPushCapable(Account account) {
        return getBackend(account).isPushCapable();
    }

    public boolean supportsSeenFlag(Account account) {
        return getBackend(account).getSupportsSeenFlag();
    }

    public boolean supportsExpunge(Account account) {
        return getBackend(account).getSupportsExpunge();
    }

    public boolean supportsSearchByDate(Account account) {
        return getBackend(account).getSupportsSearchByDate();
    }

    public boolean supportsUpload(Account account) {
        return getBackend(account).getSupportsUpload();
    }

    public void checkIncomingServerSettings(Account account) throws MessagingException {
        getBackend(account).checkIncomingServerSettings();
    }

    public void checkOutgoingServerSettings(Account account) throws MessagingException {
        getBackend(account).checkOutgoingServerSettings();
    }

    public void moveMessages(final Account srcAccount, final String srcFolder,
            List<MessageReference> messageReferences, final String destFolder) {
        actOnMessageGroup(srcAccount, srcFolder, messageReferences, new MessageActor() {
            @Override
            public void act(final Account account, LocalFolder messageFolder, final List<LocalMessage> messages) {
                suppressMessages(account, messages);

                putBackground("moveMessages", null, new Runnable() {
                    @Override
                    public void run() {
                        moveOrCopyMessageSynchronous(account, srcFolder, messages, destFolder, false);
                    }
                });
            }
        });
    }

    public void moveMessagesInThread(Account srcAccount, final String srcFolder,
            final List<MessageReference> messageReferences, final String destFolder) {
        actOnMessageGroup(srcAccount, srcFolder, messageReferences, new MessageActor() {
            @Override
            public void act(final Account account, LocalFolder messageFolder, final List<LocalMessage> messages) {
                suppressMessages(account, messages);

                putBackground("moveMessagesInThread", null, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            List<Message> messagesInThreads = collectMessagesInThreads(account, messages);
                            moveOrCopyMessageSynchronous(account, srcFolder, messagesInThreads, destFolder, false);
                        } catch (MessagingException e) {
                            Timber.e(e, "Exception while moving messages");
                        }
                    }
                });
            }
        });
    }

    public void moveMessage(final Account account, final String srcFolder, final MessageReference message,
            final String destFolder) {
        moveMessages(account, srcFolder, Collections.singletonList(message), destFolder);
    }

    public void copyMessages(final Account srcAccount, final String srcFolder,
            final List<MessageReference> messageReferences, final String destFolder) {
        actOnMessageGroup(srcAccount, srcFolder, messageReferences, new MessageActor() {
            @Override
            public void act(final Account account, LocalFolder messageFolder, final List<LocalMessage> messages) {
                putBackground("copyMessages", null, new Runnable() {
                    @Override
                    public void run() {
                        moveOrCopyMessageSynchronous(srcAccount, srcFolder, messages, destFolder, true);
                    }
                });
            }
        });
    }

    public void copyMessagesInThread(Account srcAccount, final String srcFolder,
            final List<MessageReference> messageReferences, final String destFolder) {
        actOnMessageGroup(srcAccount, srcFolder, messageReferences, new MessageActor() {
            @Override
            public void act(final Account account, LocalFolder messageFolder, final List<LocalMessage> messages) {
                putBackground("copyMessagesInThread", null, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            List<Message> messagesInThreads = collectMessagesInThreads(account, messages);
                            moveOrCopyMessageSynchronous(account, srcFolder, messagesInThreads, destFolder,
                                    true);
                        } catch (MessagingException e) {
                            Timber.e(e, "Exception while copying messages");
                        }
                    }
                });
            }
        });
    }

    public void copyMessage(final Account account, final String srcFolder, final MessageReference message,
            final String destFolder) {

        copyMessages(account, srcFolder, Collections.singletonList(message), destFolder);
    }

    private void moveOrCopyMessageSynchronous(final Account account, final String srcFolder,
            final List<? extends Message> inMessages, final String destFolder, final boolean isCopy) {

        try {
            LocalStore localStore = account.getLocalStore();
            if (!isCopy && !isMoveCapable(account)) {
                return;
            }
            if (isCopy && !isCopyCapable(account)) {
                return;
            }

            LocalFolder localSrcFolder = localStore.getFolder(srcFolder);
            Folder localDestFolder = localStore.getFolder(destFolder);

            boolean unreadCountAffected = false;
            List<String> uids = new LinkedList<>();
            for (Message message : inMessages) {
                String uid = message.getUid();
                if (!uid.startsWith(K9.LOCAL_UID_PREFIX)) {
                    uids.add(uid);
                }

                if (!unreadCountAffected && !message.isSet(Flag.SEEN)) {
                    unreadCountAffected = true;
                }
            }

            List<LocalMessage> messages = localSrcFolder.getMessagesByUids(uids);
            if (messages.size() > 0) {
                Map<String, Message> origUidMap = new HashMap<>();

                for (Message message : messages) {
                    origUidMap.put(message.getUid(), message);
                }

                Timber.i("moveOrCopyMessageSynchronous: source folder = %s, %d messages, destination folder = %s, " +
                        "isCopy = %s", srcFolder, messages.size(), destFolder, isCopy);

                Map<String, String> uidMap;

                if (isCopy) {
                    FetchProfile fp = new FetchProfile();
                    fp.add(Item.ENVELOPE);
                    fp.add(Item.BODY);
                    localSrcFolder.fetch(messages, fp, null);
                    uidMap = localSrcFolder.copyMessages(messages, localDestFolder);

                    if (unreadCountAffected) {
                        // If this copy operation changes the unread count in the destination
                        // folder, notify the listeners.
                        int unreadMessageCount = localDestFolder.getUnreadMessageCount();
                        for (MessagingListener l : getListeners()) {
                            l.folderStatusChanged(account, destFolder, unreadMessageCount);
                        }
                    }
                } else {
                    uidMap = localSrcFolder.moveMessages(messages, localDestFolder);
                    for (Entry<String, Message> entry : origUidMap.entrySet()) {
                        String origUid = entry.getKey();
                        Message message = entry.getValue();
                        for (MessagingListener l : getListeners()) {
                            l.messageUidChanged(account, srcFolder, origUid, message.getUid());
                        }
                    }
                    unsuppressMessages(account, messages);

                    if (unreadCountAffected) {
                        // If this move operation changes the unread count, notify the listeners
                        // that the unread count changed in both the source and destination folder.
                        int unreadMessageCountSrc = localSrcFolder.getUnreadMessageCount();
                        int unreadMessageCountDest = localDestFolder.getUnreadMessageCount();
                        for (MessagingListener l : getListeners()) {
                            l.folderStatusChanged(account, srcFolder, unreadMessageCountSrc);
                            l.folderStatusChanged(account, destFolder, unreadMessageCountDest);
                        }
                    }
                }

                List<String> origUidKeys = new ArrayList<>(origUidMap.keySet());
                queueMoveOrCopy(account, srcFolder, destFolder, isCopy, origUidKeys, uidMap);
            }

            processPendingCommands(account);
        } catch (UnavailableStorageException e) {
            Timber.i("Failed to move/copy message because storage is not available - trying again later.");
            throw new UnavailableAccountException(e);
        } catch (MessagingException me) {
            throw new RuntimeException("Error moving message", me);
        }
    }

    public void expunge(final Account account, final String folder) {
        putBackground("expunge", null, new Runnable() {
            @Override
            public void run() {
                queueExpunge(account, folder);
            }
        });
    }

    public void deleteDraft(final Account account, long id) {
        LocalFolder localFolder = null;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(account.getDraftsFolder());
            localFolder.open(Folder.OPEN_MODE_RW);
            String uid = localFolder.getMessageUidById(id);
            if (uid != null) {
                MessageReference messageReference = new MessageReference(
                        account.getUuid(), account.getDraftsFolder(), uid, null);
                deleteMessage(messageReference, null);
            }
        } catch (MessagingException me) {
            Timber.e(me, "Error deleting draft");
        } finally {
            closeFolder(localFolder);
        }
    }

    public void deleteThreads(final List<MessageReference> messages) {
        actOnMessagesGroupedByAccountAndFolder(messages, new MessageActor() {
            @Override
            public void act(final Account account, final LocalFolder messageFolder,
                    final List<LocalMessage> accountMessages) {
                suppressMessages(account, accountMessages);

                putBackground("deleteThreads", null, new Runnable() {
                    @Override
                    public void run() {
                        deleteThreadsSynchronous(account, messageFolder.getServerId(), accountMessages);
                    }
                });
            }
        });
    }

    private void deleteThreadsSynchronous(Account account, String folderServerId, List<? extends Message> messages) {
        try {
            List<Message> messagesToDelete = collectMessagesInThreads(account, messages);

            deleteMessagesSynchronous(account, folderServerId,
                    messagesToDelete, null);
        } catch (MessagingException e) {
            Timber.e(e, "Something went wrong while deleting threads");
        }
    }

    private static List<Message> collectMessagesInThreads(Account account, List<? extends Message> messages)
            throws MessagingException {

        LocalStore localStore = account.getLocalStore();

        List<Message> messagesInThreads = new ArrayList<>();
        for (Message message : messages) {
            LocalMessage localMessage = (LocalMessage) message;
            long rootId = localMessage.getRootId();
            long threadId = (rootId == -1) ? localMessage.getThreadId() : rootId;

            List<? extends Message> messagesInThread = localStore.getMessagesInThread(threadId);

            messagesInThreads.addAll(messagesInThread);
        }

        return messagesInThreads;
    }

    public void deleteMessage(MessageReference message, final MessagingListener listener) {
        deleteMessages(Collections.singletonList(message), listener);
    }

    public void deleteMessages(List<MessageReference> messages, final MessagingListener listener) {
        actOnMessagesGroupedByAccountAndFolder(messages, new MessageActor() {

            @Override
            public void act(final Account account, final LocalFolder messageFolder,
                    final List<LocalMessage> accountMessages) {
                suppressMessages(account, accountMessages);

                putBackground("deleteMessages", null, new Runnable() {
                    @Override
                    public void run() {
                        deleteMessagesSynchronous(account, messageFolder.getServerId(), accountMessages, listener);
                    }
                });
            }

        });
    }

    @SuppressLint("NewApi") // used for debugging only
    public void debugClearMessagesLocally(final List<MessageReference> messages) {
        if (!BuildConfig.DEBUG) {
            throw new AssertionError("method must only be used in debug build!");
        }

        actOnMessagesGroupedByAccountAndFolder(messages, new MessageActor() {

            @Override
            public void act(final Account account, final LocalFolder messageFolder,
                    final List<LocalMessage> accountMessages) {

                putBackground("debugClearLocalMessages", null, new Runnable() {
                    @Override
                    public void run() {
                        for (LocalMessage message : accountMessages) {
                            try {
                                message.debugClearLocalData();
                            } catch (MessagingException e) {
                                throw new AssertionError("clearing local message content failed!", e);
                            }
                        }
                    }
                });
            }
        });

    }

    private void deleteMessagesSynchronous(final Account account, final String folder,
            final List<? extends Message> messages,
            MessagingListener listener) {
        LocalFolder localFolder = null;
        LocalFolder localTrashFolder = null;
        try {
            //We need to make these callbacks before moving the messages to the trash
            //as messages get a new UID after being moved
            for (Message message : messages) {
                String messageServerId = message.getUid();
                for (MessagingListener l : getListeners(listener)) {
                    l.messageDeleted(account, folder, messageServerId);
                }
            }

            List<Message> localOnlyMessages = new ArrayList<>();
            List<Message> syncedMessages = new ArrayList<>();
            List<String> syncedMessageUids = new ArrayList<>();
            for (Message message : messages) {
                String uid = message.getUid();
                if (uid.startsWith(K9.LOCAL_UID_PREFIX)) {
                    localOnlyMessages.add(message);
                } else {
                    syncedMessages.add(message);
                    syncedMessageUids.add(uid);
                }
            }

            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folder);
            Map<String, String> uidMap = null;
            if (folder.equals(account.getTrashFolder()) || !account.hasTrashFolder()) {
                Timber.d("Deleting messages in trash folder or trash set to -None-, not copying");

                if (!localOnlyMessages.isEmpty()) {
                    localFolder.destroyMessages(localOnlyMessages);
                }
                if (!syncedMessages.isEmpty()) {
                    localFolder.setFlags(syncedMessages, Collections.singleton(Flag.DELETED), true);
                }
            } else {
                Timber.d("Deleting messages in normal folder, moving");
                localTrashFolder = localStore.getFolder(account.getTrashFolder());
                uidMap = localFolder.moveMessages(messages, localTrashFolder);
            }

            for (MessagingListener l : getListeners()) {
                l.folderStatusChanged(account, folder, localFolder.getUnreadMessageCount());
                if (localTrashFolder != null) {
                    l.folderStatusChanged(account, account.getTrashFolder(),
                            localTrashFolder.getUnreadMessageCount());
                }
            }

            Timber.d("Delete policy for account %s is %s", account.getDescription(), account.getDeletePolicy());

            if (folder.equals(account.getOutboxFolder())) {
                for (Message message : messages) {
                    // If the message was in the Outbox, then it has been copied to local Trash, and has
                    // to be copied to remote trash
                    PendingCommand command = PendingAppend.create(account.getTrashFolder(), message.getUid());
                    queuePendingCommand(account, command);
                }
                processPendingCommands(account);
            } else if (!syncedMessageUids.isEmpty()) {
                if (account.getDeletePolicy() == DeletePolicy.ON_DELETE) {
                    if (folder.equals(account.getTrashFolder())) {
                        queueSetFlag(account, folder, true, Flag.DELETED, syncedMessageUids);
                    } else {
                        queueMoveOrCopy(account, folder, account.getTrashFolder(), false,
                                    syncedMessageUids, uidMap);
                    }
                    processPendingCommands(account);
                } else if (account.getDeletePolicy() == DeletePolicy.MARK_AS_READ) {
                    queueSetFlag(account, folder, true, Flag.SEEN, syncedMessageUids);
                    processPendingCommands(account);
                } else {
                    Timber.d("Delete policy %s prevents delete from server", account.getDeletePolicy());
                }
            }

            unsuppressMessages(account, messages);
        } catch (UnavailableStorageException e) {
            Timber.i("Failed to delete message because storage is not available - trying again later.");
            throw new UnavailableAccountException(e);
        } catch (MessagingException me) {
            throw new RuntimeException("Error deleting message from local store.", me);
        } finally {
            closeFolder(localFolder);
            closeFolder(localTrashFolder);
        }
    }

    private static List<String> getUidsFromMessages(List<? extends Message> messages) {
        List<String> uids = new ArrayList<>(messages.size());
        for (int i = 0; i < messages.size(); i++) {
            uids.add(messages.get(i).getUid());
        }
        return uids;
    }

    void processPendingEmptyTrash(Account account) throws MessagingException {
        if (!account.hasTrashFolder()) {
            return;
        }

        Backend backend = getBackend(account);

        String trashFolderServerId = account.getTrashFolder();
        backend.deleteAllMessages(trashFolderServerId);

        if (account.getExpungePolicy() == Expunge.EXPUNGE_IMMEDIATELY) {
            backend.expunge(trashFolderServerId);
        }

        // When we empty trash, we need to actually synchronize the folder
        // or local deletes will never get cleaned up
        LocalStore localStore = account.getLocalStore();
        LocalFolder folder = localStore.getFolder(trashFolderServerId);
        folder.open(Folder.OPEN_MODE_RW);
        synchronizeFolder(account, folder, true, 0, null);

        compact(account, null);
    }

    public void emptyTrash(final Account account, MessagingListener listener) {
        putBackground("emptyTrash", listener, new Runnable() {
            @Override
            public void run() {
                LocalFolder localFolder = null;
                try {
                    LocalStore localStore = account.getLocalStore();
                    localFolder = localStore.getFolder(account.getTrashFolder());
                    localFolder.open(Folder.OPEN_MODE_RW);

                    boolean isTrashLocalOnly = isTrashLocalOnly(account);
                    if (isTrashLocalOnly) {
                        localFolder.clearAllMessages();
                    } else {
                        localFolder.setFlags(Collections.singleton(Flag.DELETED), true);
                    }

                    for (MessagingListener l : getListeners()) {
                        l.emptyTrashCompleted(account);
                    }

                    if (!isTrashLocalOnly) {
                        PendingCommand command = PendingEmptyTrash.create();
                        queuePendingCommand(account, command);
                        processPendingCommands(account);
                    }
                } catch (UnavailableStorageException e) {
                    Timber.i("Failed to empty trash because storage is not available - trying again later.");
                    throw new UnavailableAccountException(e);
                } catch (Exception e) {
                    Timber.e(e, "emptyTrash failed");
                } finally {
                    closeFolder(localFolder);
                }
            }
        });
    }

    public void clearFolder(final Account account, final String folderServerId, final MessagingListener listener) {
        putBackground("clearFolder", listener, new Runnable() {
            @Override
            public void run() {
                clearFolderSynchronous(account, folderServerId, listener);
            }
        });
    }

    @VisibleForTesting
    protected void clearFolderSynchronous(Account account, String folderServerId, MessagingListener listener) {
        LocalFolder localFolder = null;
        try {
            localFolder = account.getLocalStore().getFolder(folderServerId);
            localFolder.open(Folder.OPEN_MODE_RW);
            localFolder.clearAllMessages();
        } catch (UnavailableStorageException e) {
            Timber.i("Failed to clear folder because storage is not available - trying again later.");
            throw new UnavailableAccountException(e);
        } catch (Exception e) {
            Timber.e(e, "clearFolder failed");
        } finally {
            closeFolder(localFolder);
        }

        listFoldersSynchronous(account, false, listener);
    }


    /**
     * Find out whether the account type only supports a local Trash folder.
     * <p>
     * <p>Note: Currently this is only the case for POP3 accounts.</p>
     *
     * @param account
     *         The account to check.
     *
     * @return {@code true} if the account only has a local Trash folder that is not synchronized
     * with a folder on the server. {@code false} otherwise.
     */
    private boolean isTrashLocalOnly(Account account) {
        Backend backend = getBackend(account);
        return !backend.getSupportsTrashFolder();
    }

    public void sendAlternate(Context context, Account account, LocalMessage message) {
        Timber.d("Got message %s:%s:%s for sendAlternate",
                account.getDescription(), message.getFolder(), message.getUid());

        Intent msg = new Intent(Intent.ACTION_SEND);
        String quotedText = null;
        Part part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
        if (part == null) {
            part = MimeUtility.findFirstPartByMimeType(message, "text/html");
        }
        if (part != null) {
            quotedText = MessageExtractor.getTextFromPart(part);
        }
        if (quotedText != null) {
            msg.putExtra(Intent.EXTRA_TEXT, quotedText);
        }
        msg.putExtra(Intent.EXTRA_SUBJECT, message.getSubject());

        Address[] from = message.getFrom();
        String[] senders = new String[from.length];
        for (int i = 0; i < from.length; i++) {
            senders[i] = from[i].toString();
        }
        msg.putExtra(Intents.Share.EXTRA_FROM, senders);

        Address[] to = message.getRecipients(RecipientType.TO);
        String[] recipientsTo = new String[to.length];
        for (int i = 0; i < to.length; i++) {
            recipientsTo[i] = to[i].toString();
        }
        msg.putExtra(Intent.EXTRA_EMAIL, recipientsTo);

        Address[] cc = message.getRecipients(RecipientType.CC);
        String[] recipientsCc = new String[cc.length];
        for (int i = 0; i < cc.length; i++) {
            recipientsCc[i] = cc[i].toString();
        }
        msg.putExtra(Intent.EXTRA_CC, recipientsCc);

        msg.setType("text/plain");
        Intent chooserIntent = Intent.createChooser(msg, resourceProvider.sendAlternateChooserTitle());
        context.startActivity(chooserIntent);
    }

    /**
     * Checks mail for one or multiple accounts. If account is null all accounts
     * are checked.
     */
    public void checkMail(final Context context, final Account account,
            final boolean ignoreLastCheckedTime,
            final boolean useManualWakeLock,
            final MessagingListener listener) {

        TracingWakeLock twakeLock = null;
        if (useManualWakeLock) {
            TracingPowerManager pm = TracingPowerManager.getPowerManager(context);

            twakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "K9 MessagingController.checkMail");
            twakeLock.setReferenceCounted(false);
            twakeLock.acquire(K9.MANUAL_WAKE_LOCK_TIMEOUT);
        }
        final TracingWakeLock wakeLock = twakeLock;

        for (MessagingListener l : getListeners()) {
            l.checkMailStarted(context, account);
        }
        putBackground("checkMail", listener, new Runnable() {
            @Override
            public void run() {

                try {
                    Timber.i("Starting mail check");

                    Preferences prefs = Preferences.getPreferences(context);

                    Collection<Account> accounts;
                    if (account != null) {
                        accounts = new ArrayList<>(1);
                        accounts.add(account);
                    } else {
                        accounts = prefs.getAvailableAccounts();
                    }

                    for (final Account account : accounts) {
                        checkMailForAccount(context, account, ignoreLastCheckedTime, listener);
                    }

                } catch (Exception e) {
                    Timber.e(e, "Unable to synchronize mail");
                }
                putBackground("finalize sync", null, new Runnable() {
                            @Override
                            public void run() {

                                Timber.i("Finished mail sync");

                                if (wakeLock != null) {
                                    wakeLock.release();
                                }
                                for (MessagingListener l : getListeners()) {
                                    l.checkMailFinished(context, account);
                                }

                            }
                        }
                );
            }
        });
    }


    private void checkMailForAccount(final Context context, final Account account,
            final boolean ignoreLastCheckedTime,
            final MessagingListener listener) {
        if (!account.isAvailable(context)) {
            Timber.i("Skipping synchronizing unavailable account %s", account.getDescription());
            return;
        }
        final long accountInterval = account.getAutomaticCheckIntervalMinutes() * 60 * 1000;
        if (!ignoreLastCheckedTime && accountInterval <= 0) {
            Timber.i("Skipping synchronizing account %s", account.getDescription());
            return;
        }

        Timber.i("Synchronizing account %s", account.getDescription());

        account.setRingNotified(false);

        sendPendingMessages(account, listener);

        try {
            Account.FolderMode aDisplayMode = account.getFolderDisplayMode();
            Account.FolderMode aSyncMode = account.getFolderSyncMode();

            LocalStore localStore = account.getLocalStore();
            for (final Folder folder : localStore.getPersonalNamespaces(false)) {
                folder.open(Folder.OPEN_MODE_RW);

                Folder.FolderClass fDisplayClass = folder.getDisplayClass();
                Folder.FolderClass fSyncClass = folder.getSyncClass();

                if (modeMismatch(aDisplayMode, fDisplayClass)) {
                    // Never sync a folder that isn't displayed
                    /*
                    if (K9.DEBUG) {
                        Log.v(K9.LOG_TAG, "Not syncing folder " + folder.getName() +
                              " which is in display mode " + fDisplayClass + " while account is in display mode " + aDisplayMode);
                    }
                    */

                    continue;
                }

                if (modeMismatch(aSyncMode, fSyncClass)) {
                    // Do not sync folders in the wrong class
                    /*
                    if (K9.DEBUG) {
                        Log.v(K9.LOG_TAG, "Not syncing folder " + folder.getName() +
                              " which is in sync mode " + fSyncClass + " while account is in sync mode " + aSyncMode);
                    }
                    */

                    continue;
                }
                synchronizeFolder(account, folder, ignoreLastCheckedTime, accountInterval, listener);
            }
        } catch (MessagingException e) {
            Timber.e(e, "Unable to synchronize account %s", account.getName());
        } finally {
            putBackground("clear notification flag for " + account.getDescription(), null, new Runnable() {
                        @Override
                        public void run() {
                            Timber.v("Clearing notification flag for %s", account.getDescription());

                            account.setRingNotified(false);
                            try {
                                AccountStats stats = getAccountStats(account);
                                if (stats == null || stats.unreadMessageCount == 0) {
                                    notificationController.clearNewMailNotifications(account);
                                }
                            } catch (MessagingException e) {
                                Timber.e(e, "Unable to getUnreadMessageCount for account: %s", account);
                            }
                        }
                    }
            );
        }


    }


    private void synchronizeFolder(
            final Account account,
            final Folder folder,
            final boolean ignoreLastCheckedTime,
            final long accountInterval,
            final MessagingListener listener) {

        Timber.v("Folder %s was last synced @ %tc", folder.getServerId(), folder.getLastChecked());

        if (!ignoreLastCheckedTime && folder.getLastChecked() > System.currentTimeMillis() - accountInterval) {
            Timber.v("Not syncing folder %s, previously synced @ %tc which would be too recent for the account " +
                    "period", folder.getServerId(), folder.getLastChecked());
            return;
        }

        putBackground("sync" + folder.getServerId(), null, new Runnable() {
                    @Override
                    public void run() {
                        LocalFolder tLocalFolder = null;
                        try {
                            // In case multiple Commands get enqueued, don't run more than
                            // once
                            final LocalStore localStore = account.getLocalStore();
                            tLocalFolder = localStore.getFolder(folder.getServerId());
                            tLocalFolder.open(Folder.OPEN_MODE_RW);

                            if (!ignoreLastCheckedTime && tLocalFolder.getLastChecked() >
                                    (System.currentTimeMillis() - accountInterval)) {
                                Timber.v("Not running Command for folder %s, previously synced @ %tc which would " +
                                        "be too recent for the account period",
                                        folder.getServerId(), folder.getLastChecked());
                                return;
                            }
                            showFetchingMailNotificationIfNecessary(account, folder);
                            try {
                                synchronizeMailboxSynchronous(account, folder.getServerId(), listener, null);
                            } finally {
                                clearFetchingMailNotificationIfNecessary(account);
                            }
                        } catch (Exception e) {
                            Timber.e(e, "Exception while processing folder %s:%s",
                                    account.getDescription(), folder.getServerId());
                        } finally {
                            closeFolder(tLocalFolder);
                        }
                    }
                }
        );


    }

    private void showFetchingMailNotificationIfNecessary(Account account, Folder folder) {
        if (account.isShowOngoing()) {
            notificationController.showFetchingMailNotification(account, folder);
        }
    }

    private void clearFetchingMailNotificationIfNecessary(Account account) {
        if (account.isShowOngoing()) {
            notificationController.clearFetchingMailNotification(account);
        }
    }


    public void compact(final Account account, final MessagingListener ml) {
        putBackground("compact:" + account.getDescription(), ml, new Runnable() {
            @Override
            public void run() {
                try {
                    LocalStore localStore = account.getLocalStore();
                    long oldSize = localStore.getSize();
                    localStore.compact();
                    long newSize = localStore.getSize();
                    for (MessagingListener l : getListeners(ml)) {
                        l.accountSizeChanged(account, oldSize, newSize);
                    }
                } catch (UnavailableStorageException e) {
                    Timber.i("Failed to compact account because storage is not available - trying again later.");
                    throw new UnavailableAccountException(e);
                } catch (Exception e) {
                    Timber.e(e, "Failed to compact account %s", account.getDescription());
                }
            }
        });
    }

    public void clear(final Account account, final MessagingListener ml) {
        putBackground("clear:" + account.getDescription(), ml, new Runnable() {
            @Override
            public void run() {
                try {
                    LocalStore localStore = account.getLocalStore();
                    long oldSize = localStore.getSize();
                    localStore.clear();
                    localStore.resetVisibleLimits(account.getDisplayCount());
                    long newSize = localStore.getSize();
                    AccountStats stats = new AccountStats();
                    stats.size = newSize;
                    stats.unreadMessageCount = 0;
                    stats.flaggedMessageCount = 0;
                    for (MessagingListener l : getListeners(ml)) {
                        l.accountSizeChanged(account, oldSize, newSize);
                        l.accountStatusChanged(account, stats);
                    }
                } catch (UnavailableStorageException e) {
                    Timber.i("Failed to clear account because storage is not available - trying again later.");
                    throw new UnavailableAccountException(e);
                } catch (Exception e) {
                    Timber.e(e, "Failed to clear account %s", account.getDescription());
                }
            }
        });
    }

    public void recreate(final Account account, final MessagingListener ml) {
        putBackground("recreate:" + account.getDescription(), ml, new Runnable() {
            @Override
            public void run() {
                try {
                    LocalStore localStore = account.getLocalStore();
                    long oldSize = localStore.getSize();
                    localStore.recreate();
                    localStore.resetVisibleLimits(account.getDisplayCount());
                    long newSize = localStore.getSize();
                    AccountStats stats = new AccountStats();
                    stats.size = newSize;
                    stats.unreadMessageCount = 0;
                    stats.flaggedMessageCount = 0;
                    for (MessagingListener l : getListeners(ml)) {
                        l.accountSizeChanged(account, oldSize, newSize);
                        l.accountStatusChanged(account, stats);
                    }
                } catch (UnavailableStorageException e) {
                    Timber.i("Failed to recreate an account because storage is not available - trying again later.");
                    throw new UnavailableAccountException(e);
                } catch (Exception e) {
                    Timber.e(e, "Failed to recreate account %s", account.getDescription());
                }
            }
        });
    }


    public boolean shouldNotifyForMessage(Account account, LocalFolder localFolder, Message message,
            boolean isOldMessage) {
        // If we don't even have an account name, don't show the notification.
        // (This happens during initial account setup)
        if (account.getName() == null) {
            return false;
        }

        if (K9.isQuietTime() && !K9.isNotificationDuringQuietTimeEnabled()) {
            return false;
        }

        // Do not notify if the user does not have notifications enabled or if the message has
        // been read.
        if (!account.isNotifyNewMail() || message.isSet(Flag.SEEN) || isOldMessage) {
            return false;
        }

        Account.FolderMode aDisplayMode = account.getFolderDisplayMode();
        Account.FolderMode aNotifyMode = account.getFolderNotifyNewMailMode();
        Folder.FolderClass fDisplayClass = localFolder.getDisplayClass();
        Folder.FolderClass fNotifyClass = localFolder.getNotifyClass();

        if (modeMismatch(aDisplayMode, fDisplayClass)) {
            // Never notify a folder that isn't displayed
            return false;
        }

        if (modeMismatch(aNotifyMode, fNotifyClass)) {
            // Do not notify folders in the wrong class
            return false;
        }

        // No notification for new messages in Trash, Drafts, Spam or Sent folder.
        // But do notify if it's the INBOX (see issue 1817).
        Folder folder = message.getFolder();
        if (folder != null) {
            String folderServerId = folder.getServerId();
            if (!folderServerId.equals(account.getInboxFolder()) &&
                    (folderServerId.equals(account.getTrashFolder())
                            || folderServerId.equals(account.getDraftsFolder())
                            || folderServerId.equals(account.getSpamFolder())
                            || folderServerId.equals(account.getSentFolder()))) {
                return false;
            }
        }

        // Don't notify if the sender address matches one of our identities and the user chose not
        // to be notified for such messages.
        if (account.isAnIdentity(message.getFrom()) && !account.isNotifySelfNewMail()) {
            return false;
        }

        return !account.isNotifyContactsMailOnly() || contacts.isAnyInContacts(message.getFrom());
    }

    public void deleteAccount(Account account) {
        notificationController.clearNewMailNotifications(account);
        memorizingMessagingListener.removeAccount(account);
    }

    /**
     * Save a draft message.
     *
     * @param account
     *         Account we are saving for.
     * @param message
     *         Message to save.
     *
     * @return Message representing the entry in the local store.
     */
    public Message saveDraft(final Account account, final Message message, long existingDraftId, boolean saveRemotely) {
        Message localMessage = null;
        try {
            LocalStore localStore = account.getLocalStore();
            LocalFolder localFolder = localStore.getFolder(account.getDraftsFolder());
            localFolder.open(Folder.OPEN_MODE_RW);

            if (existingDraftId != INVALID_MESSAGE_ID) {
                String uid = localFolder.getMessageUidById(existingDraftId);
                message.setUid(uid);
            }

            // Save the message to the store.
            localFolder.appendMessages(Collections.singletonList(message));
            // Fetch the message back from the store.  This is the Message that's returned to the caller.
            localMessage = localFolder.getMessage(message.getUid());
            localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);

            if (saveRemotely) {
                PendingCommand command = PendingAppend.create(localFolder.getServerId(), localMessage.getUid());
                queuePendingCommand(account, command);
                processPendingCommands(account);
            }

        } catch (MessagingException e) {
            Timber.e(e, "Unable to save message as draft.");
        }
        return localMessage;
    }

    public long getId(Message message) {
        long id;
        if (message instanceof LocalMessage) {
            id = ((LocalMessage) message).getDatabaseId();
        } else {
            Timber.w("MessagingController.getId() called without a LocalMessage");
            id = INVALID_MESSAGE_ID;
        }

        return id;
    }

    private boolean modeMismatch(Account.FolderMode aMode, Folder.FolderClass fMode) {
        return aMode == Account.FolderMode.NONE
                || (aMode == Account.FolderMode.FIRST_CLASS &&
                fMode != Folder.FolderClass.FIRST_CLASS)
                || (aMode == Account.FolderMode.FIRST_AND_SECOND_CLASS &&
                fMode != Folder.FolderClass.FIRST_CLASS &&
                fMode != Folder.FolderClass.SECOND_CLASS)
                || (aMode == Account.FolderMode.NOT_SECOND_CLASS &&
                fMode == Folder.FolderClass.SECOND_CLASS);
    }

    private static AtomicInteger sequencing = new AtomicInteger(0);

    private static class Command implements Comparable<Command> {
        public Runnable runnable;
        public MessagingListener listener;
        public String description;
        boolean isForegroundPriority;

        int sequence = sequencing.getAndIncrement();

        @Override
        public int compareTo(@NonNull Command other) {
            if (other.isForegroundPriority && !isForegroundPriority) {
                return 1;
            } else if (!other.isForegroundPriority && isForegroundPriority) {
                return -1;
            } else {
                return (sequence - other.sequence);
            }
        }
    }

    public MessagingListener getCheckMailListener() {
        return checkMailListener;
    }

    public void setCheckMailListener(MessagingListener checkMailListener) {
        if (this.checkMailListener != null) {
            removeListener(this.checkMailListener);
        }
        this.checkMailListener = checkMailListener;
        if (this.checkMailListener != null) {
            addListener(this.checkMailListener);
        }
    }

    public Collection<Pusher> getPushers() {
        return pushers.values();
    }

    public boolean setupPushing(final Account account) {
        try {
            Pusher previousPusher = pushers.remove(account);
            if (previousPusher != null) {
                previousPusher.stop();
            }

            Account.FolderMode aDisplayMode = account.getFolderDisplayMode();
            Account.FolderMode aPushMode = account.getFolderPushMode();

            List<String> names = new ArrayList<>();

            LocalStore localStore = account.getLocalStore();
            for (final Folder folder : localStore.getPersonalNamespaces(false)) {
                if (folder.getServerId().equals(account.getOutboxFolder())) {
                    continue;
                }
                folder.open(Folder.OPEN_MODE_RW);

                Folder.FolderClass fDisplayClass = folder.getDisplayClass();
                Folder.FolderClass fPushClass = folder.getPushClass();

                if (modeMismatch(aDisplayMode, fDisplayClass)) {
                    // Never push a folder that isn't displayed
                    /*
                    if (K9.DEBUG) {
                        Log.v(K9.LOG_TAG, "Not pushing folder " + folder.getName() +
                              " which is in display class " + fDisplayClass + " while account is in display mode " + aDisplayMode);
                    }
                    */

                    continue;
                }

                if (modeMismatch(aPushMode, fPushClass)) {
                    // Do not push folders in the wrong class
                    /*
                    if (K9.DEBUG) {
                        Log.v(K9.LOG_TAG, "Not pushing folder " + folder.getName() +
                              " which is in push mode " + fPushClass + " while account is in push mode " + aPushMode);
                    }
                    */

                    continue;
                }

                Timber.i("Starting pusher for %s:%s", account.getDescription(), folder.getServerId());

                names.add(folder.getServerId());
            }

            if (!names.isEmpty()) {
                PushReceiver receiver = new MessagingControllerPushReceiver(context, account, this);
                int maxPushFolders = account.getMaxPushFolders();

                if (names.size() > maxPushFolders) {
                    Timber.i("Count of folders to push for account %s is %d, greater than limit of %d, truncating",
                            account.getDescription(), names.size(), maxPushFolders);

                    names = names.subList(0, maxPushFolders);
                }

                try {
                    Backend backend = getBackend(account);
                    if (!backend.isPushCapable()) {
                        Timber.i("Account %s is not push capable, skipping", account.getDescription());

                        return false;
                    }
                    Pusher pusher = backend.createPusher(receiver);
                    Pusher oldPusher = pushers.putIfAbsent(account, pusher);
                    if (oldPusher == null) {
                        pusher.start(names);
                    }
                } catch (Exception e) {
                    Timber.e(e, "Could not get remote store");
                    return false;
                }

                return true;
            } else {
                Timber.i("No folders are configured for pushing in account %s", account.getDescription());
                return false;
            }

        } catch (Exception e) {
            Timber.e(e, "Got exception while setting up pushing");
        }
        return false;
    }

    public void stopAllPushing() {
        Timber.i("Stopping all pushers");

        Iterator<Pusher> iter = pushers.values().iterator();
        while (iter.hasNext()) {
            Pusher pusher = iter.next();
            iter.remove();
            pusher.stop();
        }
    }

    public void systemStatusChanged() {
        for (MessagingListener l : getListeners()) {
            l.systemStatusChanged();
        }
    }

    public void cancelNotificationsForAccount(Account account) {
        notificationController.clearNewMailNotifications(account);
    }

    public void cancelNotificationForMessage(Account account, MessageReference messageReference) {
        notificationController.removeNewMailNotification(account, messageReference);
    }

    public void clearCertificateErrorNotifications(Account account, boolean incoming) {
        notificationController.clearCertificateErrorNotifications(account, incoming);
    }

    public void notifyUserIfCertificateProblem(Account account, Exception exception, boolean incoming) {
        if (!(exception instanceof CertificateValidationException)) {
            return;
        }

        CertificateValidationException cve = (CertificateValidationException) exception;
        if (!cve.needsUserAttention()) {
            return;
        }

        notificationController.showCertificateErrorNotification(account, incoming);
    }

    private void actOnMessagesGroupedByAccountAndFolder(List<MessageReference> messages, MessageActor actor) {
        Map<String, Map<String, List<MessageReference>>> accountMap = groupMessagesByAccountAndFolder(messages);

        for (Map.Entry<String, Map<String, List<MessageReference>>> entry : accountMap.entrySet()) {
            String accountUuid = entry.getKey();
            Account account = Preferences.getPreferences(context).getAccount(accountUuid);

            Map<String, List<MessageReference>> folderMap = entry.getValue();
            for (Map.Entry<String, List<MessageReference>> folderEntry : folderMap.entrySet()) {
                String folderServerId = folderEntry.getKey();
                List<MessageReference> messageList = folderEntry.getValue();
                actOnMessageGroup(account, folderServerId, messageList, actor);
            }
        }
    }

    @NonNull
    private Map<String, Map<String, List<MessageReference>>> groupMessagesByAccountAndFolder(
            List<MessageReference> messages) {
        Map<String, Map<String, List<MessageReference>>> accountMap = new HashMap<>();

        for (MessageReference message : messages) {
            if (message == null) {
                continue;
            }
            String accountUuid = message.getAccountUuid();
            String folderServerId = message.getFolderServerId();

            Map<String, List<MessageReference>> folderMap = accountMap.get(accountUuid);
            if (folderMap == null) {
                folderMap = new HashMap<>();
                accountMap.put(accountUuid, folderMap);
            }
            List<MessageReference> messageList = folderMap.get(folderServerId);
            if (messageList == null) {
                messageList = new LinkedList<>();
                folderMap.put(folderServerId, messageList);
            }

            messageList.add(message);
        }
        return accountMap;
    }

    private void actOnMessageGroup(
            Account account, String folderServerId, List<MessageReference> messageReferences, MessageActor actor) {
        try {
            LocalFolder messageFolder = account.getLocalStore().getFolder(folderServerId);
            List<LocalMessage> localMessages = messageFolder.getMessagesByReference(messageReferences);
            actor.act(account, messageFolder, localMessages);
        } catch (MessagingException e) {
            Timber.e(e, "Error loading account?!");
        }

    }

    private interface MessageActor {
        void act(Account account, LocalFolder messageFolder, List<LocalMessage> messages);
    }

    class ControllerSyncListener implements SyncListener {
        private final Account account;
        private final MessagingListener listener;
        private final LocalStore localStore;
        private final int previousUnreadMessageCount;
        boolean syncFailed = false;


        ControllerSyncListener(Account account, MessagingListener listener) {
            this.account = account;
            this.listener = listener;
            this.localStore = getLocalStoreOrThrow(account);

            previousUnreadMessageCount = getUnreadMessageCount();
        }

        private int getUnreadMessageCount() {
            try {
                AccountStats stats = getAccountStats(account);
                return stats.unreadMessageCount;
            } catch (MessagingException e) {
                Timber.e(e, "Unable to getUnreadMessageCount for account: %s", account);
                return 0;
            }
        }

        @Override
        public void syncStarted(@NotNull String folderServerId, @NotNull String folderName) {
            for (MessagingListener messagingListener : getListeners(listener)) {
                messagingListener.synchronizeMailboxStarted(account, folderServerId, folderName);
            }
        }

        @Override
        public void syncAuthenticationSuccess() {
            notificationController.clearAuthenticationErrorNotification(account, true);
        }

        @Override
        public void syncHeadersStarted(@NotNull String folderServerId, @NotNull String folderName) {
            for (MessagingListener messagingListener : getListeners(listener)) {
                messagingListener.synchronizeMailboxHeadersStarted(account, folderServerId, folderName);
            }
        }

        @Override
        public void syncHeadersProgress(@NotNull String folderServerId, int completed, int total) {
            for (MessagingListener messagingListener : getListeners(listener)) {
                messagingListener.synchronizeMailboxHeadersProgress(account, folderServerId, completed, total);
            }
        }

        @Override
        public void syncHeadersFinished(@NotNull String folderServerId, int totalMessagesInMailbox,
                int numNewMessages) {
            for (MessagingListener messagingListener : getListeners(listener)) {
                messagingListener.synchronizeMailboxHeadersFinished(account, folderServerId, totalMessagesInMailbox,
                        numNewMessages);
            }
        }

        @Override
        public void syncProgress(@NotNull String folderServerId, int completed, int total) {
            for (MessagingListener messagingListener : getListeners(listener)) {
                messagingListener.synchronizeMailboxProgress(account, folderServerId, completed, total);
            }
        }

        @Override
        public void syncNewMessage(@NotNull String folderServerId, @NotNull String messageServerId,
                boolean isOldMessage) {

            // Send a notification of this message
            LocalMessage message = loadMessage(folderServerId, messageServerId);
            LocalFolder localFolder = message.getFolder();
            if (shouldNotifyForMessage(account, localFolder, message, isOldMessage)) {
                // Notify with the localMessage so that we don't have to recalculate the content preview.
                notificationController.addNewMailNotification(account, message, previousUnreadMessageCount);
            }

            if (!message.isSet(Flag.SEEN)) {
                for (MessagingListener messagingListener : getListeners(listener)) {
                    messagingListener.synchronizeMailboxNewMessage(account, folderServerId, message);
                }
            }
        }

        @Override
        public void syncRemovedMessage(@NotNull String folderServerId, @NotNull String messageServerId) {
            for (MessagingListener messagingListener : getListeners(listener)) {
                messagingListener.synchronizeMailboxRemovedMessage(account, folderServerId, messageServerId);
            }
        }

        @Override
        public void syncFlagChanged(@NotNull String folderServerId, @NotNull String messageServerId) {
            boolean shouldBeNotifiedOf = false;
            LocalMessage message = loadMessage(folderServerId, messageServerId);
            if (message.isSet(Flag.DELETED) || isMessageSuppressed(message)) {
                syncRemovedMessage(folderServerId, message.getUid());
            } else {
                LocalFolder localFolder = message.getFolder();
                if (shouldNotifyForMessage(account, localFolder, message, false)) {
                    shouldBeNotifiedOf = true;
                }
            }

            // we're only interested in messages that need removing
            if (!shouldBeNotifiedOf) {
                MessageReference messageReference = message.makeMessageReference();
                notificationController.removeNewMailNotification(account, messageReference);
            }
        }

        @Override
        public void syncFinished(@NotNull String folderServerId, int totalMessagesInMailbox, int numNewMessages) {
            for (MessagingListener messagingListener : getListeners(listener)) {
                messagingListener.synchronizeMailboxFinished(account, folderServerId, totalMessagesInMailbox,
                        numNewMessages);
            }
        }

        @Override
        public void syncFailed(@NotNull String folderServerId, @NotNull String message, Exception exception) {
            syncFailed = true;

            if (exception instanceof AuthenticationFailedException) {
                handleAuthenticationFailure(account, true);
            } else {
                notifyUserIfCertificateProblem(account, exception, true);
            }

            for (MessagingListener messagingListener : getListeners(listener)) {
                messagingListener.synchronizeMailboxFailed(account, folderServerId, message);
            }
        }

        @Override
        public void folderStatusChanged(@NotNull String folderServerId, int unreadMessageCount) {
            for (MessagingListener messagingListener : getListeners(listener)) {
                messagingListener.folderStatusChanged(account, folderServerId, unreadMessageCount);
            }
        }

        private LocalMessage loadMessage(String folderServerId, String messageServerId) {
            try {
                LocalFolder localFolder = localStore.getFolder(folderServerId);
                localFolder.open(Folder.OPEN_MODE_RW);
                return localFolder.getMessage(messageServerId);
            } catch (MessagingException e) {
                throw new RuntimeException("Couldn't load message (" + folderServerId + ":" + messageServerId + ")", e);
            }
        }
    }
}
