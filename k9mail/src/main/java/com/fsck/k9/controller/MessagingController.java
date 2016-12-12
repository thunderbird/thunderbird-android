package com.fsck.k9.controller;


import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.Account.DeletePolicy;
import com.fsck.k9.Account.Expunge;
import com.fsck.k9.AccountStats;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.K9;
import com.fsck.k9.K9.Intents;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection;
import com.fsck.k9.cache.EmailProviderCache;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.FetchProfile.Item;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Folder.FolderType;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.Pusher;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mail.power.TracingPowerManager;
import com.fsck.k9.mail.power.TracingPowerManager.TracingWakeLock;
import com.fsck.k9.mail.store.pop3.Pop3Store;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalFolder.MoreMessages;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.LocalStore.PendingCommand;
import com.fsck.k9.mailstore.MessageRemovalListener;
import com.fsck.k9.mailstore.UnavailableStorageException;
import com.fsck.k9.notification.NotificationController;
import com.fsck.k9.provider.EmailProvider;
import com.fsck.k9.provider.EmailProvider.StatsColumns;
import com.fsck.k9.search.ConditionsTreeNode;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.fsck.k9.search.SearchSpecification;
import com.fsck.k9.search.SqlQueryBuilder;


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

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Set<Flag> SYNC_FLAGS = EnumSet.of(Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED, Flag.FORWARDED);

    private static final String PENDING_COMMAND_MOVE_OR_COPY = "com.fsck.k9.MessagingController.moveOrCopy";
    private static final String PENDING_COMMAND_MOVE_OR_COPY_BULK = "com.fsck.k9.MessagingController.moveOrCopyBulk";
    private static final String PENDING_COMMAND_MOVE_OR_COPY_BULK_NEW = "com.fsck.k9.MessagingController.moveOrCopyBulkNew";
    private static final String PENDING_COMMAND_EMPTY_TRASH = "com.fsck.k9.MessagingController.emptyTrash";
    private static final String PENDING_COMMAND_SET_FLAG_BULK = "com.fsck.k9.MessagingController.setFlagBulk";
    private static final String PENDING_COMMAND_SET_FLAG = "com.fsck.k9.MessagingController.setFlag";
    private static final String PENDING_COMMAND_APPEND = "com.fsck.k9.MessagingController.append";
    private static final String PENDING_COMMAND_MARK_ALL_AS_READ = "com.fsck.k9.MessagingController.markAllAsRead";
    private static final String PENDING_COMMAND_EXPUNGE = "com.fsck.k9.MessagingController.expunge";


    private static MessagingController inst = null;


    private final Context context;
    private final Contacts contacts;
    private final NotificationController notificationController;

    private final Thread controllerThread;

    private final BlockingQueue<Command> queuedCommands = new PriorityBlockingQueue<>();
    private final Set<MessagingListener> listeners = new CopyOnWriteArraySet<>();
    private final ConcurrentHashMap<String, AtomicInteger> sendCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Account, Pusher> pushers = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final MemorizingMessagingListener memorizingMessagingListener = new MemorizingMessagingListener();


    private MessagingListener checkMailListener = null;
    private volatile boolean stopped = false;


    public synchronized static MessagingController getInstance(Context context) {
        if (inst == null) {
            Context appContext = context.getApplicationContext();
            NotificationController notificationController = NotificationController.newInstance(appContext);
            Contacts contacts = Contacts.getInstance(context);
            inst = new MessagingController(appContext, notificationController, contacts);
        }
        return inst;
    }


    @VisibleForTesting
    MessagingController(Context context, NotificationController notificationController, Contacts contacts) {
        this.context = context;
        this.notificationController = notificationController;
        this.contacts = contacts;

        controllerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runInBackground();
            }
        });
        controllerThread.setName("MessagingController");
        controllerThread.start();
        addListener(memorizingMessagingListener);
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

                    if (K9.DEBUG) {
                        Log.i(K9.LOG_TAG, "Running command '" + command.description + "', seq = " + command.sequence +
                                "(" + (command.isForegroundPriority ? "foreground" : "background") + "priority)");
                    }

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
                                    Log.e(K9.LOG_TAG, "interrupted while putting a pending command for"
                                          + " an unavailable account back into the queue."
                                          + " THIS SHOULD NEVER HAPPEN.");
                                }
                            }
                        } .start();
                    }

                    if (K9.DEBUG) {
                        Log.i(K9.LOG_TAG, " Command '" + command.description + "' completed");
                    }

                    for (MessagingListener l : getListeners(command.listener)) {
                        l.controllerCommandCompleted(!queuedCommands.isEmpty());
                    }
                }
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Error running command '" + commandDescription + "'", e);
            }
        }
    }

    private void put(String description, MessagingListener listener, Runnable runnable) {
        putCommand(queuedCommands, description, listener, runnable, true);
    }

    private void putBackground(String description, MessagingListener listener, Runnable runnable) {
        putCommand(queuedCommands, description, listener, runnable, false);
    }

    private void putCommand(BlockingQueue<Command> queue, String description, MessagingListener listener, Runnable runnable, boolean isForeground) {
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

    private boolean isMessageSuppressed(LocalMessage message) {
        long messageId = message.getId();
        long folderId = message.getFolder().getId();

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
    public void listFoldersSynchronous(final Account account, final boolean refreshRemote, final MessagingListener listener) {
        for (MessagingListener l : getListeners(listener)) {
            l.listFoldersStarted(account);
        }
        List<LocalFolder> localFolders = null;
        if (!account.isAvailable(context)) {
            Log.i(K9.LOG_TAG, "not listing folders of unavailable account");
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

                addErrorMessage(account, null, e);
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
            Store store = account.getRemoteStore();

            List <? extends Folder > remoteFolders = store.getPersonalNamespaces(false);

            LocalStore localStore = account.getLocalStore();
            Set<String> remoteFolderNames = new HashSet<>();
            List<LocalFolder> foldersToCreate = new LinkedList<>();

            localFolders = localStore.getPersonalNamespaces(false);
            Set<String> localFolderNames = new HashSet<>();
            for (Folder localFolder : localFolders) {
                localFolderNames.add(localFolder.getName());
            }

            for (Folder remoteFolder : remoteFolders) {
                if (!localFolderNames.contains(remoteFolder.getName())) {
                    LocalFolder localFolder = localStore.getFolder(remoteFolder.getName());
                    foldersToCreate.add(localFolder);
                }
                remoteFolderNames.add(remoteFolder.getName());
            }
            localStore.createFolders(foldersToCreate, account.getDisplayCount());

            localFolders = localStore.getPersonalNamespaces(false);

            /*
             * Clear out any folders that are no longer on the remote store.
             */
            for (Folder localFolder : localFolders) {
                String localFolderName = localFolder.getName();

                // FIXME: This is a hack used to clean up when we accidentally created the
                //        special placeholder folder "-NONE-".
                if (K9.FOLDER_NONE.equals(localFolderName)) {
                    localFolder.delete(false);
                }

                if (!account.isSpecialFolder(localFolderName) &&
                        !remoteFolderNames.contains(localFolderName)) {
                    localFolder.delete(false);
                }
            }

            localFolders = localStore.getPersonalNamespaces(false);

            for (MessagingListener l : getListeners(listener)) {
                l.listFolders(account, localFolders);
            }
            for (MessagingListener l : getListeners(listener)) {
                l.listFoldersFinished(account);
            }
        } catch (Exception e) {
            for (MessagingListener l : getListeners(listener)) {
                l.listFoldersFailed(account, "");
            }
            addErrorMessage(account, null, e);
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
                public void messageStarted(String message, int number, int ofTotal) {}
                @Override
                public void messagesFinished(int number) {}
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

            // alert everyone the search has started
            if (listener != null) {
                listener.listLocalMessagesStarted(account, null);
            }

            // build and do the query in the localstore
            try {
                LocalStore localStore = account.getLocalStore();
                localStore.searchForMessages(retrievalListener, search);
            } catch (Exception e) {
                if (listener != null) {
                    listener.listLocalMessagesFailed(account, null, e.getMessage());
                }
                addErrorMessage(account, null, e);
            } finally {
                if (listener != null) {
                    listener.listLocalMessagesFinished(account, null);
                }
            }
        }

        // publish the total search statistics
        if (listener != null) {
            listener.searchStats(stats);
        }
    }

    public Future<?> searchRemoteMessages(final String acctUuid, final String folderName, final String query,
            final Set<Flag> requiredFlags, final Set<Flag> forbiddenFlags, final MessagingListener listener) {
        if (K9.DEBUG) {
            String msg = "searchRemoteMessages ("
                         + "acct=" + acctUuid
                         + ", folderName = " + folderName
                         + ", query = " + query
                         + ")";
            Log.i(K9.LOG_TAG, msg);
        }

        return threadPool.submit(new Runnable() {
            @Override
            public void run() {
                searchRemoteMessagesSynchronous(acctUuid, folderName, query, requiredFlags, forbiddenFlags, listener);
            }
        });
    }

    @VisibleForTesting
    void searchRemoteMessagesSynchronous(final String acctUuid, final String folderName, final String query,
            final Set<Flag> requiredFlags, final Set<Flag> forbiddenFlags, final MessagingListener listener) {
        final Account acct = Preferences.getPreferences(context).getAccount(acctUuid);

        if (listener != null) {
            listener.remoteSearchStarted(folderName);
        }

        List<Message> extraResults = new ArrayList<>();
        try {
            Store remoteStore = acct.getRemoteStore();
            LocalStore localStore = acct.getLocalStore();

            if (remoteStore == null || localStore == null) {
                throw new MessagingException("Could not get store");
            }

            Folder remoteFolder = remoteStore.getFolder(folderName);
            LocalFolder localFolder = localStore.getFolder(folderName);
            if (remoteFolder == null || localFolder == null) {
                throw new MessagingException("Folder not found");
            }

            List<Message> messages = remoteFolder.search(query, requiredFlags, forbiddenFlags);

            if (K9.DEBUG) {
                Log.i("Remote Search", "Remote search got " + messages.size() + " results");
            }

            // There's no need to fetch messages already completely downloaded
            List<Message> remoteMessages = localFolder.extractNewMessages(messages);
            messages.clear();

            if (listener != null) {
                listener.remoteSearchServerQueryComplete(folderName, remoteMessages.size(), acct.getRemoteSearchNumResults());
            }

            Collections.sort(remoteMessages, new UidReverseComparator());

            int resultLimit = acct.getRemoteSearchNumResults();
            if (resultLimit > 0 && remoteMessages.size() > resultLimit) {
                extraResults = remoteMessages.subList(resultLimit, remoteMessages.size());
                remoteMessages = remoteMessages.subList(0, resultLimit);
            }

            loadSearchResultsSynchronous(remoteMessages, localFolder, remoteFolder, listener);


        } catch (Exception e) {
            if (Thread.currentThread().isInterrupted()) {
                Log.i(K9.LOG_TAG, "Caught exception on aborted remote search; safe to ignore.", e);
            } else {
                Log.e(K9.LOG_TAG, "Could not complete remote search", e);
                if (listener != null) {
                    listener.remoteSearchFailed(null, e.getMessage());
                }
                addErrorMessage(acct, null, e);
            }
        } finally {
            if (listener != null) {
                listener.remoteSearchFinished(folderName, 0, acct.getRemoteSearchNumResults(), extraResults);
            }
        }

    }

    public void loadSearchResults(final Account account, final String folderName, final List<Message> messages,
            final MessagingListener listener) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.enableProgressIndicator(true);
                }
                try {
                    Store remoteStore = account.getRemoteStore();
                    LocalStore localStore = account.getLocalStore();

                    if (remoteStore == null || localStore == null) {
                        throw new MessagingException("Could not get store");
                    }

                    Folder remoteFolder = remoteStore.getFolder(folderName);
                    LocalFolder localFolder = localStore.getFolder(folderName);
                    if (remoteFolder == null || localFolder == null) {
                        throw new MessagingException("Folder not found");
                    }

                    loadSearchResultsSynchronous(messages, localFolder, remoteFolder, listener);
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "Exception in loadSearchResults: " + e);
                    addErrorMessage(account, null, e);
                } finally {
                    if (listener != null) {
                        listener.enableProgressIndicator(false);
                    }
                }
            }
        });
    }

    private void loadSearchResultsSynchronous(List<Message> messages, LocalFolder localFolder, Folder remoteFolder,
            MessagingListener listener) throws MessagingException {
        final FetchProfile header = new FetchProfile();
        header.add(FetchProfile.Item.FLAGS);
        header.add(FetchProfile.Item.ENVELOPE);
        final FetchProfile structure = new FetchProfile();
        structure.add(FetchProfile.Item.STRUCTURE);

        int i = 0;
        for (Message message : messages) {
            i++;
            LocalMessage localMsg = localFolder.getMessage(message.getUid());

            if (localMsg == null) {
                remoteFolder.fetch(Collections.singletonList(message), header, null);
                //fun fact: ImapFolder.fetch can't handle getting STRUCTURE at same time as headers
                remoteFolder.fetch(Collections.singletonList(message), structure, null);
                localFolder.appendMessages(Collections.singletonList(message));
                localMsg = localFolder.getMessage(message.getUid());
            }

            if (listener != null) {
                listener.remoteSearchAddMessage(remoteFolder.getName(), localMsg, i, messages.size());
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
            addErrorMessage(account, null, me);

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
     *
     * TODO Break this method up into smaller chunks.
     */
    @VisibleForTesting
    void synchronizeMailboxSynchronous(final Account account, final String folder, final MessagingListener listener,
            Folder providedRemoteFolder) {
        Folder remoteFolder = null;
        LocalFolder tLocalFolder = null;

        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Synchronizing folder " + account.getDescription() + ":" + folder);

        for (MessagingListener l : getListeners(listener)) {
            l.synchronizeMailboxStarted(account, folder);
        }
        /*
         * We don't ever sync the Outbox or errors folder
         */
        if (folder.equals(account.getOutboxFolderName()) || folder.equals(account.getErrorFolderName())) {
            for (MessagingListener l : getListeners(listener)) {
                l.synchronizeMailboxFinished(account, folder, 0, 0);
            }

            return;
        }

        Exception commandException = null;
        try {
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "SYNC: About to process pending commands for account " + account.getDescription());

            try {
                processPendingCommandsSynchronous(account);
            } catch (Exception e) {
                addErrorMessage(account, null, e);

                Log.e(K9.LOG_TAG, "Failure processing command, but allow message sync attempt", e);
                commandException = e;
            }

            /*
             * Get the message list from the local store and create an index of
             * the uids within the list.
             */
            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "SYNC: About to get local folder " + folder);

            final LocalStore localStore = account.getLocalStore();
            tLocalFolder = localStore.getFolder(folder);
            final LocalFolder localFolder = tLocalFolder;
            localFolder.open(Folder.OPEN_MODE_RW);
            localFolder.updateLastUid();
            List<? extends Message> localMessages = localFolder.getMessages(null);
            Map<String, Message> localUidMap = new HashMap<>();
            for (Message message : localMessages) {
                localUidMap.put(message.getUid(), message);
            }

            if (providedRemoteFolder != null) {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "SYNC: using providedRemoteFolder " + folder);
                remoteFolder = providedRemoteFolder;
            } else {
                Store remoteStore = account.getRemoteStore();

                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "SYNC: About to get remote folder " + folder);
                remoteFolder = remoteStore.getFolder(folder);

                if (! verifyOrCreateRemoteSpecialFolder(account, folder, remoteFolder, listener)) {
                    return;
                }


                /*
                 * Synchronization process:
                 *
                Open the folder
                Upload any local messages that are marked as PENDING_UPLOAD (Drafts, Sent, Trash)
                Get the message count
                Get the list of the newest K9.DEFAULT_VISIBLE_LIMIT messages
                getMessages(messageCount - K9.DEFAULT_VISIBLE_LIMIT, messageCount)
                See if we have each message locally, if not fetch it's flags and envelope
                Get and update the unread count for the folder
                Update the remote flags of any messages we have locally with an internal date newer than the remote message.
                Get the current flags for any messages we have locally but did not just download
                Update local flags
                For any message we have locally but not remotely, delete the local message to keep cache clean.
                Download larger parts of any new messages.
                (Optional) Download small attachments in the background.
                 */

                /*
                 * Open the remote folder. This pre-loads certain metadata like message count.
                 */
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "SYNC: About to open remote folder " + folder);

                remoteFolder.open(Folder.OPEN_MODE_RW);
                if (Expunge.EXPUNGE_ON_POLL == account.getExpungePolicy()) {
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "SYNC: Expunging folder " + account.getDescription() + ":" + folder);
                    remoteFolder.expunge();
                }

            }

            notificationController.clearAuthenticationErrorNotification(account, true);

            /*
             * Get the remote message count.
             */
            int remoteMessageCount = remoteFolder.getMessageCount();

            int visibleLimit = localFolder.getVisibleLimit();

            if (visibleLimit < 0) {
                visibleLimit = K9.DEFAULT_VISIBLE_LIMIT;
            }

            final List<Message> remoteMessages = new ArrayList<>();
            Map<String, Message> remoteUidMap = new HashMap<>();

            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "SYNC: Remote message count for folder " + folder + " is " + remoteMessageCount);
            final Date earliestDate = account.getEarliestPollDate();


            int remoteStart = 1;
            if (remoteMessageCount > 0) {
                /* Message numbers start at 1.  */
                if (visibleLimit > 0) {
                    remoteStart = Math.max(0, remoteMessageCount - visibleLimit) + 1;
                } else {
                    remoteStart = 1;
                }

                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "SYNC: About to get messages " + remoteStart + " through " + remoteMessageCount + " for folder " + folder);

                final AtomicInteger headerProgress = new AtomicInteger(0);
                for (MessagingListener l : getListeners(listener)) {
                    l.synchronizeMailboxHeadersStarted(account, folder);
                }


                List<? extends Message> remoteMessageArray = remoteFolder.getMessages(remoteStart, remoteMessageCount, earliestDate, null);

                int messageCount = remoteMessageArray.size();

                for (Message thisMess : remoteMessageArray) {
                    headerProgress.incrementAndGet();
                    for (MessagingListener l : getListeners(listener)) {
                        l.synchronizeMailboxHeadersProgress(account, folder, headerProgress.get(), messageCount);
                    }
                    Message localMessage = localUidMap.get(thisMess.getUid());
                    if (localMessage == null || !localMessage.olderThan(earliestDate)) {
                        remoteMessages.add(thisMess);
                        remoteUidMap.put(thisMess.getUid(), thisMess);
                    }
                }
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "SYNC: Got " + remoteUidMap.size() + " messages for folder " + folder);

                for (MessagingListener l : getListeners(listener)) {
                    l.synchronizeMailboxHeadersFinished(account, folder, headerProgress.get(), remoteUidMap.size());
                }

            } else if(remoteMessageCount < 0) {
                throw new Exception("Message count " + remoteMessageCount + " for folder " + folder);
            }

            /*
             * Remove any messages that are in the local store but no longer on the remote store or are too old
             */
            MoreMessages moreMessages = localFolder.getMoreMessages();
            if (account.syncRemoteDeletions()) {
                List<Message> destroyMessages = new ArrayList<>();
                for (Message localMessage : localMessages) {
                    if (remoteUidMap.get(localMessage.getUid()) == null) {
                        destroyMessages.add(localMessage);
                    }
                }

                if (!destroyMessages.isEmpty()) {
                    moreMessages = MoreMessages.UNKNOWN;

                    localFolder.destroyMessages(destroyMessages);

                    for (Message destroyMessage : destroyMessages) {
                        for (MessagingListener l : getListeners(listener)) {
                            l.synchronizeMailboxRemovedMessage(account, folder, destroyMessage);
                        }
                    }
                }
            }
            // noinspection UnusedAssignment, free memory early? (better break up the method!)
            localMessages = null;

            if (moreMessages == MoreMessages.UNKNOWN) {
                updateMoreMessages(remoteFolder, localFolder, earliestDate, remoteStart);
            }

            /*
             * Now we download the actual content of messages.
             */
            int newMessages = downloadMessages(account, remoteFolder, localFolder, remoteMessages, false, true);

            int unreadMessageCount = localFolder.getUnreadMessageCount();
            for (MessagingListener l : getListeners()) {
                l.folderStatusChanged(account, folder, unreadMessageCount);
            }

            /* Notify listeners that we're finally done. */

            localFolder.setLastChecked(System.currentTimeMillis());
            localFolder.setStatus(null);

            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "Done synchronizing folder " + account.getDescription() + ":" + folder +
                      " @ " + new Date() + " with " + newMessages + " new messages");

            for (MessagingListener l : getListeners(listener)) {
                l.synchronizeMailboxFinished(account, folder, remoteMessageCount, newMessages);
            }


            if (commandException != null) {
                String rootMessage = getRootCauseMessage(commandException);
                Log.e(K9.LOG_TAG, "Root cause failure in " + account.getDescription() + ":" +
                      tLocalFolder.getName() + " was '" + rootMessage + "'");
                localFolder.setStatus(rootMessage);
                for (MessagingListener l : getListeners(listener)) {
                    l.synchronizeMailboxFailed(account, folder, rootMessage);
                }
            }

            if (K9.DEBUG)
                Log.i(K9.LOG_TAG, "Done synchronizing folder " + account.getDescription() + ":" + folder);

        } catch (AuthenticationFailedException e) {
            handleAuthenticationFailure(account, true);

            for (MessagingListener l : getListeners(listener)) {
                l.synchronizeMailboxFailed(account, folder, "Authentication failure");
            }
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "synchronizeMailbox", e);
            // If we don't set the last checked, it can try too often during
            // failure conditions
            String rootMessage = getRootCauseMessage(e);
            if (tLocalFolder != null) {
                try {
                    tLocalFolder.setStatus(rootMessage);
                    tLocalFolder.setLastChecked(System.currentTimeMillis());
                } catch (MessagingException me) {
                    Log.e(K9.LOG_TAG, "Could not set last checked on folder " + account.getDescription() + ":" +
                          tLocalFolder.getName(), e);
                }
            }

            for (MessagingListener l : getListeners(listener)) {
                l.synchronizeMailboxFailed(account, folder, rootMessage);
            }
            notifyUserIfCertificateProblem(account, e, true);
            addErrorMessage(account, null, e);
            Log.e(K9.LOG_TAG, "Failed synchronizing folder " + account.getDescription() + ":" + folder + " @ " + new Date());

        } finally {
            if (providedRemoteFolder == null) {
                closeFolder(remoteFolder);
            }

            closeFolder(tLocalFolder);
        }

    }

    void handleAuthenticationFailure(Account account, boolean incoming) {
        notificationController.showAuthenticationErrorNotification(account, incoming);
    }

    private void updateMoreMessages(Folder remoteFolder, LocalFolder localFolder, Date earliestDate, int remoteStart)
            throws MessagingException, IOException {

        if (remoteStart == 1) {
            localFolder.setMoreMessages(MoreMessages.FALSE);
        } else {
            boolean moreMessagesAvailable = remoteFolder.areMoreMessagesAvailable(remoteStart, earliestDate);

            MoreMessages newMoreMessages = (moreMessagesAvailable) ? MoreMessages.TRUE : MoreMessages.FALSE;
            localFolder.setMoreMessages(newMoreMessages);
        }
    }

    private static void closeFolder(Folder f) {
        if (f != null) {
            f.close();
        }
    }

    /*
     * If the folder is a "special" folder we need to see if it exists
     * on the remote server. It if does not exist we'll try to create it. If we
     * can't create we'll abort. This will happen on every single Pop3 folder as
     * designed and on Imap folders during error conditions. This allows us
     * to treat Pop3 and Imap the same in this code.
     */
    private boolean verifyOrCreateRemoteSpecialFolder(Account account, String folder, Folder remoteFolder,
            MessagingListener listener) throws MessagingException {
        if (folder.equals(account.getTrashFolderName()) ||
                folder.equals(account.getSentFolderName()) ||
                folder.equals(account.getDraftsFolderName())) {
            if (!remoteFolder.exists()) {
                if (!remoteFolder.create(FolderType.HOLDS_MESSAGES)) {
                    for (MessagingListener l : getListeners(listener)) {
                        l.synchronizeMailboxFinished(account, folder, 0, 0);
                    }
                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Done synchronizing folder " + folder);

                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Fetches the messages described by inputMessages from the remote store and writes them to
     * local storage.
     *
     * @param account
     *            The account the remote store belongs to.
     * @param remoteFolder
     *            The remote folder to download messages from.
     * @param localFolder
     *            The {@link LocalFolder} instance corresponding to the remote folder.
     * @param inputMessages
     *            A list of messages objects that store the UIDs of which messages to download.
     * @param flagSyncOnly
     *            Only flags will be fetched from the remote store if this is {@code true}.
     * @param purgeToVisibleLimit
     *            If true, local messages will be purged down to the limit of visible messages.
     *
     * @return The number of downloaded messages that are not flagged as {@link Flag#SEEN}.
     *
     * @throws MessagingException
     */
    private int downloadMessages(final Account account, final Folder remoteFolder,
            final LocalFolder localFolder, List<Message> inputMessages,
            boolean flagSyncOnly, boolean purgeToVisibleLimit) throws MessagingException {

        final Date earliestDate = account.getEarliestPollDate();
        Date downloadStarted = new Date(); // now

        if (earliestDate != null) {
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "Only syncing messages after " + earliestDate);
            }
        }
        final String folder = remoteFolder.getName();

        int unreadBeforeStart = 0;
        try {
            AccountStats stats = account.getStats(context);
            unreadBeforeStart = stats.unreadMessageCount;

        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to getUnreadMessageCount for account: " + account, e);
        }

        List<Message> syncFlagMessages = new ArrayList<>();
        List<Message> unsyncedMessages = new ArrayList<>();
        final AtomicInteger newMessages = new AtomicInteger(0);

        List<Message> messages = new ArrayList<>(inputMessages);

        for (Message message : messages) {
            evaluateMessageForDownload(message, folder, localFolder, remoteFolder, account, unsyncedMessages, syncFlagMessages , flagSyncOnly);
        }

        final AtomicInteger progress = new AtomicInteger(0);
        final int todo = unsyncedMessages.size() + syncFlagMessages.size();
        for (MessagingListener l : getListeners()) {
            l.synchronizeMailboxProgress(account, folder, progress.get(), todo);
        }

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Have " + unsyncedMessages.size() + " unsynced messages");

        messages.clear();
        final List<Message> largeMessages = new ArrayList<>();
        final List<Message> smallMessages = new ArrayList<>();
        if (!unsyncedMessages.isEmpty()) {

            /*
             * Reverse the order of the messages. Depending on the server this may get us
             * fetch results for newest to oldest. If not, no harm done.
             */
            Collections.sort(unsyncedMessages, new UidReverseComparator());
            int visibleLimit = localFolder.getVisibleLimit();
            int listSize = unsyncedMessages.size();

            if ((visibleLimit > 0) && (listSize > visibleLimit)) {
                unsyncedMessages = unsyncedMessages.subList(0, visibleLimit);
            }

            FetchProfile fp = new FetchProfile();
            if (remoteFolder.supportsFetchingFlags()) {
                fp.add(FetchProfile.Item.FLAGS);
            }
            fp.add(FetchProfile.Item.ENVELOPE);

            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "SYNC: About to fetch " + unsyncedMessages.size() + " unsynced messages for folder " + folder);

            fetchUnsyncedMessages(account, remoteFolder, unsyncedMessages, smallMessages, largeMessages, progress, todo, fp);

            String updatedPushState = localFolder.getPushState();
            for (Message message : unsyncedMessages) {
                String newPushState = remoteFolder.getNewPushState(updatedPushState, message);
                if (newPushState != null) {
                    updatedPushState = newPushState;
                }
            }
            localFolder.setPushState(updatedPushState);

            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "SYNC: Synced unsynced messages for folder " + folder);
            }
        }
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Have "
                  + largeMessages.size() + " large messages and "
                  + smallMessages.size() + " small messages out of "
                  + unsyncedMessages.size() + " unsynced messages");

        unsyncedMessages.clear();
        /*
         * Grab the content of the small messages first. This is going to
         * be very fast and at very worst will be a single up of a few bytes and a single
         * download of 625k.
         */
        FetchProfile fp = new FetchProfile();
        //TODO: Only fetch small and large messages if we have some
        fp.add(FetchProfile.Item.BODY);
        //        fp.add(FetchProfile.Item.FLAGS);
        //        fp.add(FetchProfile.Item.ENVELOPE);
        downloadSmallMessages(account, remoteFolder, localFolder, smallMessages, progress, unreadBeforeStart, newMessages, todo, fp);
        smallMessages.clear();
        /*
         * Now do the large messages that require more round trips.
         */
        fp = new FetchProfile();
        fp.add(FetchProfile.Item.STRUCTURE);
        downloadLargeMessages(account, remoteFolder, localFolder, largeMessages, progress, unreadBeforeStart, newMessages, todo, fp);
        largeMessages.clear();

        /*
         * Refresh the flags for any messages in the local store that we didn't just
         * download.
         */

        refreshLocalMessageFlags(account, remoteFolder, localFolder, syncFlagMessages, progress, todo);

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Synced remote messages for folder " + folder + ", " + newMessages.get() + " new messages");

        if (purgeToVisibleLimit) {
            localFolder.purgeToVisibleLimit(new MessageRemovalListener() {
                @Override
                public void messageRemoved(Message message) {
                    for (MessagingListener l : getListeners()) {
                        l.synchronizeMailboxRemovedMessage(account, folder, message);
                    }
                }

            });
        }

        // If the oldest message seen on this sync is newer than
        // the oldest message seen on the previous sync, then
        // we want to move our high-water mark forward
        // this is all here just for pop which only syncs inbox
        // this would be a little wrong for IMAP (we'd want a folder-level pref, not an account level pref.)
        // fortunately, we just don't care.
        Long oldestMessageTime = localFolder.getOldestMessageDate();

        if (oldestMessageTime != null) {
            Date oldestExtantMessage = new Date(oldestMessageTime);
            if (oldestExtantMessage.before(downloadStarted) &&
                    oldestExtantMessage.after(new Date(account.getLatestOldMessageSeenTime()))) {
                account.setLatestOldMessageSeenTime(oldestExtantMessage.getTime());
                account.save(Preferences.getPreferences(context));
            }

        }
        return newMessages.get();
    }

    private void evaluateMessageForDownload(final Message message, final String folder,
                                            final LocalFolder localFolder,
                                            final Folder remoteFolder,
                                            final Account account,
                                            final List<Message> unsyncedMessages,
                                            final List<Message> syncFlagMessages,
                                            boolean flagSyncOnly) throws MessagingException {
        if (message.isSet(Flag.DELETED)) {
            syncFlagMessages.add(message);
            return;
        }

        Message localMessage = localFolder.getMessage(message.getUid());

        if (localMessage == null) {
            if (!flagSyncOnly) {
                if (!message.isSet(Flag.X_DOWNLOADED_FULL) && !message.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Message with uid " + message.getUid() + " has not yet been downloaded");

                    unsyncedMessages.add(message);
                } else {
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Message with uid " + message.getUid() + " is partially or fully downloaded");

                    // Store the updated message locally
                    localFolder.appendMessages(Collections.singletonList(message));

                    localMessage = localFolder.getMessage(message.getUid());

                    localMessage.setFlag(Flag.X_DOWNLOADED_FULL, message.isSet(Flag.X_DOWNLOADED_FULL));
                    localMessage.setFlag(Flag.X_DOWNLOADED_PARTIAL, message.isSet(Flag.X_DOWNLOADED_PARTIAL));

                    for (MessagingListener l : getListeners()) {
                        l.synchronizeMailboxAddOrUpdateMessage(account, folder, localMessage);
                        if (!localMessage.isSet(Flag.SEEN)) {
                            l.synchronizeMailboxNewMessage(account, folder, localMessage);
                        }
                    }
                }
            }
        } else if (!localMessage.isSet(Flag.DELETED)) {
            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "Message with uid " + message.getUid() + " is present in the local store");

            if (!localMessage.isSet(Flag.X_DOWNLOADED_FULL) && !localMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "Message with uid " + message.getUid()
                          + " is not downloaded, even partially; trying again");

                unsyncedMessages.add(message);
            } else {
                String newPushState = remoteFolder.getNewPushState(localFolder.getPushState(), message);
                if (newPushState != null) {
                    localFolder.setPushState(newPushState);
                }
                syncFlagMessages.add(message);
            }
        }
    }

    private <T extends Message> void fetchUnsyncedMessages(final Account account, final Folder<T> remoteFolder,
                                       List<T> unsyncedMessages,
                                       final List<Message> smallMessages,
                                       final List<Message> largeMessages,
                                       final AtomicInteger progress,
                                       final int todo,
                                       FetchProfile fp) throws MessagingException {
        final String folder = remoteFolder.getName();

        final Date earliestDate = account.getEarliestPollDate();
        remoteFolder.fetch(unsyncedMessages, fp,
        new MessageRetrievalListener<T>() {
            @Override
            public void messageFinished(T message, int number, int ofTotal) {
                try {
                    if (message.isSet(Flag.DELETED) || message.olderThan(earliestDate)) {
                        if (K9.DEBUG) {
                            if (message.isSet(Flag.DELETED)) {
                                Log.v(K9.LOG_TAG, "Newly downloaded message " + account + ":" + folder + ":" + message.getUid()
                                      + " was marked deleted on server, skipping");
                            } else {
                                Log.d(K9.LOG_TAG, "Newly downloaded message " + message.getUid() + " is older than "
                                      + earliestDate + ", skipping");
                            }
                        }
                        progress.incrementAndGet();
                        for (MessagingListener l : getListeners()) {
                            //TODO: This might be the source of poll count errors in the UI. Is todo always the same as ofTotal
                            l.synchronizeMailboxProgress(account, folder, progress.get(), todo);
                        }
                        return;
                    }

                    if (account.getMaximumAutoDownloadMessageSize() > 0 &&
                    message.getSize() > account.getMaximumAutoDownloadMessageSize()) {
                        largeMessages.add(message);
                    } else {
                        smallMessages.add(message);
                    }
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Error while storing downloaded message.", e);
                    addErrorMessage(account, null, e);
                }
            }

            @Override
            public void messageStarted(String uid, int number, int ofTotal) {}

            @Override
            public void messagesFinished(int total) {
                // FIXME this method is almost never invoked by various Stores! Don't rely on it unless fixed!!
            }

        });
    }

    private boolean shouldImportMessage(final Account account, final Message message,
            final Date earliestDate) {

        if (account.isSearchByDateCapable() && message.olderThan(earliestDate)) {
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "Message " + message.getUid() + " is older than "
                      + earliestDate + ", hence not saving");
            }
            return false;
        }
        return true;
    }

    private <T extends Message> void downloadSmallMessages(final Account account, final Folder<T> remoteFolder,
                                       final LocalFolder localFolder,
                                       List<T> smallMessages,
                                       final AtomicInteger progress,
                                       final int unreadBeforeStart,
                                       final AtomicInteger newMessages,
                                       final int todo,
                                       FetchProfile fp) throws MessagingException {
        final String folder = remoteFolder.getName();

        final Date earliestDate = account.getEarliestPollDate();

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Fetching " + smallMessages.size() + " small messages for folder " + folder);

        remoteFolder.fetch(smallMessages,
        fp, new MessageRetrievalListener<T>() {
            @Override
            public void messageFinished(final T message, int number, int ofTotal) {
                try {

                    if (!shouldImportMessage(account, message, earliestDate)) {
                        progress.incrementAndGet();

                        return;
                    }

                    // Store the updated message locally
                    final LocalMessage localMessage = localFolder.storeSmallMessage(message, new Runnable() {
                        @Override
                        public void run() {
                            progress.incrementAndGet();
                        }
                    });

                    // Increment the number of "new messages" if the newly downloaded message is
                    // not marked as read.
                    if (!localMessage.isSet(Flag.SEEN)) {
                        newMessages.incrementAndGet();
                    }

                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "About to notify listeners that we got a new small message "
                              + account + ":" + folder + ":" + message.getUid());

                    // Update the listener with what we've found
                    for (MessagingListener l : getListeners()) {
                        l.synchronizeMailboxAddOrUpdateMessage(account, folder, localMessage);
                        l.synchronizeMailboxProgress(account, folder, progress.get(), todo);
                        if (!localMessage.isSet(Flag.SEEN)) {
                            l.synchronizeMailboxNewMessage(account, folder, localMessage);
                        }
                    }
                    // Send a notification of this message

                    if (shouldNotifyForMessage(account, localFolder, message)) {
                        // Notify with the localMessage so that we don't have to recalculate the content preview.
                        notificationController.addNewMailNotification(account, localMessage, unreadBeforeStart);
                    }

                } catch (MessagingException me) {
                    addErrorMessage(account, null, me);
                    Log.e(K9.LOG_TAG, "SYNC: fetch small messages", me);
                }
            }

            @Override
            public void messageStarted(String uid, int number, int ofTotal) {}

            @Override
            public void messagesFinished(int total) {}
        });

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Done fetching small messages for folder " + folder);
    }

    private <T extends Message> void downloadLargeMessages(final Account account, final Folder<T> remoteFolder,
                                       final LocalFolder localFolder,
                                       List<T> largeMessages,
                                       final AtomicInteger progress,
                                       final int unreadBeforeStart,
                                       final AtomicInteger newMessages,
                                       final int todo,
                                       FetchProfile fp) throws MessagingException {
        final String folder = remoteFolder.getName();

        final Date earliestDate = account.getEarliestPollDate();

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Fetching large messages for folder " + folder);

        remoteFolder.fetch(largeMessages, fp, null);
        for (T message : largeMessages) {

            if (!shouldImportMessage(account, message, earliestDate)) {
                progress.incrementAndGet();
                continue;
            }

            if (message.getBody() == null) {
                /*
                 * The provider was unable to get the structure of the message, so
                 * we'll download a reasonable portion of the messge and mark it as
                 * incomplete so the entire thing can be downloaded later if the user
                 * wishes to download it.
                 */
                fp = new FetchProfile();
                fp.add(FetchProfile.Item.BODY_SANE);
                /*
                 *  TODO a good optimization here would be to make sure that all Stores set
                 *  the proper size after this fetch and compare the before and after size. If
                 *  they equal we can mark this SYNCHRONIZED instead of PARTIALLY_SYNCHRONIZED
                 */

                remoteFolder.fetch(Collections.singletonList(message), fp, null);

                // Store the updated message locally
                localFolder.appendMessages(Collections.singletonList(message));

                Message localMessage = localFolder.getMessage(message.getUid());


                // Certain (POP3) servers give you the whole message even when you ask for only the first x Kb
                if (!message.isSet(Flag.X_DOWNLOADED_FULL)) {
                    /*
                     * Mark the message as fully downloaded if the message size is smaller than
                     * the account's autodownload size limit, otherwise mark as only a partial
                     * download.  This will prevent the system from downloading the same message
                     * twice.
                     *
                     * If there is no limit on autodownload size, that's the same as the message
                     * being smaller than the max size
                     */
                    if (account.getMaximumAutoDownloadMessageSize() == 0 || message.getSize() < account.getMaximumAutoDownloadMessageSize()) {
                        localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);
                    } else {
                        // Set a flag indicating that the message has been partially downloaded and
                        // is ready for view.
                        localMessage.setFlag(Flag.X_DOWNLOADED_PARTIAL, true);
                    }
                }
            } else {
                /*
                 * We have a structure to deal with, from which
                 * we can pull down the parts we want to actually store.
                 * Build a list of parts we are interested in. Text parts will be downloaded
                 * right now, attachments will be left for later.
                 */

                Set<Part> viewables = MessageExtractor.collectTextParts(message);

                /*
                 * Now download the parts we're interested in storing.
                 */
                for (Part part : viewables) {
                    remoteFolder.fetchPart(message, part, null);
                }
                // Store the updated message locally
                localFolder.appendMessages(Collections.singletonList(message));

                Message localMessage = localFolder.getMessage(message.getUid());

                // Set a flag indicating this message has been fully downloaded and can be
                // viewed.
                localMessage.setFlag(Flag.X_DOWNLOADED_PARTIAL, true);
            }
            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "About to notify listeners that we got a new large message "
                      + account + ":" + folder + ":" + message.getUid());

            // Update the listener with what we've found
            progress.incrementAndGet();
            // TODO do we need to re-fetch this here?
            LocalMessage localMessage = localFolder.getMessage(message.getUid());

            // Increment the number of "new messages" if the newly downloaded message is
            // not marked as read.
            if (!localMessage.isSet(Flag.SEEN)) {
                newMessages.incrementAndGet();
            }

            for (MessagingListener l : getListeners()) {
                l.synchronizeMailboxAddOrUpdateMessage(account, folder, localMessage);
                l.synchronizeMailboxProgress(account, folder, progress.get(), todo);
                if (!localMessage.isSet(Flag.SEEN)) {
                    l.synchronizeMailboxNewMessage(account, folder, localMessage);
                }
            }

            // Send a notification of this message
            if (shouldNotifyForMessage(account, localFolder, message)) {
                // Notify with the localMessage so that we don't have to recalculate the content preview.
                notificationController.addNewMailNotification(account, localMessage, unreadBeforeStart);
            }

        }//for large messages
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Done fetching large messages for folder " + folder);

    }

    private void refreshLocalMessageFlags(final Account account, final Folder remoteFolder,
                                          final LocalFolder localFolder,
                                          List<Message> syncFlagMessages,
                                          final AtomicInteger progress,
                                          final int todo
                                         ) throws MessagingException {

        final String folder = remoteFolder.getName();
        if (remoteFolder.supportsFetchingFlags()) {
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "SYNC: About to sync flags for "
                      + syncFlagMessages.size() + " remote messages for folder " + folder);

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.FLAGS);

            List<Message> undeletedMessages = new LinkedList<>();
            for (Message message : syncFlagMessages) {
                if (!message.isSet(Flag.DELETED)) {
                    undeletedMessages.add(message);
                }
            }

            remoteFolder.fetch(undeletedMessages, fp, null);
            for (Message remoteMessage : syncFlagMessages) {
                LocalMessage localMessage = localFolder.getMessage(remoteMessage.getUid());
                boolean messageChanged = syncFlags(localMessage, remoteMessage);
                if (messageChanged) {
                    boolean shouldBeNotifiedOf = false;
                    if (localMessage.isSet(Flag.DELETED) || isMessageSuppressed(localMessage)) {
                        for (MessagingListener l : getListeners()) {
                            l.synchronizeMailboxRemovedMessage(account, folder, localMessage);
                        }
                    } else {
                        for (MessagingListener l : getListeners()) {
                            l.synchronizeMailboxAddOrUpdateMessage(account, folder, localMessage);
                        }
                        if (shouldNotifyForMessage(account, localFolder, localMessage)) {
                            shouldBeNotifiedOf = true;
                        }
                    }

                    // we're only interested in messages that need removing
                    if (!shouldBeNotifiedOf) {
                        MessageReference messageReference = localMessage.makeMessageReference();
                        notificationController.removeNewMailNotification(account, messageReference);
                    }
                }
                progress.incrementAndGet();
                for (MessagingListener l : getListeners()) {
                    l.synchronizeMailboxProgress(account, folder, progress.get(), todo);
                }
            }
        }
    }

    private boolean syncFlags(LocalMessage localMessage, Message remoteMessage) throws MessagingException {
        boolean messageChanged = false;
        if (localMessage == null || localMessage.isSet(Flag.DELETED)) {
            return false;
        }
        if (remoteMessage.isSet(Flag.DELETED)) {
            if (localMessage.getFolder().syncRemoteDeletions()) {
                localMessage.setFlag(Flag.DELETED, true);
                messageChanged = true;
            }
        } else {
            for (Flag flag : MessagingController.SYNC_FLAGS) {
                if (remoteMessage.isSet(flag) != localMessage.isSet(flag)) {
                    localMessage.setFlag(flag, remoteMessage.isSet(flag));
                    messageChanged = true;
                }
            }
        }
        return messageChanged;
    }
    private String getRootCauseMessage(Throwable t) {
        Throwable rootCause = t;
        Throwable nextCause;
        do {
            nextCause = rootCause.getCause();
            if (nextCause != null) {
                rootCause = nextCause;
            }
        } while (nextCause != null);
        if (rootCause instanceof MessagingException) {
            return rootCause.getMessage();
        } else {
            // Remove the namespace on the exception so we have a fighting chance of seeing more of the error in the
            // notification.
            return (rootCause.getLocalizedMessage() != null)
                ? (rootCause.getClass().getSimpleName() + ": " + rootCause.getLocalizedMessage())
                : rootCause.getClass().getSimpleName();
        }
    }

    private void queuePendingCommand(Account account, PendingCommand command) {
        try {
            LocalStore localStore = account.getLocalStore();
            localStore.addPendingCommand(command);
        } catch (Exception e) {
            addErrorMessage(account, null, e);

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
                    Log.i(K9.LOG_TAG, "Failed to process pending command because storage is not available - trying again later.");
                    throw new UnavailableAccountException(e);
                } catch (MessagingException me) {
                    Log.e(K9.LOG_TAG, "processPendingCommands", me);

                    addErrorMessage(account, null, me);

                    /*
                     * Ignore any exceptions from the commands. Commands will be processed
                     * on the next round.
                     */
                }
            }
        });
    }

    private void processPendingCommandsSynchronous(Account account) throws MessagingException {
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
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Processing pending command '" + command + "'");

                String[] components = command.command.split("\\.");
                String commandTitle = components[components.length - 1];
                for (MessagingListener l : getListeners()) {
                    l.pendingCommandStarted(account, commandTitle);
                }
                /*
                 * We specifically do not catch any exceptions here. If a command fails it is
                 * most likely due to a server or IO error and it must be retried before any
                 * other command processes. This maintains the order of the commands.
                 */
                try {
                    if (PENDING_COMMAND_APPEND.equals(command.command)) {
                        processPendingAppend(command, account);
                    } else if (PENDING_COMMAND_SET_FLAG_BULK.equals(command.command)) {
                        processPendingSetFlag(command, account);
                    } else if (PENDING_COMMAND_SET_FLAG.equals(command.command)) {
                        processPendingSetFlagOld(command, account);
                    } else if (PENDING_COMMAND_MARK_ALL_AS_READ.equals(command.command)) {
                        processPendingMarkAllAsRead(command, account);
                    } else if (PENDING_COMMAND_MOVE_OR_COPY_BULK.equals(command.command)) {
                        processPendingMoveOrCopyOld2(command, account);
                    } else if (PENDING_COMMAND_MOVE_OR_COPY_BULK_NEW.equals(command.command)) {
                        processPendingMoveOrCopy(command, account);
                    } else if (PENDING_COMMAND_MOVE_OR_COPY.equals(command.command)) {
                        processPendingMoveOrCopyOld(command, account);
                    } else if (PENDING_COMMAND_EMPTY_TRASH.equals(command.command)) {
                        processPendingEmptyTrash(command, account);
                    } else if (PENDING_COMMAND_EXPUNGE.equals(command.command)) {
                        processPendingExpunge(command, account);
                    }
                    localStore.removePendingCommand(command);
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "Done processing pending command '" + command + "'");
                } catch (MessagingException me) {
                    if (me.isPermanentFailure()) {
                        addErrorMessage(account, null, me);
                        Log.e(K9.LOG_TAG, "Failure of command '" + command + "' was permanent, removing command from queue");
                        localStore.removePendingCommand(processingCommand);
                    } else {
                        throw me;
                    }
                } finally {
                    progress++;
                    for (MessagingListener l : getListeners()) {
                        l.synchronizeMailboxProgress(account, null, progress, todo);
                        l.pendingCommandCompleted(account, commandTitle);
                    }
                }
            }
        } catch (MessagingException me) {
            notifyUserIfCertificateProblem(account, me, true);
            addErrorMessage(account, null, me);
            Log.e(K9.LOG_TAG, "Could not process command '" + processingCommand + "'", me);
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
     * TODO update the local message UID instead of deleteing it
     */
    private void processPendingAppend(PendingCommand command, Account account) throws MessagingException {
        Folder remoteFolder = null;
        LocalFolder localFolder = null;
        try {

            String folder = command.arguments[0];
            String uid = command.arguments[1];

            if (account.getErrorFolderName().equals(folder)) {
                return;
            }

            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folder);
            LocalMessage localMessage = localFolder.getMessage(uid);

            if (localMessage == null) {
                return;
            }

            Store remoteStore = account.getRemoteStore();
            remoteFolder = remoteStore.getFolder(folder);
            if (!remoteFolder.exists()) {
                if (!remoteFolder.create(FolderType.HOLDS_MESSAGES)) {
                    return;
                }
            }
            remoteFolder.open(Folder.OPEN_MODE_RW);
            if (remoteFolder.getMode() != Folder.OPEN_MODE_RW) {
                return;
            }

            Message remoteMessage = null;
            if (!localMessage.getUid().startsWith(K9.LOCAL_UID_PREFIX)) {
                remoteMessage = remoteFolder.getMessage(localMessage.getUid());
            }

            if (remoteMessage == null) {
                if (localMessage.isSet(Flag.X_REMOTE_COPY_STARTED)) {
                    Log.w(K9.LOG_TAG, "Local message with uid " + localMessage.getUid() +
                          " has flag " + Flag.X_REMOTE_COPY_STARTED + " already set, checking for remote message with " +
                          " same message id");
                    String rUid = remoteFolder.getUidFromMessageId(localMessage);
                    if (rUid != null) {
                        Log.w(K9.LOG_TAG, "Local message has flag " + Flag.X_REMOTE_COPY_STARTED + " already set, and there is a remote message with " +
                              " uid " + rUid + ", assuming message was already copied and aborting this copy");

                        String oldUid = localMessage.getUid();
                        localMessage.setUid(rUid);
                        localFolder.changeUid(localMessage);
                        for (MessagingListener l : getListeners()) {
                            l.messageUidChanged(account, folder, oldUid, localMessage.getUid());
                        }
                        return;
                    } else {
                        Log.w(K9.LOG_TAG, "No remote message with message-id found, proceeding with append");
                    }
                }

                /*
                 * If the message does not exist remotely we just upload it and then
                 * update our local copy with the new uid.
                 */
                FetchProfile fp = new FetchProfile();
                fp.add(FetchProfile.Item.BODY);
                localFolder.fetch(Collections.singletonList(localMessage) , fp, null);
                String oldUid = localMessage.getUid();
                localMessage.setFlag(Flag.X_REMOTE_COPY_STARTED, true);
                remoteFolder.appendMessages(Collections.singletonList(localMessage));

                localFolder.changeUid(localMessage);
                for (MessagingListener l : getListeners()) {
                    l.messageUidChanged(account, folder, oldUid, localMessage.getUid());
                }
            } else {
                /*
                 * If the remote message exists we need to determine which copy to keep.
                 */
                /*
                 * See if the remote message is newer than ours.
                 */
                FetchProfile fp = new FetchProfile();
                fp.add(FetchProfile.Item.ENVELOPE);
                remoteFolder.fetch(Collections.singletonList(remoteMessage), fp, null);
                Date localDate = localMessage.getInternalDate();
                Date remoteDate = remoteMessage.getInternalDate();
                if (remoteDate != null && remoteDate.compareTo(localDate) > 0) {
                    /*
                     * If the remote message is newer than ours we'll just
                     * delete ours and move on. A sync will get the server message
                     * if we need to be able to see it.
                     */
                    localMessage.destroy();
                } else {
                    /*
                     * Otherwise we'll upload our message and then delete the remote message.
                     */
                    fp = new FetchProfile();
                    fp.add(FetchProfile.Item.BODY);
                    localFolder.fetch(Collections.singletonList(localMessage), fp, null);
                    String oldUid = localMessage.getUid();

                    localMessage.setFlag(Flag.X_REMOTE_COPY_STARTED, true);

                    remoteFolder.appendMessages(Collections.singletonList(localMessage));
                    localFolder.changeUid(localMessage);
                    for (MessagingListener l : getListeners()) {
                        l.messageUidChanged(account, folder, oldUid, localMessage.getUid());
                    }
                    if (remoteDate != null) {
                        remoteMessage.setFlag(Flag.DELETED, true);
                        if (Expunge.EXPUNGE_IMMEDIATELY == account.getExpungePolicy()) {
                            remoteFolder.expunge();
                        }
                    }
                }
            }
        } finally {
            closeFolder(remoteFolder);
            closeFolder(localFolder);
        }
    }
    private void queueMoveOrCopy(Account account, String srcFolder, String destFolder, boolean isCopy, String uids[]) {
        if (account.getErrorFolderName().equals(srcFolder)) {
            return;
        }
        PendingCommand command = new PendingCommand();
        command.command = PENDING_COMMAND_MOVE_OR_COPY_BULK_NEW;

        int length = 4 + uids.length;
        command.arguments = new String[length];
        command.arguments[0] = srcFolder;
        command.arguments[1] = destFolder;
        command.arguments[2] = Boolean.toString(isCopy);
        command.arguments[3] = Boolean.toString(false);
        System.arraycopy(uids, 0, command.arguments, 4, uids.length);
        queuePendingCommand(account, command);
    }

    private void queueMoveOrCopy(Account account, String srcFolder, String destFolder,
            boolean isCopy, String uids[], Map<String, String> uidMap) {
        if (uidMap == null || uidMap.isEmpty()) {
            queueMoveOrCopy(account, srcFolder, destFolder, isCopy, uids);
        } else {
            if (account.getErrorFolderName().equals(srcFolder)) {
                return;
            }
            PendingCommand command = new PendingCommand();
            command.command = PENDING_COMMAND_MOVE_OR_COPY_BULK_NEW;

            int length = 4 + uidMap.keySet().size() + uidMap.values().size();
            command.arguments = new String[length];
            command.arguments[0] = srcFolder;
            command.arguments[1] = destFolder;
            command.arguments[2] = Boolean.toString(isCopy);
            command.arguments[3] = Boolean.toString(true);
            System.arraycopy(uidMap.keySet().toArray(EMPTY_STRING_ARRAY), 0, command.arguments, 4, uidMap.keySet().size());
            System.arraycopy(uidMap.values().toArray(EMPTY_STRING_ARRAY), 0, command.arguments, 4 + uidMap.keySet().size(), uidMap.values().size());
            queuePendingCommand(account, command);
        }
    }

    /**
     * Convert pending command to new format and call
     * {@link #processPendingMoveOrCopy(PendingCommand, Account)}.
     *
     * <p>
     * TODO: This method is obsolete and is only for transition from K-9 4.0 to K-9 4.2
     * Eventually, it should be removed.
     * </p>
     *
     * @param command
     *         Pending move/copy command in old format.
     * @param account
     *         The account the pending command belongs to.
     *
     * @throws MessagingException
     *         In case of an error.
     */
    private void processPendingMoveOrCopyOld2(PendingCommand command, Account account) throws MessagingException {
        PendingCommand newCommand = new PendingCommand();
        int len = command.arguments.length;
        newCommand.command = PENDING_COMMAND_MOVE_OR_COPY_BULK_NEW;
        newCommand.arguments = new String[len + 1];
        newCommand.arguments[0] = command.arguments[0];
        newCommand.arguments[1] = command.arguments[1];
        newCommand.arguments[2] = command.arguments[2];
        newCommand.arguments[3] = Boolean.toString(false);
        System.arraycopy(command.arguments, 3, newCommand.arguments, 4, len - 3);

        processPendingMoveOrCopy(newCommand, account);
    }

    /**
     * Process a pending trash message command.
     */
    private void processPendingMoveOrCopy(PendingCommand command, Account account) throws MessagingException {
        Folder remoteSrcFolder = null;
        Folder remoteDestFolder = null;
        LocalFolder localDestFolder;
        try {
            String srcFolder = command.arguments[0];
            if (account.getErrorFolderName().equals(srcFolder)) {
                return;
            }
            String destFolder = command.arguments[1];
            String isCopyS = command.arguments[2];
            String hasNewUidsS = command.arguments[3];

            boolean hasNewUids = false;
            if (hasNewUidsS != null) {
                hasNewUids = Boolean.parseBoolean(hasNewUidsS);
            }

            Store remoteStore = account.getRemoteStore();
            remoteSrcFolder = remoteStore.getFolder(srcFolder);

            Store localStore = account.getLocalStore();
            localDestFolder = (LocalFolder) localStore.getFolder(destFolder);
            List<Message> messages = new ArrayList<>();

            /*
             * We split up the localUidMap into two parts while sending the command, here we assemble it back.
             */
            Map<String, String> localUidMap = new HashMap<>();
            if (hasNewUids) {
                int offset = (command.arguments.length - 4) / 2;

                for (int i = 4; i < 4 + offset; i++) {
                    localUidMap.put(command.arguments[i], command.arguments[i + offset]);

                    String uid = command.arguments[i];
                    if (!uid.startsWith(K9.LOCAL_UID_PREFIX)) {
                        messages.add(remoteSrcFolder.getMessage(uid));
                    }
                }

            } else {
                for (int i = 4; i < command.arguments.length; i++) {
                    String uid = command.arguments[i];
                    if (!uid.startsWith(K9.LOCAL_UID_PREFIX)) {
                        messages.add(remoteSrcFolder.getMessage(uid));
                    }
                }
            }

            boolean isCopy = false;
            if (isCopyS != null) {
                isCopy = Boolean.parseBoolean(isCopyS);
            }

            if (!remoteSrcFolder.exists()) {
                throw new MessagingException("processingPendingMoveOrCopy: remoteFolder " + srcFolder + " does not exist", true);
            }
            remoteSrcFolder.open(Folder.OPEN_MODE_RW);
            if (remoteSrcFolder.getMode() != Folder.OPEN_MODE_RW) {
                throw new MessagingException("processingPendingMoveOrCopy: could not open remoteSrcFolder " + srcFolder + " read/write", true);
            }

            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "processingPendingMoveOrCopy: source folder = " + srcFolder
                      + ", " + messages.size() + " messages, destination folder = " + destFolder + ", isCopy = " + isCopy);

            Map <String, String> remoteUidMap = null;

            if (!isCopy && destFolder.equals(account.getTrashFolderName())) {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "processingPendingMoveOrCopy doing special case for deleting message");

                String destFolderName = destFolder;
                if (K9.FOLDER_NONE.equals(destFolderName)) {
                    destFolderName = null;
                }
                remoteSrcFolder.delete(messages, destFolderName);
            } else {
                remoteDestFolder = remoteStore.getFolder(destFolder);

                if (isCopy) {
                    remoteUidMap = remoteSrcFolder.copyMessages(messages, remoteDestFolder);
                } else {
                    remoteUidMap = remoteSrcFolder.moveMessages(messages, remoteDestFolder);
                }
            }
            if (!isCopy && Expunge.EXPUNGE_IMMEDIATELY == account.getExpungePolicy()) {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "processingPendingMoveOrCopy expunging folder " + account.getDescription() + ":" + srcFolder);

                remoteSrcFolder.expunge();
            }

            /*
             * This next part is used to bring the local UIDs of the local destination folder
             * upto speed with the remote UIDs of remote destination folder.
             */
            if (!localUidMap.isEmpty() && remoteUidMap != null && !remoteUidMap.isEmpty()) {
                for (Map.Entry<String, String> entry : remoteUidMap.entrySet()) {
                    String remoteSrcUid = entry.getKey();
                    String localDestUid = localUidMap.get(remoteSrcUid);
                    String newUid = entry.getValue();

                    Message localDestMessage = localDestFolder.getMessage(localDestUid);
                    if (localDestMessage != null) {
                        localDestMessage.setUid(newUid);
                        localDestFolder.changeUid((LocalMessage)localDestMessage);
                        for (MessagingListener l : getListeners()) {
                            l.messageUidChanged(account, destFolder, localDestUid, newUid);
                        }
                    }
                }
            }
        } finally {
            closeFolder(remoteSrcFolder);
            closeFolder(remoteDestFolder);
        }
    }

    private void queueSetFlag(final Account account, final String folderName,
            final String newState, final String flag, final String[] uids) {
        putBackground("queueSetFlag " + account.getDescription() + ":" + folderName, null, new Runnable() {
            @Override
            public void run() {
                PendingCommand command = new PendingCommand();
                command.command = PENDING_COMMAND_SET_FLAG_BULK;
                int length = 3 + uids.length;
                command.arguments = new String[length];
                command.arguments[0] = folderName;
                command.arguments[1] = newState;
                command.arguments[2] = flag;
                System.arraycopy(uids, 0, command.arguments, 3, uids.length);
                queuePendingCommand(account, command);
                processPendingCommands(account);
            }
        });
    }
    /**
     * Processes a pending mark read or unread command.
     */
    private void processPendingSetFlag(PendingCommand command, Account account) throws MessagingException {
        String folder = command.arguments[0];

        if (account.getErrorFolderName().equals(folder)) {
            return;
        }

        boolean newState = Boolean.parseBoolean(command.arguments[1]);

        Flag flag = Flag.valueOf(command.arguments[2]);

        Store remoteStore = account.getRemoteStore();
        Folder remoteFolder = remoteStore.getFolder(folder);
        if (!remoteFolder.exists() || !remoteFolder.isFlagSupported(flag)) {
            return;
        }

        try {
            remoteFolder.open(Folder.OPEN_MODE_RW);
            if (remoteFolder.getMode() != Folder.OPEN_MODE_RW) {
                return;
            }
            List<Message> messages = new ArrayList<>();
            for (int i = 3; i < command.arguments.length; i++) {
                String uid = command.arguments[i];
                if (!uid.startsWith(K9.LOCAL_UID_PREFIX)) {
                    messages.add(remoteFolder.getMessage(uid));
                }
            }

            if (messages.isEmpty()) {
                return;
            }
            remoteFolder.setFlags(messages, Collections.singleton(flag), newState);
        } finally {
            closeFolder(remoteFolder);
        }
    }

    // TODO: This method is obsolete and is only for transition from K-9 2.0 to K-9 2.1
    // Eventually, it should be removed
    private void processPendingSetFlagOld(PendingCommand command, Account account) throws MessagingException {
        String folder = command.arguments[0];
        String uid = command.arguments[1];

        if (account.getErrorFolderName().equals(folder)) {
            return;
        }
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "processPendingSetFlagOld: folder = " + folder + ", uid = " + uid);

        boolean newState = Boolean.parseBoolean(command.arguments[2]);

        Flag flag = Flag.valueOf(command.arguments[3]);
        Folder remoteFolder = null;
        try {
            Store remoteStore = account.getRemoteStore();
            remoteFolder = remoteStore.getFolder(folder);
            if (!remoteFolder.exists()) {
                return;
            }
            remoteFolder.open(Folder.OPEN_MODE_RW);
            if (remoteFolder.getMode() != Folder.OPEN_MODE_RW) {
                return;
            }
            Message remoteMessage = null;
            if (!uid.startsWith(K9.LOCAL_UID_PREFIX)) {
                remoteMessage = remoteFolder.getMessage(uid);
            }
            if (remoteMessage == null) {
                return;
            }
            remoteMessage.setFlag(flag, newState);
        } finally {
            closeFolder(remoteFolder);
        }
    }
    private void queueExpunge(final Account account, final String folderName) {
        putBackground("queueExpunge " + account.getDescription() + ":" + folderName, null, new Runnable() {
            @Override
            public void run() {
                PendingCommand command = new PendingCommand();
                command.command = PENDING_COMMAND_EXPUNGE;

                command.arguments = new String[1];

                command.arguments[0] = folderName;
                queuePendingCommand(account, command);
                processPendingCommands(account);
            }
        });
    }
    private void processPendingExpunge(PendingCommand command, Account account) throws MessagingException {
        String folder = command.arguments[0];

        if (account.getErrorFolderName().equals(folder)) {
            return;
        }
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "processPendingExpunge: folder = " + folder);

        Store remoteStore = account.getRemoteStore();
        Folder remoteFolder = remoteStore.getFolder(folder);
        try {
            if (!remoteFolder.exists()) {
                return;
            }
            remoteFolder.open(Folder.OPEN_MODE_RW);
            if (remoteFolder.getMode() != Folder.OPEN_MODE_RW) {
                return;
            }
            remoteFolder.expunge();
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "processPendingExpunge: complete for folder = " + folder);
        } finally {
            closeFolder(remoteFolder);
        }
    }


    // TODO: This method is obsolete and is only for transition from K-9 2.0 to K-9 2.1
    // Eventually, it should be removed
    private void processPendingMoveOrCopyOld(PendingCommand command, Account account) throws MessagingException {
        String srcFolder = command.arguments[0];
        String uid = command.arguments[1];
        String destFolder = command.arguments[2];
        String isCopyS = command.arguments[3];

        boolean isCopy = false;
        if (isCopyS != null) {
            isCopy = Boolean.parseBoolean(isCopyS);
        }

        if (account.getErrorFolderName().equals(srcFolder)) {
            return;
        }

        Store remoteStore = account.getRemoteStore();
        Folder remoteSrcFolder = remoteStore.getFolder(srcFolder);
        Folder remoteDestFolder = remoteStore.getFolder(destFolder);

        if (!remoteSrcFolder.exists()) {
            throw new MessagingException("processPendingMoveOrCopyOld: remoteFolder " + srcFolder + " does not exist", true);
        }
        remoteSrcFolder.open(Folder.OPEN_MODE_RW);
        if (remoteSrcFolder.getMode() != Folder.OPEN_MODE_RW) {
            throw new MessagingException("processPendingMoveOrCopyOld: could not open remoteSrcFolder " + srcFolder + " read/write", true);
        }

        Message remoteMessage = null;
        if (!uid.startsWith(K9.LOCAL_UID_PREFIX)) {
            remoteMessage = remoteSrcFolder.getMessage(uid);
        }
        if (remoteMessage == null) {
            throw new MessagingException("processPendingMoveOrCopyOld: remoteMessage " + uid + " does not exist", true);
        }

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "processPendingMoveOrCopyOld: source folder = " + srcFolder
                  + ", uid = " + uid + ", destination folder = " + destFolder + ", isCopy = " + isCopy);

        if (!isCopy && destFolder.equals(account.getTrashFolderName())) {
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "processPendingMoveOrCopyOld doing special case for deleting message");

            remoteMessage.delete(account.getTrashFolderName());
            remoteSrcFolder.close();
            return;
        }

        remoteDestFolder.open(Folder.OPEN_MODE_RW);
        if (remoteDestFolder.getMode() != Folder.OPEN_MODE_RW) {
            throw new MessagingException("processPendingMoveOrCopyOld: could not open remoteDestFolder " + srcFolder + " read/write", true);
        }

        if (isCopy) {
            remoteSrcFolder.copyMessages(Collections.singletonList(remoteMessage), remoteDestFolder);
        } else {
            remoteSrcFolder.moveMessages(Collections.singletonList(remoteMessage), remoteDestFolder);
        }
        remoteSrcFolder.close();
        remoteDestFolder.close();
    }

    private void processPendingMarkAllAsRead(PendingCommand command, Account account) throws MessagingException {
        String folder = command.arguments[0];
        Folder remoteFolder = null;
        LocalFolder localFolder = null;
        try {
            Store localStore = account.getLocalStore();
            localFolder = (LocalFolder) localStore.getFolder(folder);
            localFolder.open(Folder.OPEN_MODE_RW);
            List<? extends Message> messages = localFolder.getMessages(null, false);
            for (Message message : messages) {
                if (!message.isSet(Flag.SEEN)) {
                    message.setFlag(Flag.SEEN, true);
                    for (MessagingListener l : getListeners()) {
                        l.listLocalMessagesUpdateMessage(account, folder, message);
                    }
                }
            }

            for (MessagingListener l : getListeners()) {
                l.folderStatusChanged(account, folder, 0);
            }


            if (account.getErrorFolderName().equals(folder)) {
                return;
            }

            Store remoteStore = account.getRemoteStore();
            remoteFolder = remoteStore.getFolder(folder);

            if (!remoteFolder.exists() || !remoteFolder.isFlagSupported(Flag.SEEN)) {
                return;
            }
            remoteFolder.open(Folder.OPEN_MODE_RW);
            if (remoteFolder.getMode() != Folder.OPEN_MODE_RW) {
                return;
            }

            remoteFolder.setFlags(Collections.singleton(Flag.SEEN), true);
            remoteFolder.close();
        } catch (UnsupportedOperationException uoe) {
            Log.w(K9.LOG_TAG, "Could not mark all server-side as read because store doesn't support operation", uoe);
        } finally {
            closeFolder(localFolder);
            closeFolder(remoteFolder);
        }
    }

    void addErrorMessage(Account account, String subject, Throwable t) {
        try {
            if (t == null) {
                return;
            }

            CharArrayWriter baos = new CharArrayWriter(t.getStackTrace().length * 10);
            PrintWriter ps = new PrintWriter(baos);
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0);
                ps.format("K9-Mail version: %s\r\n", packageInfo.versionName);
            } catch (Exception e) {
                // ignore
            }
            ps.format("Device make: %s\r\n", Build.MANUFACTURER);
            ps.format("Device model: %s\r\n", Build.MODEL);
            ps.format("Android version: %s\r\n\r\n", Build.VERSION.RELEASE);
            t.printStackTrace(ps);
            ps.close();

            if (subject == null) {
                subject = getRootCauseMessage(t);
            }

            addErrorMessage(account, subject, baos.toString());
        } catch (Throwable it) {
            Log.e(K9.LOG_TAG, "Could not save error message to " + account.getErrorFolderName(), it);
        }
    }

    private static AtomicBoolean loopCatch = new AtomicBoolean();
    private void addErrorMessage(Account account, String subject, String body) {
        if (!K9.DEBUG) {
            return;
        }
        if (!loopCatch.compareAndSet(false, true)) {
            return;
        }
        try {
            if (body == null || body.length() < 1) {
                return;
            }

            Store localStore = account.getLocalStore();
            LocalFolder localFolder = (LocalFolder)localStore.getFolder(account.getErrorFolderName());
            MimeMessage message = new MimeMessage();

            MimeMessageHelper.setBody(message, new TextBody(body));
            message.setFlag(Flag.X_DOWNLOADED_FULL, true);
            message.setSubject(subject);

            long nowTime = System.currentTimeMillis();
            Date nowDate = new Date(nowTime);
            message.setInternalDate(nowDate);
            message.addSentDate(nowDate, K9.hideTimeZone());
            message.setFrom(new Address(account.getEmail(), "K9mail internal"));

            localFolder.appendMessages(Collections.singletonList(message));

            localFolder.clearMessagesOlderThan(nowTime - (15 * 60 * 1000));

        } catch (Throwable it) {
            Log.e(K9.LOG_TAG, "Could not save error message to " + account.getErrorFolderName(), it);
        } finally {
            loopCatch.set(false);
        }
    }



    public void markAllMessagesRead(final Account account, final String folder) {

        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "Marking all messages in " + account.getDescription() + ":" + folder + " as read");
        }
        PendingCommand command = new PendingCommand();
        command.command = PENDING_COMMAND_MARK_ALL_AS_READ;
        command.arguments = new String[] { folder };
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
            Log.e(K9.LOG_TAG, "Couldn't get LocalStore instance", e);
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
            Log.e(K9.LOG_TAG, "Couldn't set flags in local database", e);
        }

        // Read folder name and UID of messages from the database
        Map<String, List<String>> folderMap;
        try {
            folderMap = localStore.getFoldersAndUids(ids, threadedList);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Couldn't get folder name and UID of messages", e);
            return;
        }

        // Loop over all folders
        for (Entry<String, List<String>> entry : folderMap.entrySet()) {
            String folderName = entry.getKey();

            // Notify listeners of changed folder status
            LocalFolder localFolder = localStore.getFolder(folderName);
            try {
                int unreadMessageCount = localFolder.getUnreadMessageCount();
                for (MessagingListener l : getListeners()) {
                    l.folderStatusChanged(account, folderName, unreadMessageCount);
                }
            } catch (MessagingException e) {
                Log.w(K9.LOG_TAG, "Couldn't get unread count for folder: " + folderName, e);
            }

            // The error folder is always a local folder
            // TODO: Skip the remote part for all local-only folders
            if (account.getErrorFolderName().equals(folderName)) {
                continue;
            }

            // Send flag change to server
            List<String> value = entry.getValue();
            String[] uids = value.toArray(new String[value.size()]);
            queueSetFlag(account, folderName, Boolean.toString(newState), flag.toString(), uids);
            processPendingCommands(account);
        }
    }

    /**
     * Set or remove a flag for a set of messages in a specific folder.
     *
     * <p>
     * The {@link Message} objects passed in are updated to reflect the new flag state.
     * </p>
     *
     * @param account
     *         The account the folder containing the messages belongs to.
     * @param folderName
     *         The name of the folder.
     * @param messages
     *         The messages to change the flag for.
     * @param flag
     *         The flag to change.
     * @param newState
     *         {@code true}, if the flag should be set. {@code false} if it should be removed.
     */
    public void setFlag(Account account, String folderName, List<? extends Message> messages, Flag flag,
            boolean newState) {
        // TODO: Put this into the background, but right now some callers depend on the message
        //       objects being modified right after this method returns.
        Folder localFolder = null;
        try {
            Store localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folderName);
            localFolder.open(Folder.OPEN_MODE_RW);

            // Allows for re-allowing sending of messages that could not be sent
            if (flag == Flag.FLAGGED && !newState &&
                    account.getOutboxFolderName().equals(folderName)) {
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
                l.folderStatusChanged(account, folderName, unreadMessageCount);
            }


            /*
             * Handle the remote side
             */

            // The error folder is always a local folder
            // TODO: Skip the remote part for all local-only folders
            if (account.getErrorFolderName().equals(folderName)) {
                return;
            }

            String[] uids = new String[messages.size()];
            for (int i = 0, end = uids.length; i < end; i++) {
                uids[i] = messages.get(i).getUid();
            }

            queueSetFlag(account, folderName, Boolean.toString(newState), flag.toString(), uids);
            processPendingCommands(account);
        } catch (MessagingException me) {
            addErrorMessage(account, null, me);
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
     * @param folderName
     *         The name of the folder.
     * @param uid
     *         The UID of the message to change the flag for.
     * @param flag
     *         The flag to change.
     * @param newState
     *         {@code true}, if the flag should be set. {@code false} if it should be removed.
     */
    public void setFlag(Account account, String folderName, String uid, Flag flag,
            boolean newState) {
        Folder localFolder = null;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folderName);
            localFolder.open(Folder.OPEN_MODE_RW);

            Message message = localFolder.getMessage(uid);
            if (message != null) {
                setFlag(account, folderName, Collections.singletonList(message), flag, newState);
            }
        } catch (MessagingException me) {
            addErrorMessage(account, null, me);
            throw new RuntimeException(me);
        } finally {
            closeFolder(localFolder);
        }
    }

    public void clearAllPending(final Account account) {
        try {
            Log.w(K9.LOG_TAG, "Clearing pending commands!");
            LocalStore localStore = account.getLocalStore();
            localStore.removePendingCommands();
        } catch (MessagingException me) {
            Log.e(K9.LOG_TAG, "Unable to clear pending command", me);
            addErrorMessage(account, null, me);
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
        Folder remoteFolder = null;
        LocalFolder localFolder = null;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folder);
            localFolder.open(Folder.OPEN_MODE_RW);

            LocalMessage message = localFolder.getMessage(uid);

            if (uid.startsWith(K9.LOCAL_UID_PREFIX)) {
                Log.w(K9.LOG_TAG, "Message has local UID so cannot download fully.");
                // ASH move toast
                android.widget.Toast.makeText(context,
                        "Message has local UID so cannot download fully",
                        android.widget.Toast.LENGTH_LONG).show();
                // TODO: Using X_DOWNLOADED_FULL is wrong because it's only a partial message. But
                // one we can't download completely. Maybe add a new flag; X_PARTIAL_MESSAGE ?
                message.setFlag(Flag.X_DOWNLOADED_FULL, true);
                message.setFlag(Flag.X_DOWNLOADED_PARTIAL, false);
            }
            /* commented out because this was pulled from another unmerged branch:
            } else if (localFolder.isLocalOnly() && !force) {
                Log.w(K9.LOG_TAG, "Message in local-only folder so cannot download fully.");
                // ASH move toast
                android.widget.Toast.makeText(mApplication,
                        "Message in local-only folder so cannot download fully",
                        android.widget.Toast.LENGTH_LONG).show();
                message.setFlag(Flag.X_DOWNLOADED_FULL, true);
                message.setFlag(Flag.X_DOWNLOADED_PARTIAL, false);
            }*/

            /*if (!message.isSet(Flag.X_DOWNLOADED_FULL)) */ {
                /*
                 * At this point the message is not available, so we need to download it
                 * fully if possible.
                 */

                Store remoteStore = account.getRemoteStore();
                remoteFolder = remoteStore.getFolder(folder);
                remoteFolder.open(Folder.OPEN_MODE_RW);

                // Get the remote message and fully download it
                Message remoteMessage = remoteFolder.getMessage(uid);

                if (loadPartialFromSearch) {
                    downloadMessages(account, remoteFolder, localFolder,
                            Collections.singletonList(remoteMessage), false, false);
                } else {
                    FetchProfile fp = new FetchProfile();
                    fp.add(FetchProfile.Item.BODY);
                    remoteFolder.fetch(Collections.singletonList(remoteMessage), fp, null);
                    localFolder.appendMessages(Collections.singletonList(remoteMessage));
                }

                message = localFolder.getMessage(uid);

                if (!loadPartialFromSearch) {
                    message.setFlag(Flag.X_DOWNLOADED_FULL, true);
                }
            }

            // Mark that this message is now fully synched
            if (account.isMarkMessageAsReadOnView()) {
                message.setFlag(Flag.SEEN, true);
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
            addErrorMessage(account, null, e);
            return false;
        } finally {
            closeFolder(remoteFolder);
            closeFolder(localFolder);
        }
    }

    public LocalMessage loadMessage(Account account, String folderName, String uid) throws MessagingException {
        LocalStore localStore = account.getLocalStore();
        LocalFolder localFolder = localStore.getFolder(folderName);
        localFolder.open(Folder.OPEN_MODE_RW);

        LocalMessage message = localFolder.getMessage(uid);
        if (message == null || message.getId() == 0) {
            throw new IllegalArgumentException("Message not found: folder=" + folderName + ", uid=" + uid);
        }

        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY);
        localFolder.fetch(Collections.singletonList(message), fp, null);
        localFolder.close();

        notificationController.removeNewMailNotification(account, message.makeMessageReference());
        markMessageAsReadOnView(account, message);

        return message;
    }

    private void markMessageAsReadOnView(Account account, LocalMessage message)
            throws MessagingException {

        if (account.isMarkMessageAsReadOnView() && !message.isSet(Flag.SEEN)) {
            List<Long> messageIds = Collections.singletonList(message.getId());
            setFlag(account, messageIds, Flag.SEEN, true);

            message.setFlagInternal(Flag.SEEN, true);
        }
    }

    public void loadAttachment(final Account account, final LocalMessage message, final Part part,
            final MessagingListener listener) {

        put("loadAttachment", listener, new Runnable() {
            @Override
            public void run() {
                Folder remoteFolder = null;
                LocalFolder localFolder = null;
                try {
                    String folderName = message.getFolder().getName();

                    LocalStore localStore = account.getLocalStore();
                    localFolder = localStore.getFolder(folderName);

                    Store remoteStore = account.getRemoteStore();
                    remoteFolder = remoteStore.getFolder(folderName);
                    remoteFolder.open(Folder.OPEN_MODE_RW);

                    Message remoteMessage = remoteFolder.getMessage(message.getUid());
                    remoteFolder.fetchPart(remoteMessage, part, null);

                    localFolder.addPartToMessage(message, part);

                    for (MessagingListener l : getListeners(listener)) {
                        l.loadAttachmentFinished(account, message, part);
                    }
                } catch (MessagingException me) {
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Exception loading attachment", me);

                    for (MessagingListener l : getListeners(listener)) {
                        l.loadAttachmentFailed(account, message, part, me.getMessage());
                    }
                    notifyUserIfCertificateProblem(account, me, true);
                    addErrorMessage(account, null, me);

                } finally {
                    closeFolder(localFolder);
                    closeFolder(remoteFolder);
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
            LocalFolder localFolder = localStore.getFolder(account.getOutboxFolderName());
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
            addErrorMessage(account, null, e);

        }
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
                              account.getOutboxFolderName());
            if (!localFolder.exists()) {
                return false;
            }

            localFolder.open(Folder.OPEN_MODE_RW);

            if (localFolder.getMessageCount() > 0) {
                return true;
            }
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Exception while checking for unsent messages", e);
        } finally {
            closeFolder(localFolder);
        }
        return false;
    }

    /**
     * Attempt to send any messages that are sitting in the Outbox.
     */
    private void sendPendingMessagesSynchronous(final Account account) {
        LocalFolder localFolder = null;
        Exception lastFailure = null;
        boolean wasPermanentFailure = false;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(
                              account.getOutboxFolderName());
            if (!localFolder.exists()) {
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
                l.synchronizeMailboxProgress(account, account.getSentFolderName(), progress, todo);
            }
            /*
             * The profile we will use to pull all of the content
             * for a given local message into memory for sending.
             */
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.BODY);

            if (K9.DEBUG)
                Log.i(K9.LOG_TAG, "Scanning folder '" + account.getOutboxFolderName() + "' (" + localFolder.getId() + ") for messages to send");

            Transport transport = Transport.getInstance(K9.app, account);
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

                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Send count for message " + message.getUid() + " is " + count.get());

                    if (count.incrementAndGet() > K9.MAX_SEND_ATTEMPTS) {
                        Log.e(K9.LOG_TAG, "Send count for message " + message.getUid() + " can't be delivered after " + K9.MAX_SEND_ATTEMPTS + " attempts.  Giving up until the user restarts the device");
                        notificationController.showSendFailedNotification(account,
                                new MessagingException(message.getSubject()));
                        continue;
                    }



                    localFolder.fetch(Collections.singletonList(message), fp, null);
                    try {


                        if (message.getHeader(K9.IDENTITY_HEADER).length > 0) {
                            Log.v(K9.LOG_TAG, "The user has set the Outbox and Drafts folder to the same thing. " +
                                  "This message appears to be a draft, so K-9 will not send it");
                            continue;

                        }


                        message.setFlag(Flag.X_SEND_IN_PROGRESS, true);
                        if (K9.DEBUG)
                            Log.i(K9.LOG_TAG, "Sending message with UID " + message.getUid());
                        transport.sendMessage(message);
                        message.setFlag(Flag.X_SEND_IN_PROGRESS, false);
                        message.setFlag(Flag.SEEN, true);
                        progress++;
                        for (MessagingListener l : getListeners()) {
                            l.synchronizeMailboxProgress(account, account.getSentFolderName(), progress, todo);
                        }
                        if (!account.hasSentFolder()) {
                            if (K9.DEBUG)
                                Log.i(K9.LOG_TAG, "Account does not have a sent mail folder; deleting sent message");
                            message.setFlag(Flag.DELETED, true);
                        } else {
                            LocalFolder localSentFolder = localStore.getFolder(account.getSentFolderName());
                            if (K9.DEBUG)
                                Log.i(K9.LOG_TAG, "Moving sent message to folder '" + account.getSentFolderName() + "' (" + localSentFolder.getId() + ") ");

                            localFolder.moveMessages(Collections.singletonList(message), localSentFolder);

                            if (K9.DEBUG)
                                Log.i(K9.LOG_TAG, "Moved sent message to folder '" + account.getSentFolderName() + "' (" + localSentFolder.getId() + ") ");

                            PendingCommand command = new PendingCommand();
                            command.command = PENDING_COMMAND_APPEND;
                            command.arguments = new String[] { localSentFolder.getName(), message.getUid() };
                            queuePendingCommand(account, command);
                            processPendingCommands(account);
                        }
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

                    Log.e(K9.LOG_TAG, "Failed to fetch message for sending", e);

                    addErrorMessage(account, "Failed to fetch message for sending", e);
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
            Log.i(K9.LOG_TAG, "Failed to send pending messages because storage is not available - trying again later.");
            throw new UnavailableAccountException(e);
        } catch (Exception e) {
            for (MessagingListener l : getListeners()) {
                l.sendPendingMessagesFailed(account);
            }
            addErrorMessage(account, null, e);

        } finally {
            if (lastFailure == null) {
                notificationController.clearSendFailedNotification(account);
            }
            closeFolder(localFolder);
        }
    }

    private void handleSendFailure(Account account, Store localStore, Folder localFolder, Message message,
            Exception exception, boolean permanentFailure) throws MessagingException {

        Log.e(K9.LOG_TAG, "Failed to send message", exception);

        if (permanentFailure) {
            moveMessageToDraftsFolder(account, localFolder, localStore, message);
        }

        addErrorMessage(account, "Failed to send message", exception);
        message.setFlag(Flag.X_SEND_FAILED, true);

        notifySynchronizeMailboxFailed(account, localFolder, exception);
    }

    private void moveMessageToDraftsFolder(Account account, Folder localFolder, Store localStore, Message message)
            throws MessagingException {
        LocalFolder draftsFolder = (LocalFolder) localStore.getFolder(account.getDraftsFolderName());
        localFolder.moveMessages(Collections.singletonList(message), draftsFolder);
    }

    private void notifySynchronizeMailboxFailed(Account account, Folder localFolder, Exception exception) {
        String folderName = localFolder.getName();
        String errorMessage = getRootCauseMessage(exception);
        for (MessagingListener listener : getListeners()) {
            listener.synchronizeMailboxFailed(account, folderName, errorMessage);
        }
    }

    public void getAccountStats(final Context context, final Account account,
            final MessagingListener listener) {

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    AccountStats stats = account.getStats(context);
                    listener.accountStatusChanged(account, stats);
                } catch (MessagingException me) {
                    Log.e(K9.LOG_TAG, "Count not get unread count for account " +
                            account.getDescription(), me);
                }

            }
        });
    }

    public void getSearchAccountStats(final SearchAccount searchAccount,
            final MessagingListener listener) {

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                getSearchAccountStatsSynchronous(searchAccount, listener);
            }
        });
    }

    public AccountStats getSearchAccountStatsSynchronous(final SearchAccount searchAccount,
            final MessagingListener listener) {

        Preferences preferences = Preferences.getPreferences(context);
        LocalSearch search = searchAccount.getRelatedSearch();

        // Collect accounts that belong to the search
        String[] accountUuids = search.getAccountUuids();
        List<Account> accounts;
        if (search.searchAllAccounts()) {
            accounts = preferences.getAccounts();
        } else {
            accounts = new ArrayList<>(accountUuids.length);
            for (int i = 0, len = accountUuids.length; i < len; i++) {
                String accountUuid = accountUuids[i];
                accounts.set(i, preferences.getAccount(accountUuid));
            }
        }

        ContentResolver cr = context.getContentResolver();

        int unreadMessageCount = 0;
        int flaggedMessageCount = 0;

        String[] projection = {
                StatsColumns.UNREAD_COUNT,
                StatsColumns.FLAGGED_COUNT
        };

        for (Account account : accounts) {
            StringBuilder query = new StringBuilder();
            List<String> queryArgs = new ArrayList<>();
            ConditionsTreeNode conditions = search.getConditions();
            SqlQueryBuilder.buildWhereClause(account, conditions, query, queryArgs);

            String selection = query.toString();
            String[] selectionArgs = queryArgs.toArray(new String[queryArgs.size()]);

            Uri uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI,
                    "account/" + account.getUuid() + "/stats");

            // Query content provider to get the account stats
            Cursor cursor = cr.query(uri, projection, selection, selectionArgs, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    unreadMessageCount += cursor.getInt(0);
                    flaggedMessageCount += cursor.getInt(1);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        // Create AccountStats instance...
        AccountStats stats = new AccountStats();
        stats.unreadMessageCount = unreadMessageCount;
        stats.flaggedMessageCount = flaggedMessageCount;

        // ...and notify the listener
        if (listener != null) {
            listener.accountStatusChanged(searchAccount, stats);
        }

        return stats;
    }

    public void getFolderUnreadMessageCount(final Account account, final String folderName,
                                            final MessagingListener l) {
        Runnable unreadRunnable = new Runnable() {
            @Override
            public void run() {

                int unreadMessageCount = 0;
                try {
                    Folder localFolder = account.getLocalStore().getFolder(folderName);
                    unreadMessageCount = localFolder.getUnreadMessageCount();
                } catch (MessagingException me) {
                    Log.e(K9.LOG_TAG, "Count not get unread count for account " + account.getDescription(), me);
                }
                l.folderStatusChanged(account, folderName, unreadMessageCount);
            }
        };


        put("getFolderUnread:" + account.getDescription() + ":" + folderName, l, unreadRunnable);
    }



    public boolean isMoveCapable(MessageReference messageReference) {
        return !messageReference.getUid().startsWith(K9.LOCAL_UID_PREFIX);
    }
    public boolean isCopyCapable(MessageReference message) {
        return isMoveCapable(message);
    }

    public boolean isMoveCapable(final Account account) {
        try {
            Store localStore = account.getLocalStore();
            Store remoteStore = account.getRemoteStore();
            return localStore.isMoveCapable() && remoteStore.isMoveCapable();
        } catch (MessagingException me) {

            Log.e(K9.LOG_TAG, "Exception while ascertaining move capability", me);
            return false;
        }
    }
    public boolean isCopyCapable(final Account account) {
        try {
            Store localStore = account.getLocalStore();
            Store remoteStore = account.getRemoteStore();
            return localStore.isCopyCapable() && remoteStore.isCopyCapable();
        } catch (MessagingException me) {
            Log.e(K9.LOG_TAG, "Exception while ascertaining copy capability", me);
            return false;
        }
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
                            addErrorMessage(account, "Exception while moving messages", e);
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
                            addErrorMessage(account, "Exception while copying messages", e);
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
            Store remoteStore = account.getRemoteStore();
            if (!isCopy && (!remoteStore.isMoveCapable() || !localStore.isMoveCapable())) {
                return;
            }
            if (isCopy && (!remoteStore.isCopyCapable() || !localStore.isCopyCapable())) {
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

                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "moveOrCopyMessageSynchronous: source folder = " + srcFolder
                          + ", " + messages.size() + " messages, " + ", destination folder = " + destFolder + ", isCopy = " + isCopy);

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

                Set<String> origUidKeys = origUidMap.keySet();
                queueMoveOrCopy(account, srcFolder, destFolder, isCopy, origUidKeys.toArray(new String[origUidKeys.size()]), uidMap);
            }

            processPendingCommands(account);
        } catch (UnavailableStorageException e) {
            Log.i(K9.LOG_TAG, "Failed to move/copy message because storage is not available - trying again later.");
            throw new UnavailableAccountException(e);
        } catch (MessagingException me) {
            addErrorMessage(account, null, me);

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
            localFolder = localStore.getFolder(account.getDraftsFolderName());
            localFolder.open(Folder.OPEN_MODE_RW);
            String uid = localFolder.getMessageUidById(id);
            if (uid != null) {
                MessageReference messageReference = new MessageReference(
                        account.getUuid(), account.getDraftsFolderName(), uid, null);
                deleteMessage(messageReference, null);
            }
        } catch (MessagingException me) {
            addErrorMessage(account, null, me);
        } finally {
            closeFolder(localFolder);
        }
    }

    public void deleteThreads(final List<MessageReference> messages) {
        actOnMessagesGroupedByAccountAndFolder(messages, new MessageActor() {
            @Override
            public void act(final Account account, final LocalFolder messageFolder, final List<LocalMessage> accountMessages) {
                suppressMessages(account, accountMessages);

                putBackground("deleteThreads", null, new Runnable() {
                    @Override
                    public void run() {
                        deleteThreadsSynchronous(account, messageFolder.getName(), accountMessages);
                    }
                });
            }
        });
    }

    private void deleteThreadsSynchronous(Account account, String folderName, List<? extends Message> messages) {
        try {
            List<Message> messagesToDelete = collectMessagesInThreads(account, messages);

            deleteMessagesSynchronous(account, folderName,
                    messagesToDelete, null);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Something went wrong while deleting threads", e);
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
            public void act(final Account account, final LocalFolder messageFolder, final List<LocalMessage> accountMessages) {
                suppressMessages(account, accountMessages);

                putBackground("deleteMessages", null, new Runnable() {
                    @Override
                    public void run() {
                        deleteMessagesSynchronous(account, messageFolder.getName(), accountMessages, listener);
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

    private void deleteMessagesSynchronous(final Account account, final String folder, final List<? extends Message> messages,
                                           MessagingListener listener) {
        Folder localFolder = null;
        Folder localTrashFolder = null;
        String[] uids = getUidsFromMessages(messages);
        try {
            //We need to make these callbacks before moving the messages to the trash
            //as messages get a new UID after being moved
            for (Message message : messages) {
                for (MessagingListener l : getListeners(listener)) {
                    l.messageDeleted(account, folder, message);
                }
            }
            Store localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folder);
            Map<String, String> uidMap = null;
            if (folder.equals(account.getTrashFolderName()) || !account.hasTrashFolder()) {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Deleting messages in trash folder or trash set to -None-, not copying");

                localFolder.setFlags(messages, Collections.singleton(Flag.DELETED), true);
            } else {
                localTrashFolder = localStore.getFolder(account.getTrashFolderName());
                if (!localTrashFolder.exists()) {
                    localTrashFolder.create(Folder.FolderType.HOLDS_MESSAGES);
                }
                if (localTrashFolder.exists()) {
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "Deleting messages in normal folder, moving");

                    uidMap = localFolder.moveMessages(messages, localTrashFolder);

                }
            }

            for (MessagingListener l : getListeners()) {
                l.folderStatusChanged(account, folder, localFolder.getUnreadMessageCount());
                if (localTrashFolder != null) {
                    l.folderStatusChanged(account, account.getTrashFolderName(), localTrashFolder.getUnreadMessageCount());
                }
            }

            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "Delete policy for account " + account.getDescription() + " is " + account.getDeletePolicy());

            if (folder.equals(account.getOutboxFolderName())) {
                for (Message message : messages) {
                    // If the message was in the Outbox, then it has been copied to local Trash, and has
                    // to be copied to remote trash
                    PendingCommand command = new PendingCommand();
                    command.command = PENDING_COMMAND_APPEND;
                    command.arguments =
                        new String[] {
                        account.getTrashFolderName(),
                        message.getUid()
                    };
                    queuePendingCommand(account, command);
                }
                processPendingCommands(account);
            } else if (account.getDeletePolicy() == DeletePolicy.ON_DELETE) {
                if (folder.equals(account.getTrashFolderName())) {
                    queueSetFlag(account, folder, Boolean.toString(true), Flag.DELETED.toString(), uids);
                } else {
                    queueMoveOrCopy(account, folder, account.getTrashFolderName(), false, uids, uidMap);
                }
                processPendingCommands(account);
            } else if (account.getDeletePolicy() == DeletePolicy.MARK_AS_READ) {
                queueSetFlag(account, folder, Boolean.toString(true), Flag.SEEN.toString(), uids);
                processPendingCommands(account);
            } else {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Delete policy " + account.getDeletePolicy() + " prevents delete from server");
            }

            unsuppressMessages(account, messages);
        } catch (UnavailableStorageException e) {
            Log.i(K9.LOG_TAG, "Failed to delete message because storage is not available - trying again later.");
            throw new UnavailableAccountException(e);
        } catch (MessagingException me) {
            addErrorMessage(account, null, me);

            throw new RuntimeException("Error deleting message from local store.", me);
        } finally {
            closeFolder(localFolder);
            closeFolder(localTrashFolder);
        }
    }

    private static String[] getUidsFromMessages(List <? extends Message> messages) {
        String[] uids = new String[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            uids[i] = messages.get(i).getUid();
        }
        return uids;
    }

    @SuppressWarnings("UnusedParameters") // for consistency with other PendingCommand methods
    private void processPendingEmptyTrash(PendingCommand command, Account account) throws MessagingException {
        Store remoteStore = account.getRemoteStore();

        Folder remoteFolder = remoteStore.getFolder(account.getTrashFolderName());
        try {
            if (remoteFolder.exists()) {
                remoteFolder.open(Folder.OPEN_MODE_RW);
                remoteFolder.setFlags(Collections.singleton(Flag.DELETED), true);
                if (Expunge.EXPUNGE_IMMEDIATELY == account.getExpungePolicy()) {
                    remoteFolder.expunge();
                }

                // When we empty trash, we need to actually synchronize the folder
                // or local deletes will never get cleaned up
                synchronizeFolder(account, remoteFolder, true, 0, null);
                compact(account, null);


            }
        } finally {
            closeFolder(remoteFolder);
        }
    }

    public void emptyTrash(final Account account, MessagingListener listener) {
        putBackground("emptyTrash", listener, new Runnable() {
            @Override
            public void run() {
                LocalFolder localFolder = null;
                try {
                    Store localStore = account.getLocalStore();
                    localFolder = (LocalFolder) localStore.getFolder(account.getTrashFolderName());
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
                        PendingCommand command = new PendingCommand();
                        command.command = PENDING_COMMAND_EMPTY_TRASH;
                        command.arguments = EMPTY_STRING_ARRAY;
                        queuePendingCommand(account, command);
                        processPendingCommands(account);
                    }
                } catch (UnavailableStorageException e) {
                    Log.i(K9.LOG_TAG, "Failed to empty trash because storage is not available - trying again later.");
                    throw new UnavailableAccountException(e);
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "emptyTrash failed", e);
                    addErrorMessage(account, null, e);
                } finally {
                    closeFolder(localFolder);
                }
            }
        });
    }

    /**
     * Find out whether the account type only supports a local Trash folder.
     *
     * <p>Note: Currently this is only the case for POP3 accounts.</p>
     *
     * @param account
     *         The account to check.
     *
     * @return {@code true} if the account only has a local Trash folder that is not synchronized
     *         with a folder on the server. {@code false} otherwise.
     *
     * @throws MessagingException
     *         In case of an error.
     */
    private boolean isTrashLocalOnly(Account account) throws MessagingException {
        // TODO: Get rid of the tight coupling once we properly support local folders
        return (account.getRemoteStore() instanceof Pop3Store);
    }

    public void sendAlternate(Context context, Account account, LocalMessage message) {
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "Got message " + account.getDescription() + ":" + message.getFolder()
                  + ":" + message.getUid() + " for sendAlternate");

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
        context.startActivity(Intent.createChooser(msg, context.getString(R.string.send_alternate_chooser_title)));
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
                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Starting mail check");
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
                    Log.e(K9.LOG_TAG, "Unable to synchronize mail", e);
                    addErrorMessage(account, null, e);
                }
                putBackground("finalize sync", null, new Runnable() {
                    @Override
                    public void run() {

                        if (K9.DEBUG)
                            Log.i(K9.LOG_TAG, "Finished mail sync");

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
            if (K9.DEBUG) {
                Log.i(K9.LOG_TAG, "Skipping synchronizing unavailable account " + account.getDescription());
            }
            return;
        }
        final long accountInterval = account.getAutomaticCheckIntervalMinutes() * 60 * 1000;
        if (!ignoreLastCheckedTime && accountInterval <= 0) {
            if (K9.DEBUG)
                Log.i(K9.LOG_TAG, "Skipping synchronizing account " + account.getDescription());
            return;
        }

        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Synchronizing account " + account.getDescription());

        account.setRingNotified(false);

        sendPendingMessages(account, listener);

        try {
            Account.FolderMode aDisplayMode = account.getFolderDisplayMode();
            Account.FolderMode aSyncMode = account.getFolderSyncMode();

            Store localStore = account.getLocalStore();
            for (final Folder folder : localStore.getPersonalNamespaces(false)) {
                folder.open(Folder.OPEN_MODE_RW);

                Folder.FolderClass fDisplayClass = folder.getDisplayClass();
                Folder.FolderClass fSyncClass = folder.getSyncClass();

                if (modeMismatch(aDisplayMode, fDisplayClass)) {
                    // Never sync a folder that isn't displayed
                    /*
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Not syncing folder " + folder.getName() +
                              " which is in display mode " + fDisplayClass + " while account is in display mode " + aDisplayMode);
                    */

                    continue;
                }

                if (modeMismatch(aSyncMode, fSyncClass)) {
                    // Do not sync folders in the wrong class
                    /*
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Not syncing folder " + folder.getName() +
                              " which is in sync mode " + fSyncClass + " while account is in sync mode " + aSyncMode);
                    */

                    continue;
                }
                synchronizeFolder(account, folder, ignoreLastCheckedTime, accountInterval, listener);
            }
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to synchronize account " + account.getName(), e);
            addErrorMessage(account, null, e);
        } finally {
            putBackground("clear notification flag for " + account.getDescription(), null, new Runnable() {
                @Override
                public void run() {
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Clearing notification flag for " + account.getDescription());
                    account.setRingNotified(false);
                    try {
                        AccountStats stats = account.getStats(context);
                        if (stats == null || stats.unreadMessageCount == 0) {
                            notificationController.clearNewMailNotifications(account);
                        }
                    } catch (MessagingException e) {
                        Log.e(K9.LOG_TAG, "Unable to getUnreadMessageCount for account: " + account, e);
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


        if (K9.DEBUG)
            Log.v(K9.LOG_TAG, "Folder " + folder.getName() + " was last synced @ " +
                  new Date(folder.getLastChecked()));

        if (!ignoreLastCheckedTime && folder.getLastChecked() >
                (System.currentTimeMillis() - accountInterval)) {
            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "Not syncing folder " + folder.getName()
                      + ", previously synced @ " + new Date(folder.getLastChecked())
                      + " which would be too recent for the account period");

            return;
        }
        putBackground("sync" + folder.getName(), null, new Runnable() {
            @Override
            public void run() {
                LocalFolder tLocalFolder = null;
                try {
                    // In case multiple Commands get enqueued, don't run more than
                    // once
                    final LocalStore localStore = account.getLocalStore();
                    tLocalFolder = localStore.getFolder(folder.getName());
                    tLocalFolder.open(Folder.OPEN_MODE_RW);

                    if (!ignoreLastCheckedTime && tLocalFolder.getLastChecked() >
                    (System.currentTimeMillis() - accountInterval)) {
                        if (K9.DEBUG)
                            Log.v(K9.LOG_TAG, "Not running Command for folder " + folder.getName()
                                  + ", previously synced @ " + new Date(folder.getLastChecked())
                                  + " which would be too recent for the account period");
                        return;
                    }
                    showFetchingMailNotificationIfNecessary(account, folder);
                    try {
                        synchronizeMailboxSynchronous(account, folder.getName(), listener, null);
                    } finally {
                        clearFetchingMailNotificationIfNecessary(account);
                    }
                } catch (Exception e) {

                    Log.e(K9.LOG_TAG, "Exception while processing folder " +
                          account.getDescription() + ":" + folder.getName(), e);
                    addErrorMessage(account, null, e);
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
                    Log.i(K9.LOG_TAG, "Failed to compact account because storage is not available - trying again later.");
                    throw new UnavailableAccountException(e);
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Failed to compact account " + account.getDescription(), e);
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
                    Log.i(K9.LOG_TAG, "Failed to clear account because storage is not available - trying again later.");
                    throw new UnavailableAccountException(e);
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Failed to clear account " + account.getDescription(), e);
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
                    Log.i(K9.LOG_TAG, "Failed to recreate an account because storage is not available - trying again later.");
                    throw new UnavailableAccountException(e);
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Failed to recreate account " + account.getDescription(), e);
                }
            }
        });
    }


    private boolean shouldNotifyForMessage(Account account, LocalFolder localFolder, Message message) {
        // If we don't even have an account name, don't show the notification.
        // (This happens during initial account setup)
        if (account.getName() == null) {
            return false;
        }

        // Do not notify if the user does not have notifications enabled or if the message has
        // been read.
        if (!account.isNotifyNewMail() || message.isSet(Flag.SEEN)) {
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

        // If the account is a POP3 account and the message is older than the oldest message we've
        // previously seen, then don't notify about it.
        if (account.getStoreUri().startsWith("pop3") &&
                message.olderThan(new Date(account.getLatestOldMessageSeenTime()))) {
            return false;
        }

        // No notification for new messages in Trash, Drafts, Spam or Sent folder.
        // But do notify if it's the INBOX (see issue 1817).
        Folder folder = message.getFolder();
        if (folder != null) {
            String folderName = folder.getName();
            if (!account.getInboxFolderName().equals(folderName) &&
                    (account.getTrashFolderName().equals(folderName)
                     || account.getDraftsFolderName().equals(folderName)
                     || account.getSpamFolderName().equals(folderName)
                     || account.getSentFolderName().equals(folderName))) {
                return false;
            }
        }

        if (message.getUid() != null && localFolder.getLastUid() != null) {
            try {
                Integer messageUid = Integer.parseInt(message.getUid());
                if (messageUid <= localFolder.getLastUid()) {
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "Message uid is " + messageUid + ", max message uid is " +
                              localFolder.getLastUid() + ".  Skipping notification.");
                    return false;
                }
            } catch (NumberFormatException e) {
                // Nothing to be done here.
            }
        }

        // Don't notify if the sender address matches one of our identities and the user chose not
        // to be notified for such messages.
        if (account.isAnIdentity(message.getFrom()) && !account.isNotifySelfNewMail()) {
            return false;
        }

        if (account.isNotifyContactsMailOnly() && !contacts.isAnyInContacts(message.getFrom())) {
            return false;
        }

        return true;
    }

    public void deleteAccount(Account account) {
        notificationController.clearNewMailNotifications(account);
        memorizingMessagingListener.removeAccount(account);
    }

    /**
     * Save a draft message.
     * @param account Account we are saving for.
     * @param message Message to save.
     * @return Message representing the entry in the local store.
     */
    public Message saveDraft(final Account account, final Message message, long existingDraftId, boolean saveRemotely) {
        Message localMessage = null;
        try {
            LocalStore localStore = account.getLocalStore();
            LocalFolder localFolder = localStore.getFolder(account.getDraftsFolderName());
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
                PendingCommand command = new PendingCommand();
                command.command = PENDING_COMMAND_APPEND;
                command.arguments = new String[] {
                        localFolder.getName(),
                        localMessage.getUid()
                };
                queuePendingCommand(account, command);
                processPendingCommands(account);
            }

        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to save message as draft.", e);
            addErrorMessage(account, null, e);
        }
        return localMessage;
    }

    public long getId(Message message) {
        long id;
        if (message instanceof LocalMessage) {
            id = message.getId();
        } else {
            Log.w(K9.LOG_TAG, "MessagingController.getId() called without a LocalMessage");
            id = INVALID_MESSAGE_ID;
        }

        return id;
    }

    private boolean modeMismatch(Account.FolderMode aMode, Folder.FolderClass fMode) {
        if (aMode == Account.FolderMode.NONE
                || (aMode == Account.FolderMode.FIRST_CLASS &&
                    fMode != Folder.FolderClass.FIRST_CLASS)
                || (aMode == Account.FolderMode.FIRST_AND_SECOND_CLASS &&
                    fMode != Folder.FolderClass.FIRST_CLASS &&
                    fMode != Folder.FolderClass.SECOND_CLASS)
                || (aMode == Account.FolderMode.NOT_SECOND_CLASS &&
                    fMode == Folder.FolderClass.SECOND_CLASS)) {
            return true;
        } else {
            return false;
        }
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

            Store localStore = account.getLocalStore();
            for (final Folder folder : localStore.getPersonalNamespaces(false)) {
                if (folder.getName().equals(account.getErrorFolderName())
                        || folder.getName().equals(account.getOutboxFolderName())) {
                    /*
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Not pushing folder " + folder.getName() +
                              " which should never be pushed");
                    */

                    continue;
                }
                folder.open(Folder.OPEN_MODE_RW);

                Folder.FolderClass fDisplayClass = folder.getDisplayClass();
                Folder.FolderClass fPushClass = folder.getPushClass();

                if (modeMismatch(aDisplayMode, fDisplayClass)) {
                    // Never push a folder that isn't displayed
                    /*
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Not pushing folder " + folder.getName() +
                              " which is in display class " + fDisplayClass + " while account is in display mode " + aDisplayMode);
                    */

                    continue;
                }

                if (modeMismatch(aPushMode, fPushClass)) {
                    // Do not push folders in the wrong class
                    /*
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Not pushing folder " + folder.getName() +
                              " which is in push mode " + fPushClass + " while account is in push mode " + aPushMode);
                    */

                    continue;
                }
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "Starting pusher for " + account.getDescription() + ":" + folder.getName());

                names.add(folder.getName());
            }

            if (!names.isEmpty()) {
                PushReceiver receiver = new MessagingControllerPushReceiver(context, account, this);
                int maxPushFolders = account.getMaxPushFolders();

                if (names.size() > maxPushFolders) {
                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Count of folders to push for account " + account.getDescription() + " is " + names.size()
                              + ", greater than limit of " + maxPushFolders + ", truncating");

                    names = names.subList(0, maxPushFolders);
                }

                try {
                    Store store = account.getRemoteStore();
                    if (!store.isPushCapable()) {
                        if (K9.DEBUG)
                            Log.i(K9.LOG_TAG, "Account " + account.getDescription() + " is not push capable, skipping");

                        return false;
                    }
                    Pusher pusher = store.getPusher(receiver);
                    if (pusher != null) {
                        Pusher oldPusher  = pushers.putIfAbsent(account, pusher);
                        if (oldPusher == null) {
                            pusher.start(names);
                        }
                    }
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Could not get remote store", e);
                    return false;
                }

                return true;
            } else {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "No folders are configured for pushing in account " + account.getDescription());
                return false;
            }

        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Got exception while setting up pushing", e);
        }
        return false;
    }

    public void stopAllPushing() {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Stopping all pushers");

        Iterator<Pusher> iter = pushers.values().iterator();
        while (iter.hasNext()) {
            Pusher pusher = iter.next();
            iter.remove();
            pusher.stop();
        }
    }

    public void messagesArrived(final Account account, final Folder remoteFolder, final List<Message> messages, final boolean flagSyncOnly) {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Got new pushed email messages for account " + account.getDescription()
                  + ", folder " + remoteFolder.getName());

        final CountDownLatch latch = new CountDownLatch(1);
        putBackground("Push messageArrived of account " + account.getDescription()
        + ", folder " + remoteFolder.getName(), null, new Runnable() {
            @Override
            public void run() {
                LocalFolder localFolder = null;
                try {
                    LocalStore localStore = account.getLocalStore();
                    localFolder = localStore.getFolder(remoteFolder.getName());
                    localFolder.open(Folder.OPEN_MODE_RW);

                    account.setRingNotified(false);
                    int newCount = downloadMessages(account, remoteFolder, localFolder, messages, flagSyncOnly, true);

                    int unreadMessageCount = localFolder.getUnreadMessageCount();

                    localFolder.setLastPush(System.currentTimeMillis());
                    localFolder.setStatus(null);

                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "messagesArrived newCount = " + newCount + ", unread count = " + unreadMessageCount);

                    if (unreadMessageCount == 0) {
                        notificationController.clearNewMailNotifications(account);
                    }

                    for (MessagingListener l : getListeners()) {
                        l.folderStatusChanged(account, remoteFolder.getName(), unreadMessageCount);
                    }

                } catch (Exception e) {
                    String rootMessage = getRootCauseMessage(e);
                    String errorMessage = "Push failed: " + rootMessage;
                    try {
                        localFolder.setStatus(errorMessage);
                    } catch (Exception se) {
                        Log.e(K9.LOG_TAG, "Unable to set failed status on localFolder", se);
                    }
                    for (MessagingListener l : getListeners()) {
                        l.synchronizeMailboxFailed(account, remoteFolder.getName(), errorMessage);
                    }
                    addErrorMessage(account, null, e);
                } finally {
                    closeFolder(localFolder);
                    latch.countDown();
                }

            }
        });
        try {
            latch.await();
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Interrupted while awaiting latch release", e);
        }
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "MessagingController.messagesArrivedLatch released");
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

    public void clearCertificateErrorNotifications(Account account, CheckDirection direction) {
        boolean incoming = (direction == CheckDirection.INCOMING);
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
                String folderName = folderEntry.getKey();
                List<MessageReference> messageList = folderEntry.getValue();
                actOnMessageGroup(account, folderName, messageList, actor);
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
            String folderName = message.getFolderName();

            Map<String, List<MessageReference>> folderMap = accountMap.get(accountUuid);
            if (folderMap == null) {
                folderMap = new HashMap<>();
                accountMap.put(accountUuid, folderMap);
            }
            List<MessageReference> messageList = folderMap.get(folderName);
            if (messageList == null) {
                messageList = new LinkedList<>();
                folderMap.put(folderName, messageList);
            }

            messageList.add(message);
        }
        return accountMap;
    }

    private void actOnMessageGroup(
            Account account, String folderName, List<MessageReference> messageReferences, MessageActor actor) {
        try {
            LocalFolder messageFolder = account.getLocalStore().getFolder(folderName);
            List<LocalMessage> localMessages = messageFolder.getMessagesByReference(messageReferences);
            actor.act(account, messageFolder, localMessages);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Error loading account?!", e);
        }

    }

    private interface MessageActor {
        void act(Account account, LocalFolder messageFolder, List<LocalMessage> messages);
    }
}
