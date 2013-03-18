package com.fsck.k9.controller;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Application;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationHideSubject;
import com.fsck.k9.K9.Intents;
import com.fsck.k9.K9.NotificationQuickDelete;
import com.fsck.k9.NotificationSetting;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.FolderList;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.NotificationDeleteConfirmation;
import com.fsck.k9.activity.setup.AccountSetupIncoming;
import com.fsck.k9.activity.setup.AccountSetupOutgoing;
import com.fsck.k9.cache.EmailProviderCache;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.NotificationBuilder;
import com.fsck.k9.helper.power.TracingPowerManager;
import com.fsck.k9.helper.power.TracingPowerManager.TracingWakeLock;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Folder.FolderType;
import com.fsck.k9.mail.Folder.OpenMode;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.Pusher;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.LocalStore.LocalFolder;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;
import com.fsck.k9.mail.store.LocalStore.PendingCommand;
import com.fsck.k9.mail.store.Pop3Store;
import com.fsck.k9.mail.store.UnavailableAccountException;
import com.fsck.k9.mail.store.UnavailableStorageException;
import com.fsck.k9.provider.EmailProvider;
import com.fsck.k9.provider.EmailProvider.StatsColumns;
import com.fsck.k9.search.ConditionsTreeNode;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.fsck.k9.search.SearchSpecification;
import com.fsck.k9.search.SearchSpecification.Attribute;
import com.fsck.k9.search.SearchSpecification.Searchfield;
import com.fsck.k9.search.SqlQueryBuilder;
import com.fsck.k9.service.NotificationActionService;


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
public class MessagingController implements Runnable {
    public static final long INVALID_MESSAGE_ID = -1;

    /**
     * Immutable empty {@link String} array
     */
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Immutable empty {@link Message} array
     */
    private static final Message[] EMPTY_MESSAGE_ARRAY = new Message[0];

    /**
     * Immutable empty {@link Folder} array
     */
    private static final Folder[] EMPTY_FOLDER_ARRAY = new Folder[0];

    /**
     * The maximum message size that we'll consider to be "small". A small message is downloaded
     * in full immediately instead of in pieces. Anything over this size will be downloaded in
     * pieces with attachments being left off completely and downloaded on demand.
     *
     *
     * 25k for a "small" message was picked by educated trial and error.
     * http://answers.google.com/answers/threadview?id=312463 claims that the
     * average size of an email is 59k, which I feel is too large for our
     * blind download. The following tests were performed on a download of
     * 25 random messages.
     * <pre>
     * 5k - 61 seconds,
     * 25k - 51 seconds,
     * 55k - 53 seconds,
     * </pre>
     * So 25k gives good performance and a reasonable data footprint. Sounds good to me.
     */

    private static final String PENDING_COMMAND_MOVE_OR_COPY = "com.fsck.k9.MessagingController.moveOrCopy";
    private static final String PENDING_COMMAND_MOVE_OR_COPY_BULK = "com.fsck.k9.MessagingController.moveOrCopyBulk";
    private static final String PENDING_COMMAND_MOVE_OR_COPY_BULK_NEW = "com.fsck.k9.MessagingController.moveOrCopyBulkNew";
    private static final String PENDING_COMMAND_EMPTY_TRASH = "com.fsck.k9.MessagingController.emptyTrash";
    private static final String PENDING_COMMAND_SET_FLAG_BULK = "com.fsck.k9.MessagingController.setFlagBulk";
    private static final String PENDING_COMMAND_SET_FLAG = "com.fsck.k9.MessagingController.setFlag";
    private static final String PENDING_COMMAND_APPEND = "com.fsck.k9.MessagingController.append";
    private static final String PENDING_COMMAND_MARK_ALL_AS_READ = "com.fsck.k9.MessagingController.markAllAsRead";
    private static final String PENDING_COMMAND_EXPUNGE = "com.fsck.k9.MessagingController.expunge";

    public static class UidReverseComparator implements Comparator<Message> {
        @Override
        public int compare(Message o1, Message o2) {
            if (o1 == null || o2 == null || o1.getUid() == null || o2.getUid() == null) {
                return 0;
            }
            int id1, id2;
            try {
                id1 = Integer.parseInt(o1.getUid());
                id2 = Integer.parseInt(o2.getUid());
            } catch (NumberFormatException e) {
                return 0;
            }
            //reversed intentionally.
            if (id1 < id2)
                return 1;
            if (id1 > id2)
                return -1;
            return 0;
        }
    }

    /**
     * Maximum number of unsynced messages to store at once
     */
    private static final int UNSYNC_CHUNK_SIZE = 5;

    private static MessagingController inst = null;
    private BlockingQueue<Command> mCommands = new PriorityBlockingQueue<Command>();

    private Thread mThread;
    private Set<MessagingListener> mListeners = new CopyOnWriteArraySet<MessagingListener>();

    private final ConcurrentHashMap<String, AtomicInteger> sendCount = new ConcurrentHashMap<String, AtomicInteger>();

    ConcurrentHashMap<Account, Pusher> pushers = new ConcurrentHashMap<Account, Pusher>();

    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private MessagingListener checkMailListener = null;

    private MemorizingListener memorizingListener = new MemorizingListener();

    private boolean mBusy;

    /**
     *  {@link K9}
     */
    private Application mApplication;

    /**
     * A holder class for pending notification data
     *
     * This class holds all pieces of information for constructing
     * a notification with message preview.
     */
    private static class NotificationData {
        /** Number of unread messages before constructing the notification */
        int unreadBeforeNotification;
        /**
         * List of messages that should be used for the inbox-style overview.
         * It's sorted from newest to oldest message.
         * Don't modify this list directly, but use {@link addMessage} and
         * {@link removeMatchingMessage} instead.
         */
        LinkedList<Message> messages;
        /**
         * List of references for messages that the user is still to be notified of,
         * but which don't fit into the inbox style anymore. It's sorted from newest
         * to oldest message.
         */
        LinkedList<MessageReference> droppedMessages;

        /**
         * Maximum number of messages to keep for the inbox-style overview.
         * As of Jellybean, phone notifications show a maximum of 5 lines, while tablet
         * notifications show 7 lines. To make sure no lines are silently dropped,
         * we default to 5 lines.
         */
        private final static int MAX_MESSAGES = 5;

        /**
         * Constructs a new data instance.
         *
         * @param unread Number of unread messages prior to instance construction
         */
        public NotificationData(int unread) {
            unreadBeforeNotification = unread;
            droppedMessages = new LinkedList<MessageReference>();
            messages = new LinkedList<Message>();
        }

        /**
         * Adds a new message to the list of pending messages for this notification.
         *
         * The implementation will take care of keeping a meaningful amount of
         * messages in {@link #messages}.
         *
         * @param m The new message to add.
         */
        public void addMessage(Message m) {
            while (messages.size() >= MAX_MESSAGES) {
                Message dropped = messages.removeLast();
                droppedMessages.addFirst(dropped.makeMessageReference());
            }
            messages.addFirst(m);
        }

        /**
         * Remove a certain message from the message list.
         *
         * @param context A context.
         * @param ref Reference of the message to remove
         * @return true if message was found and removed, false otherwise
         */
        public boolean removeMatchingMessage(Context context, MessageReference ref) {
            for (MessageReference dropped : droppedMessages) {
                if (dropped.equals(ref)) {
                    droppedMessages.remove(dropped);
                    return true;
                }
            }

            for (Message message : messages) {
                if (message.makeMessageReference().equals(ref)) {
                    if (messages.remove(message) && !droppedMessages.isEmpty()) {
                        Message restoredMessage = droppedMessages.getFirst().restoreToLocalMessage(context);
                        if (restoredMessage != null) {
                            messages.addLast(restoredMessage);
                            droppedMessages.removeFirst();
                        }
                    }
                    return true;
                }
            }

            return false;
        }

        /**
         * Gets a list of references for all pending messages for the notification.
         *
         * @return Message reference list
         */
        public ArrayList<MessageReference> getAllMessageRefs() {
            ArrayList<MessageReference> refs = new ArrayList<MessageReference>();
            for (Message m : messages) {
                refs.add(m.makeMessageReference());
            }
            refs.addAll(droppedMessages);
            return refs;
        }

        /**
         * Gets the total number of messages the user is to be notified of.
         *
         * @return Amount of new messages the notification notifies for
         */
        public int getNewMessageCount() {
            return messages.size() + droppedMessages.size();
        }
    };

    // Key is accountNumber
    private ConcurrentHashMap<Integer, NotificationData> notificationData = new ConcurrentHashMap<Integer, NotificationData>();

    private static final Flag[] SYNC_FLAGS = new Flag[] { Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED, Flag.FORWARDED };


    private void suppressMessages(Account account, List<Message> messages) {
        EmailProviderCache cache = EmailProviderCache.getCache(account.getUuid(),
                mApplication.getApplicationContext());
        cache.hideMessages(messages);
    }

    private void unsuppressMessages(Account account, Message[] messages) {
        EmailProviderCache cache = EmailProviderCache.getCache(account.getUuid(),
                mApplication.getApplicationContext());
        cache.unhideMessages(messages);
    }

    private boolean isMessageSuppressed(Account account, Message message) {
        LocalMessage localMessage = (LocalMessage) message;
        String accountUuid = account.getUuid();
        long messageId = localMessage.getId();
        long folderId = ((LocalFolder) localMessage.getFolder()).getId();

        EmailProviderCache cache = EmailProviderCache.getCache(accountUuid,
                mApplication.getApplicationContext());
        return cache.isMessageHidden(messageId, folderId);
    }

    private void setFlagInCache(final Account account, final List<Long> messageIds,
            final Flag flag, final boolean newState) {

        EmailProviderCache cache = EmailProviderCache.getCache(account.getUuid(),
                mApplication.getApplicationContext());
        String columnName = LocalStore.getColumnNameForFlag(flag);
        String value = Integer.toString((newState) ? 1 : 0);
        cache.setValueForMessages(messageIds, columnName, value);
    }

    private void removeFlagFromCache(final Account account, final List<Long> messageIds,
            final Flag flag) {

        EmailProviderCache cache = EmailProviderCache.getCache(account.getUuid(),
                mApplication.getApplicationContext());
        String columnName = LocalStore.getColumnNameForFlag(flag);
        cache.removeValueForMessages(messageIds, columnName);
    }

    private void setFlagForThreadsInCache(final Account account, final List<Long> threadRootIds,
            final Flag flag, final boolean newState) {

        EmailProviderCache cache = EmailProviderCache.getCache(account.getUuid(),
                mApplication.getApplicationContext());
        String columnName = LocalStore.getColumnNameForFlag(flag);
        String value = Integer.toString((newState) ? 1 : 0);
        cache.setValueForThreads(threadRootIds, columnName, value);
    }

    private void removeFlagForThreadsFromCache(final Account account, final List<Long> messageIds,
            final Flag flag) {

        EmailProviderCache cache = EmailProviderCache.getCache(account.getUuid(),
                mApplication.getApplicationContext());
        String columnName = LocalStore.getColumnNameForFlag(flag);
        cache.removeValueForThreads(messageIds, columnName);
    }


    /**
     * @param application  {@link K9}
     */
    private MessagingController(Application application) {
        mApplication = application;
        mThread = new Thread(this);
        mThread.setName("MessagingController");
        mThread.start();
        if (memorizingListener != null) {
            addListener(memorizingListener);
        }
    }

    /**
     * Gets or creates the singleton instance of MessagingController. Application is used to
     * provide a Context to classes that need it.
     * @param application {@link K9}
     * @return
     */
    public synchronized static MessagingController getInstance(Application application) {
        if (inst == null) {
            inst = new MessagingController(application);
        }
        return inst;
    }

    public boolean isBusy() {
        return mBusy;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true) {
            String commandDescription = null;
            try {
                final Command command = mCommands.take();

                if (command != null) {
                    commandDescription = command.description;

                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Running " + (command.isForeground ? "Foreground" : "Background") + " command '" + command.description + "', seq = " + command.sequence);

                    mBusy = true;
                    try {
                        command.runnable.run();
                    } catch (UnavailableAccountException e) {
                        // retry later
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    sleep(30 * 1000);
                                    mCommands.put(command);
                                } catch (InterruptedException e) {
                                    Log.e(K9.LOG_TAG, "interrupted while putting a pending command for"
                                          + " an unavailable account back into the queue."
                                          + " THIS SHOULD NEVER HAPPEN.");
                                }
                            }
                        } .start();
                    }

                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, (command.isForeground ? "Foreground" : "Background") +
                              " Command '" + command.description + "' completed");

                    for (MessagingListener l : getListeners(command.listener)) {
                        l.controllerCommandCompleted(!mCommands.isEmpty());
                    }
                }
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Error running command '" + commandDescription + "'", e);
            }
            mBusy = false;
        }
    }

    private void put(String description, MessagingListener listener, Runnable runnable) {
        putCommand(mCommands, description, listener, runnable, true);
    }

    private void putBackground(String description, MessagingListener listener, Runnable runnable) {
        putCommand(mCommands, description, listener, runnable, false);
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
                command.isForeground = isForeground;
                queue.put(command);
                return;
            } catch (InterruptedException ie) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ne) {
                }
                e = ie;
            }
        }
        throw new Error(e);
    }


    public void addListener(MessagingListener listener) {
        mListeners.add(listener);
        refreshListener(listener);
    }

    public void refreshListener(MessagingListener listener) {
        if (memorizingListener != null && listener != null) {
            memorizingListener.refreshOther(listener);
        }
    }

    public void removeListener(MessagingListener listener) {
        mListeners.remove(listener);
    }

    public Set<MessagingListener> getListeners() {
        return mListeners;
    }


    public Set<MessagingListener> getListeners(MessagingListener listener) {
        if (listener == null) {
            return mListeners;
        }

        Set<MessagingListener> listeners = new HashSet<MessagingListener>(mListeners);
        listeners.add(listener);
        return listeners;

    }


    /**
     * Lists folders that are available locally and remotely. This method calls
     * listFoldersCallback for local folders before it returns, and then for
     * remote folders at some later point. If there are no local folders
     * includeRemote is forced by this method. This method should be called from
     * a Thread as it may take several seconds to list the local folders.
     * TODO this needs to cache the remote folder list
     *
     * @param account
     * @param listener
     * @throws MessagingException
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
     *
     * @param account
     * @param listener
     * @throws MessagingException
     */
    public void listFoldersSynchronous(final Account account, final boolean refreshRemote, final MessagingListener listener) {
        for (MessagingListener l : getListeners(listener)) {
            l.listFoldersStarted(account);
        }
        List <? extends Folder > localFolders = null;
        if (!account.isAvailable(mApplication)) {
            Log.i(K9.LOG_TAG, "not listing folders of unavailable account");
        } else {
            try {
                Store localStore = account.getLocalStore();
                localFolders = localStore.getPersonalNamespaces(false);

                Folder[] folderArray = localFolders.toArray(EMPTY_FOLDER_ARRAY);

                if (refreshRemote || localFolders.isEmpty()) {
                    doRefreshRemote(account, listener);
                    return;
                }

                for (MessagingListener l : getListeners(listener)) {
                    l.listFolders(account, folderArray);
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
                List <? extends Folder > localFolders = null;
                try {
                    Store store = account.getRemoteStore();

                    List <? extends Folder > remoteFolders = store.getPersonalNamespaces(false);

                    LocalStore localStore = account.getLocalStore();
                    HashSet<String> remoteFolderNames = new HashSet<String>();
                    List<LocalFolder> foldersToCreate = new LinkedList<LocalFolder>();

                    localFolders = localStore.getPersonalNamespaces(false);
                    HashSet<String> localFolderNames = new HashSet<String>();
                    for (Folder localFolder : localFolders) {
                        localFolderNames.add(localFolder.getName());
                    }
                    for (Folder remoteFolder : remoteFolders) {
                        if (localFolderNames.contains(remoteFolder.getName()) == false) {
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
                        if (!account.isSpecialFolder(localFolderName) && !remoteFolderNames.contains(localFolderName)) {
                            localFolder.delete(false);
                        }
                    }

                    localFolders = localStore.getPersonalNamespaces(false);
                    Folder[] folderArray = localFolders.toArray(EMPTY_FOLDER_ARRAY);

                    for (MessagingListener l : getListeners(listener)) {
                        l.listFolders(account, folderArray);
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
        });
    }

    /**
     * Find all messages in any local account which match the query 'query'
     * @throws MessagingException
     */
    public void searchLocalMessages(final LocalSearch search, final MessagingListener listener) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                searchLocalMessagesSynchronous(search, listener);
            }
        });
    }

    public void searchLocalMessagesSynchronous(final LocalSearch search, final MessagingListener listener) {
        final AccountStats stats = new AccountStats();
        final HashSet<String> uuidSet = new HashSet<String>(Arrays.asList(search.getAccountUuids()));
        Account[] accounts = Preferences.getPreferences(mApplication.getApplicationContext()).getAccounts();
        boolean allAccounts = uuidSet.contains(SearchSpecification.ALL_ACCOUNTS);

        // for every account we want to search do the query in the localstore
        for (final Account account : accounts) {

            if (!allAccounts && !uuidSet.contains(account.getUuid())) {
                continue;
            }

            // Collecting statistics of the search result
            MessageRetrievalListener retrievalListener = new MessageRetrievalListener() {
                @Override
                public void messageStarted(String message, int number, int ofTotal) {}
                @Override
                public void messagesFinished(int number) {}
                @Override
                public void messageFinished(Message message, int number, int ofTotal) {
                    if (!isMessageSuppressed(message.getFolder().getAccount(), message)) {
                        List<Message> messages = new ArrayList<Message>();

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
            final Flag[] requiredFlags, final Flag[] forbiddenFlags, final MessagingListener listener) {
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
    public void searchRemoteMessagesSynchronous(final String acctUuid, final String folderName, final String query,
            final Flag[] requiredFlags, final Flag[] forbiddenFlags, final MessagingListener listener) {
        final Account acct = Preferences.getPreferences(mApplication.getApplicationContext()).getAccount(acctUuid);

        if (listener != null) {
            listener.remoteSearchStarted(acct, folderName);
        }

        List<Message> extraResults = new ArrayList<Message>();
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
                listener.remoteSearchServerQueryComplete(acct, folderName, remoteMessages.size());
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
                    listener.remoteSearchFailed(acct, null, e.getMessage());
                }
                addErrorMessage(acct, null, e);
            }
        } finally {
            if (listener != null) {
                listener.remoteSearchFinished(acct, folderName, 0, extraResults);
            }
        }

    }

    public void loadSearchResults(final Account account, final String folderName, final List<Message> messages, final MessagingListener listener) {
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

    public void loadSearchResultsSynchronous(List<Message> messages, LocalFolder localFolder, Folder remoteFolder, MessagingListener listener) throws MessagingException {
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
                remoteFolder.fetch(new Message [] {message}, header, null);
                //fun fact: ImapFolder.fetch can't handle getting STRUCTURE at same time as headers
                remoteFolder.fetch(new Message [] {message}, structure, null);
                localFolder.appendMessages(new Message [] {message});
                localMsg = localFolder.getMessage(message.getUid());
            }

            if (listener != null) {
                listener.remoteSearchAddMessage(remoteFolder.getAccount(), remoteFolder.getName(), localMsg, i, messages.size());
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

    public void resetVisibleLimits(Collection<Account> accounts) {
        for (Account account : accounts) {
            account.resetVisibleLimits();
        }
    }

    /**
     * Start background synchronization of the specified folder.
     * @param account
     * @param folder
     * @param listener
     * @param providedRemoteFolder TODO
     */
    public void synchronizeMailbox(final Account account, final String folder, final MessagingListener listener, final Folder providedRemoteFolder) {
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
     * @param account
     * @param folder
     *
     * TODO Break this method up into smaller chunks.
     * @param providedRemoteFolder TODO
     */
    private void synchronizeMailboxSynchronous(final Account account, final String folder, final MessagingListener listener, Folder providedRemoteFolder) {
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
            localFolder.open(OpenMode.READ_WRITE);
            localFolder.updateLastUid();
            Message[] localMessages = localFolder.getMessages(null);
            HashMap<String, Message> localUidMap = new HashMap<String, Message>();
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

                remoteFolder.open(OpenMode.READ_WRITE);
                if (Account.EXPUNGE_ON_POLL.equals(account.getExpungePolicy())) {
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "SYNC: Expunging folder " + account.getDescription() + ":" + folder);
                    remoteFolder.expunge();
                }

            }

            /*
             * Get the remote message count.
             */
            int remoteMessageCount = remoteFolder.getMessageCount();

            int visibleLimit = localFolder.getVisibleLimit();

            if (visibleLimit < 0) {
                visibleLimit = K9.DEFAULT_VISIBLE_LIMIT;
            }

            Message[] remoteMessageArray = EMPTY_MESSAGE_ARRAY;
            final ArrayList<Message> remoteMessages = new ArrayList<Message>();
            HashMap<String, Message> remoteUidMap = new HashMap<String, Message>();

            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "SYNC: Remote message count for folder " + folder + " is " + remoteMessageCount);
            final Date earliestDate = account.getEarliestPollDate();


            if (remoteMessageCount > 0) {
                /* Message numbers start at 1.  */
                int remoteStart;
                if (visibleLimit > 0) {
                    remoteStart = Math.max(0, remoteMessageCount - visibleLimit) + 1;
                } else {
                    remoteStart = 1;
                }
                int remoteEnd = remoteMessageCount;

                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "SYNC: About to get messages " + remoteStart + " through " + remoteEnd + " for folder " + folder);

                final AtomicInteger headerProgress = new AtomicInteger(0);
                for (MessagingListener l : getListeners(listener)) {
                    l.synchronizeMailboxHeadersStarted(account, folder);
                }


                remoteMessageArray = remoteFolder.getMessages(remoteStart, remoteEnd, earliestDate, null);

                int messageCount = remoteMessageArray.length;

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

                remoteMessageArray = null;
                for (MessagingListener l : getListeners(listener)) {
                    l.synchronizeMailboxHeadersFinished(account, folder, headerProgress.get(), remoteUidMap.size());
                }

            } else if (remoteMessageCount < 0) {
                throw new Exception("Message count " + remoteMessageCount + " for folder " + folder);
            }

            /*
             * Remove any messages that are in the local store but no longer on the remote store or are too old
             */
            if (account.syncRemoteDeletions()) {
                ArrayList<Message> destroyMessages = new ArrayList<Message>();
                for (Message localMessage : localMessages) {
                    if (remoteUidMap.get(localMessage.getUid()) == null) {
                        destroyMessages.add(localMessage);
                    }
                }


                localFolder.destroyMessages(destroyMessages.toArray(EMPTY_MESSAGE_ARRAY));

                for (Message destroyMessage : destroyMessages) {
                    for (MessagingListener l : getListeners(listener)) {
                        l.synchronizeMailboxRemovedMessage(account, folder, destroyMessage);
                    }
                }
            }
            localMessages = null;

            /*
             * Now we download the actual content of messages.
             */
            int newMessages = downloadMessages(account, remoteFolder, localFolder, remoteMessages, false);

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
            notifyUserIfCertificateProblem(mApplication, e, account, true);
            addErrorMessage(account, null, e);
            Log.e(K9.LOG_TAG, "Failed synchronizing folder " + account.getDescription() + ":" + folder + " @ " + new Date());

        } finally {
            if (providedRemoteFolder == null) {
                closeFolder(remoteFolder);
            }

            closeFolder(tLocalFolder);
        }

    }


    private void closeFolder(Folder f) {
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
    private boolean verifyOrCreateRemoteSpecialFolder(final Account account, final String folder, final Folder remoteFolder, final MessagingListener listener) throws MessagingException {
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
     *
     * @return The number of downloaded messages that are not flagged as {@link Flag#SEEN}.
     *
     * @throws MessagingException
     */
    private int downloadMessages(final Account account, final Folder remoteFolder,
                                 final LocalFolder localFolder, List<Message> inputMessages,
                                 boolean flagSyncOnly) throws MessagingException {

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
            AccountStats stats = account.getStats(mApplication);
            unreadBeforeStart = stats.unreadMessageCount;

        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to getUnreadMessageCount for account: " + account, e);
        }

        ArrayList<Message> syncFlagMessages = new ArrayList<Message>();
        List<Message> unsyncedMessages = new ArrayList<Message>();
        final AtomicInteger newMessages = new AtomicInteger(0);

        List<Message> messages = new ArrayList<Message>(inputMessages);

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
        final ArrayList<Message> largeMessages = new ArrayList<Message>();
        final ArrayList<Message> smallMessages = new ArrayList<Message>();
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


            fetchUnsyncedMessages(account, remoteFolder, localFolder, unsyncedMessages, smallMessages, largeMessages, progress, todo, fp);

            // If a message didn't exist, messageFinished won't be called, but we shouldn't try again
            // If we got here, nothing failed
            for (Message message : unsyncedMessages) {
                String newPushState = remoteFolder.getNewPushState(localFolder.getPushState(), message);
                if (newPushState != null) {
                    localFolder.setPushState(newPushState);
                }
            }
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
        fp.add(FetchProfile.Item.BODY);
        //        fp.add(FetchProfile.Item.FLAGS);
        //        fp.add(FetchProfile.Item.ENVELOPE);

        downloadSmallMessages(account, remoteFolder, localFolder, smallMessages, progress, unreadBeforeStart, newMessages, todo, fp);
        smallMessages.clear();

        /*
         * Now do the large messages that require more round trips.
         */
        fp.clear();
        fp.add(FetchProfile.Item.STRUCTURE);
        downloadLargeMessages(account, remoteFolder, localFolder, largeMessages, progress, unreadBeforeStart,  newMessages, todo, fp);
        largeMessages.clear();

        /*
         * Refresh the flags for any messages in the local store that we didn't just
         * download.
         */

        refreshLocalMessageFlags(account, remoteFolder, localFolder, syncFlagMessages, progress, todo);

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Synced remote messages for folder " + folder + ", " + newMessages.get() + " new messages");

        localFolder.purgeToVisibleLimit(new MessageRemovalListener() {
            @Override
            public void messageRemoved(Message message) {
                for (MessagingListener l : getListeners()) {
                    l.synchronizeMailboxRemovedMessage(account, folder, message);
                }
            }

        });

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
                account.save(Preferences.getPreferences(mApplication.getApplicationContext()));
            }

        }
        return newMessages.get();
    }
    private void evaluateMessageForDownload(final Message message, final String folder,
                                            final LocalFolder localFolder,
                                            final Folder remoteFolder,
                                            final Account account,
                                            final List<Message> unsyncedMessages,
                                            final ArrayList<Message> syncFlagMessages,
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
                    localFolder.appendMessages(new Message[] { message });

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

    private void fetchUnsyncedMessages(final Account account, final Folder remoteFolder,
                                       final LocalFolder localFolder,
                                       List<Message> unsyncedMessages,
                                       final ArrayList<Message> smallMessages,
                                       final ArrayList<Message> largeMessages,
                                       final AtomicInteger progress,
                                       final int todo,
                                       FetchProfile fp) throws MessagingException {
        final String folder = remoteFolder.getName();

        final Date earliestDate = account.getEarliestPollDate();

        /*
         * Messages to be batch written
         */
        final List<Message> chunk = new ArrayList<Message>(UNSYNC_CHUNK_SIZE);

        remoteFolder.fetch(unsyncedMessages.toArray(EMPTY_MESSAGE_ARRAY), fp,
        new MessageRetrievalListener() {
            @Override
            public void messageFinished(Message message, int number, int ofTotal) {
                try {
                    String newPushState = remoteFolder.getNewPushState(localFolder.getPushState(), message);
                    if (newPushState != null) {
                        localFolder.setPushState(newPushState);
                    }
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

                    // And include it in the view
                    if (message.getSubject() != null && message.getFrom() != null) {
                        /*
                         * We check to make sure that we got something worth
                         * showing (subject and from) because some protocols
                         * (POP) may not be able to give us headers for
                         * ENVELOPE, only size.
                         */

                        // keep message for delayed storing
                        chunk.add(message);

                        if (chunk.size() >= UNSYNC_CHUNK_SIZE) {
                            writeUnsyncedMessages(chunk, localFolder, account, folder);
                            chunk.clear();
                        }
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
        if (!chunk.isEmpty()) {
            writeUnsyncedMessages(chunk, localFolder, account, folder);
            chunk.clear();
        }
    }

    /**
     * Actual storing of messages
     *
     * <br>
     * FIXME: <strong>This method should really be moved in the above MessageRetrievalListener once {@link MessageRetrievalListener#messagesFinished(int)} is properly invoked by various stores</strong>
     *
     * @param messages Never <code>null</code>.
     * @param localFolder
     * @param account
     * @param folder
     */
    private void writeUnsyncedMessages(final List<Message> messages, final LocalFolder localFolder, final Account account, final String folder) {
        if (K9.DEBUG) {
            Log.v(K9.LOG_TAG, "Batch writing " + Integer.toString(messages.size()) + " messages");
        }
        try {
            // Store the new message locally
            localFolder.appendMessages(messages.toArray(new Message[messages.size()]));

            for (final Message message : messages) {
                final Message localMessage = localFolder.getMessage(message.getUid());
                syncFlags(localMessage, message);
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "About to notify listeners that we got a new unsynced message "
                          + account + ":" + folder + ":" + message.getUid());
                for (final MessagingListener l : getListeners()) {
                    l.synchronizeMailboxAddOrUpdateMessage(account, folder, localMessage);
                }
            }
        } catch (final Exception e) {
            Log.e(K9.LOG_TAG, "Error while storing downloaded message.", e);
            addErrorMessage(account, null, e);
        }
    }


    private boolean shouldImportMessage(final Account account, final String folder, final Message message, final AtomicInteger progress, final Date earliestDate) {

        if (account.isSearchByDateCapable() && message.olderThan(earliestDate)) {
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "Message " + message.getUid() + " is older than "
                      + earliestDate + ", hence not saving");
            }
            return false;
        }
        return true;
    }

    private void downloadSmallMessages(final Account account, final Folder remoteFolder,
                                       final LocalFolder localFolder,
                                       ArrayList<Message> smallMessages,
                                       final AtomicInteger progress,
                                       final int unreadBeforeStart,
                                       final AtomicInteger newMessages,
                                       final int todo,
                                       FetchProfile fp) throws MessagingException {
        final String folder = remoteFolder.getName();

        final Date earliestDate = account.getEarliestPollDate();

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Fetching small messages for folder " + folder);

        remoteFolder.fetch(smallMessages.toArray(new Message[smallMessages.size()]),
        fp, new MessageRetrievalListener() {
            @Override
            public void messageFinished(final Message message, int number, int ofTotal) {
                try {

                    if (!shouldImportMessage(account, folder, message, progress, earliestDate)) {
                        progress.incrementAndGet();

                        return;
                    }

                    // Store the updated message locally
                    final Message localMessage = localFolder.storeSmallMessage(message, new Runnable() {
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
                        notifyAccount(mApplication, account, localMessage, unreadBeforeStart);
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



    private void downloadLargeMessages(final Account account, final Folder remoteFolder,
                                       final LocalFolder localFolder,
                                       ArrayList<Message> largeMessages,
                                       final AtomicInteger progress,
                                       final int unreadBeforeStart,
                                       final AtomicInteger newMessages,
                                       final int todo,
                                       FetchProfile fp) throws MessagingException {
        final String folder = remoteFolder.getName();

        final Date earliestDate = account.getEarliestPollDate();

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Fetching large messages for folder " + folder);

        remoteFolder.fetch(largeMessages.toArray(new Message[largeMessages.size()]), fp, null);
        for (Message message : largeMessages) {

            if (!shouldImportMessage(account, folder, message, progress, earliestDate)) {
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
                fp.clear();
                fp.add(FetchProfile.Item.BODY_SANE);
                /*
                 *  TODO a good optimization here would be to make sure that all Stores set
                 *  the proper size after this fetch and compare the before and after size. If
                 *  they equal we can mark this SYNCHRONIZED instead of PARTIALLY_SYNCHRONIZED
                 */

                remoteFolder.fetch(new Message[] { message }, fp, null);

                // Store the updated message locally
                localFolder.appendMessages(new Message[] { message });

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

                Set<Part> viewables = MimeUtility.collectTextParts(message);

                /*
                 * Now download the parts we're interested in storing.
                 */
                for (Part part : viewables) {
                    remoteFolder.fetchPart(message, part, null);
                }
                // Store the updated message locally
                localFolder.appendMessages(new Message[] { message });

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
            Message localMessage = localFolder.getMessage(message.getUid());

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
                notifyAccount(mApplication, account, localMessage, unreadBeforeStart);
            }

        }//for large messages
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Done fetching large messages for folder " + folder);

    }

    private void refreshLocalMessageFlags(final Account account, final Folder remoteFolder,
                                          final LocalFolder localFolder,
                                          ArrayList<Message> syncFlagMessages,
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

            List<Message> undeletedMessages = new LinkedList<Message>();
            for (Message message : syncFlagMessages) {
                if (!message.isSet(Flag.DELETED)) {
                    undeletedMessages.add(message);
                }
            }

            remoteFolder.fetch(undeletedMessages.toArray(EMPTY_MESSAGE_ARRAY), fp, null);
            for (Message remoteMessage : syncFlagMessages) {
                Message localMessage = localFolder.getMessage(remoteMessage.getUid());
                boolean messageChanged = syncFlags(localMessage, remoteMessage);
                if (messageChanged) {
                    boolean shouldBeNotifiedOf = false;
                    if (localMessage.isSet(Flag.DELETED) || isMessageSuppressed(account, localMessage)) {
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
                        NotificationData data = getNotificationData(account, null);
                        if (data != null) {
                            synchronized (data) {
                                MessageReference ref = localMessage.makeMessageReference();
                                if (data.removeMatchingMessage(mApplication, ref)) {
                                    notifyAccountWithDataLocked(mApplication, account, null, data);
                                }
                            }
                        }
                    }
                }
                progress.incrementAndGet();
                for (MessagingListener l : getListeners()) {
                    l.synchronizeMailboxProgress(account, folder, progress.get(), todo);
                }
            }
        }
    }

    private boolean syncFlags(Message localMessage, Message remoteMessage) throws MessagingException {
        boolean messageChanged = false;
        if (localMessage == null || localMessage.isSet(Flag.DELETED)) {
            return false;
        }
        if (remoteMessage.isSet(Flag.DELETED)) {
            if (localMessage.getFolder().getAccount().syncRemoteDeletions()) {
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
        Throwable nextCause = rootCause;
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
        ArrayList<PendingCommand> commands = localStore.getPendingCommands();

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
            notifyUserIfCertificateProblem(mApplication, me, account, true);
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
     *
     * @param command arguments = (String folder, String uid)
     * @param account
     * @throws MessagingException
     */
    private void processPendingAppend(PendingCommand command, Account account)
    throws MessagingException {
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
            remoteFolder.open(OpenMode.READ_WRITE);
            if (remoteFolder.getMode() != OpenMode.READ_WRITE) {
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
                localFolder.fetch(new Message[] { localMessage } , fp, null);
                String oldUid = localMessage.getUid();
                localMessage.setFlag(Flag.X_REMOTE_COPY_STARTED, true);
                remoteFolder.appendMessages(new Message[] { localMessage });

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
                remoteFolder.fetch(new Message[] { remoteMessage }, fp, null);
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
                    fp.clear();
                    fp = new FetchProfile();
                    fp.add(FetchProfile.Item.BODY);
                    localFolder.fetch(new Message[] { localMessage }, fp, null);
                    String oldUid = localMessage.getUid();

                    localMessage.setFlag(Flag.X_REMOTE_COPY_STARTED, true);

                    remoteFolder.appendMessages(new Message[] { localMessage });
                    localFolder.changeUid(localMessage);
                    for (MessagingListener l : getListeners()) {
                        l.messageUidChanged(account, folder, oldUid, localMessage.getUid());
                    }
                    if (remoteDate != null) {
                        remoteMessage.setFlag(Flag.DELETED, true);
                        if (Account.EXPUNGE_IMMEDIATELY.equals(account.getExpungePolicy())) {
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

    private void queueMoveOrCopy(Account account, String srcFolder, String destFolder, boolean isCopy, String uids[], Map<String, String> uidMap) {
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
            System.arraycopy(uidMap.keySet().toArray(), 0, command.arguments, 4, uidMap.keySet().size());
            System.arraycopy(uidMap.values().toArray(), 0, command.arguments, 4 + uidMap.keySet().size(), uidMap.values().size());
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
    private void processPendingMoveOrCopyOld2(PendingCommand command, Account account)
            throws MessagingException {
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
     *
     * @param command arguments = (String folder, String uid)
     * @param account
     * @throws MessagingException
     */
    private void processPendingMoveOrCopy(PendingCommand command, Account account)
    throws MessagingException {
        Folder remoteSrcFolder = null;
        Folder remoteDestFolder = null;
        LocalFolder localDestFolder = null;
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
            List<Message> messages = new ArrayList<Message>();

            /*
             * We split up the localUidMap into two parts while sending the command, here we assemble it back.
             */
            Map<String, String> localUidMap = new HashMap<String, String>();
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
            remoteSrcFolder.open(OpenMode.READ_WRITE);
            if (remoteSrcFolder.getMode() != OpenMode.READ_WRITE) {
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
                remoteSrcFolder.delete(messages.toArray(EMPTY_MESSAGE_ARRAY), destFolderName);
            } else {
                remoteDestFolder = remoteStore.getFolder(destFolder);

                if (isCopy) {
                    remoteUidMap = remoteSrcFolder.copyMessages(messages.toArray(EMPTY_MESSAGE_ARRAY), remoteDestFolder);
                } else {
                    remoteUidMap = remoteSrcFolder.moveMessages(messages.toArray(EMPTY_MESSAGE_ARRAY), remoteDestFolder);
                }
            }
            if (!isCopy && Account.EXPUNGE_IMMEDIATELY.equals(account.getExpungePolicy())) {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "processingPendingMoveOrCopy expunging folder " + account.getDescription() + ":" + srcFolder);

                remoteSrcFolder.expunge();
            }

            /*
             * This next part is used to bring the local UIDs of the local destination folder
             * upto speed with the remote UIDs of remote destionation folder.
             */
            if (!localUidMap.isEmpty() && remoteUidMap != null && !remoteUidMap.isEmpty()) {
                Set<Map.Entry<String, String>> remoteSrcEntries = remoteUidMap.entrySet();
                Iterator<Map.Entry<String, String>> remoteSrcEntriesIterator = remoteSrcEntries.iterator();

                while (remoteSrcEntriesIterator.hasNext()) {
                    Map.Entry<String, String> entry = remoteSrcEntriesIterator.next();
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

    private void queueSetFlag(final Account account, final String folderName, final String newState, final String flag, final String[] uids) {
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
     *
     * @param command arguments = (String folder, String uid, boolean read)
     * @param account
     */
    private void processPendingSetFlag(PendingCommand command, Account account)
    throws MessagingException {
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
            remoteFolder.open(OpenMode.READ_WRITE);
            if (remoteFolder.getMode() != OpenMode.READ_WRITE) {
                return;
            }
            List<Message> messages = new ArrayList<Message>();
            for (int i = 3; i < command.arguments.length; i++) {
                String uid = command.arguments[i];
                if (!uid.startsWith(K9.LOCAL_UID_PREFIX)) {
                    messages.add(remoteFolder.getMessage(uid));
                }
            }

            if (messages.isEmpty()) {
                return;
            }
            remoteFolder.setFlags(messages.toArray(EMPTY_MESSAGE_ARRAY), new Flag[] { flag }, newState);
        } finally {
            closeFolder(remoteFolder);
        }
    }

    // TODO: This method is obsolete and is only for transition from K-9 2.0 to K-9 2.1
    // Eventually, it should be removed
    private void processPendingSetFlagOld(PendingCommand command, Account account)
    throws MessagingException {
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
            remoteFolder.open(OpenMode.READ_WRITE);
            if (remoteFolder.getMode() != OpenMode.READ_WRITE) {
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
    private void processPendingExpunge(PendingCommand command, Account account)
    throws MessagingException {
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
            remoteFolder.open(OpenMode.READ_WRITE);
            if (remoteFolder.getMode() != OpenMode.READ_WRITE) {
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
    private void processPendingMoveOrCopyOld(PendingCommand command, Account account)
    throws MessagingException {
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
        remoteSrcFolder.open(OpenMode.READ_WRITE);
        if (remoteSrcFolder.getMode() != OpenMode.READ_WRITE) {
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

        remoteDestFolder.open(OpenMode.READ_WRITE);
        if (remoteDestFolder.getMode() != OpenMode.READ_WRITE) {
            throw new MessagingException("processPendingMoveOrCopyOld: could not open remoteDestFolder " + srcFolder + " read/write", true);
        }

        if (isCopy) {
            remoteSrcFolder.copyMessages(new Message[] { remoteMessage }, remoteDestFolder);
        } else {
            remoteSrcFolder.moveMessages(new Message[] { remoteMessage }, remoteDestFolder);
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
            localFolder.open(OpenMode.READ_WRITE);
            Message[] messages = localFolder.getMessages(null, false);
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
            remoteFolder.open(OpenMode.READ_WRITE);
            if (remoteFolder.getMode() != OpenMode.READ_WRITE) {
                return;
            }

            remoteFolder.setFlags(new Flag[] {Flag.SEEN}, true);
            remoteFolder.close();
        } catch (UnsupportedOperationException uoe) {
            Log.w(K9.LOG_TAG, "Could not mark all server-side as read because store doesn't support operation", uoe);
        } finally {
            closeFolder(localFolder);
            closeFolder(remoteFolder);
        }
    }

    private void notifyUserIfCertificateProblem(Context context, Exception e,
            Account account, boolean incoming) {
        if (!(e instanceof CertificateValidationException)) {
            return;
        }

        CertificateValidationException cve = (CertificateValidationException) e;
        if (!cve.needsUserAttention()) {
            return;
        }

        final int id = incoming
                ? K9.CERTIFICATE_EXCEPTION_NOTIFICATION_INCOMING + account.getAccountNumber()
                : K9.CERTIFICATE_EXCEPTION_NOTIFICATION_OUTGOING + account.getAccountNumber();
        final Intent i = incoming
                ? AccountSetupIncoming.intentActionEditIncomingSettings(context, account)
                : AccountSetupOutgoing.intentActionEditOutgoingSettings(context, account);
        final PendingIntent pi = PendingIntent.getActivity(context,
                account.getAccountNumber(), i, PendingIntent.FLAG_UPDATE_CURRENT);
        final String title = context.getString(
                R.string.notification_certificate_error_title, account.getName());

        final NotificationCompat.Builder builder = new NotificationBuilder(context);
        builder.setSmallIcon(R.drawable.ic_notify_new_mail);
        builder.setWhen(System.currentTimeMillis());
        builder.setAutoCancel(true);
        builder.setTicker(title);
        builder.setContentTitle(title);
        builder.setContentText(context.getString(R.string.notification_certificate_error_text));
        builder.setContentIntent(pi);

        configureNotification(builder, null, null,
                K9.NOTIFICATION_LED_FAILURE_COLOR,
                K9.NOTIFICATION_LED_BLINK_FAST, true);

        final NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(null, id, builder.build());
    }

    public void clearCertificateErrorNotifications(Context context,
            final Account account, boolean incoming, boolean outgoing) {
        final NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (incoming) {
            nm.cancel(null, K9.CERTIFICATE_EXCEPTION_NOTIFICATION_INCOMING + account.getAccountNumber());
        }
        if (outgoing) {
            nm.cancel(null, K9.CERTIFICATE_EXCEPTION_NOTIFICATION_OUTGOING + account.getAccountNumber());
        }
    }


    static long uidfill = 0;
    static AtomicBoolean loopCatch = new AtomicBoolean();
    public void addErrorMessage(Account account, String subject, Throwable t) {
        if (!loopCatch.compareAndSet(false, true)) {
            return;
        }
        try {
            if (t == null) {
                return;
            }

            CharArrayWriter baos = new CharArrayWriter(t.getStackTrace().length * 10);
            PrintWriter ps = new PrintWriter(baos);
            t.printStackTrace(ps);
            ps.close();

            if (subject == null) {
                subject = getRootCauseMessage(t);
            }

            addErrorMessage(account, subject, baos.toString());
        } catch (Throwable it) {
            Log.e(K9.LOG_TAG, "Could not save error message to " + account.getErrorFolderName(), it);
        } finally {
            loopCatch.set(false);
        }
    }

    public void addErrorMessage(Account account, String subject, String body) {
        if (!K9.ENABLE_ERROR_FOLDER) {
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
            Message[] messages = new Message[1];
            MimeMessage message = new MimeMessage();


            message.setBody(new TextBody(body));
            message.setFlag(Flag.X_DOWNLOADED_FULL, true);
            message.setSubject(subject);

            long nowTime = System.currentTimeMillis();
            Date nowDate = new Date(nowTime);
            message.setInternalDate(nowDate);
            message.addSentDate(nowDate);
            message.setFrom(new Address(account.getEmail(), "K9mail internal"));
            messages[0] = message;

            localFolder.appendMessages(messages);

            localFolder.clearMessagesOlderThan(nowTime - (15 * 60 * 1000));

        } catch (Throwable it) {
            Log.e(K9.LOG_TAG, "Could not save error message to " + account.getErrorFolderName(), it);
        } finally {
            loopCatch.set(false);
        }
    }



    public void markAllMessagesRead(final Account account, final String folder) {

        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Marking all messages in " + account.getDescription() + ":" + folder + " as read");
        List<String> args = new ArrayList<String>();
        args.add(folder);
        PendingCommand command = new PendingCommand();
        command.command = PENDING_COMMAND_MARK_ALL_AS_READ;
        command.arguments = args.toArray(EMPTY_STRING_ARRAY);
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
            String[] uids = entry.getValue().toArray(EMPTY_STRING_ARRAY);
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
    public void setFlag(Account account, String folderName, Message[] messages, Flag flag,
            boolean newState) {
        // TODO: Put this into the background, but right now some callers depend on the message
        //       objects being modified right after this method returns.
        Folder localFolder = null;
        try {
            Store localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folderName);
            localFolder.open(OpenMode.READ_WRITE);

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
            localFolder.setFlags(messages, new Flag[] {flag}, newState);

            for (MessagingListener l : getListeners()) {
                l.folderStatusChanged(account, folderName, localFolder.getUnreadMessageCount());
            }


            /*
             * Handle the remote side
             */

            // The error folder is always a local folder
            // TODO: Skip the remote part for all local-only folders
            if (account.getErrorFolderName().equals(folderName)) {
                return;
            }

            String[] uids = new String[messages.length];
            for (int i = 0, end = uids.length; i < end; i++) {
                uids[i] = messages[i].getUid();
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
            localFolder.open(OpenMode.READ_WRITE);

            Message message = localFolder.getMessage(uid);
            if (message != null) {
                setFlag(account, folderName, new Message[] { message }, flag, newState);
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

    public void loadMessageForViewRemote(final Account account, final String folder,
                                         final String uid, final MessagingListener listener) {
        put("loadMessageForViewRemote", listener, new Runnable() {
            @Override
            public void run() {
                loadMessageForViewRemoteSynchronous(account, folder, uid, listener, false, false);
            }
        });
    }

    public boolean loadMessageForViewRemoteSynchronous(final Account account, final String folder,
            final String uid, final MessagingListener listener, final boolean force,
            final boolean loadPartialFromSearch) {
        Folder remoteFolder = null;
        LocalFolder localFolder = null;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folder);
            localFolder.open(OpenMode.READ_WRITE);

            Message message = localFolder.getMessage(uid);

            if (uid.startsWith(K9.LOCAL_UID_PREFIX)) {
                Log.w(K9.LOG_TAG, "Message has local UID so cannot download fully.");
                // ASH move toast
                android.widget.Toast.makeText(mApplication,
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

            if (message.isSet(Flag.X_DOWNLOADED_FULL)) {
                /*
                 * If the message has been synchronized since we were called we'll
                 * just hand it back cause it's ready to go.
                 */
                FetchProfile fp = new FetchProfile();
                fp.add(FetchProfile.Item.ENVELOPE);
                fp.add(FetchProfile.Item.BODY);
                localFolder.fetch(new Message[] { message }, fp, null);
            } else {
                /*
                 * At this point the message is not available, so we need to download it
                 * fully if possible.
                 */

                Store remoteStore = account.getRemoteStore();
                remoteFolder = remoteStore.getFolder(folder);
                remoteFolder.open(OpenMode.READ_WRITE);

                // Get the remote message and fully download it
                Message remoteMessage = remoteFolder.getMessage(uid);
                FetchProfile fp = new FetchProfile();
                fp.add(FetchProfile.Item.BODY);

                remoteFolder.fetch(new Message[] { remoteMessage }, fp, null);

                // Store the message locally and load the stored message into memory
                localFolder.appendMessages(new Message[] { remoteMessage });
                if (loadPartialFromSearch) {
                    fp.add(FetchProfile.Item.BODY);
                }
                fp.add(FetchProfile.Item.ENVELOPE);
                message = localFolder.getMessage(uid);
                localFolder.fetch(new Message[] { message }, fp, null);

                // Mark that this message is now fully synched
                if (account.isMarkMessageAsReadOnView()) {
                    message.setFlag(Flag.SEEN, true);
                }
                message.setFlag(Flag.X_DOWNLOADED_FULL, true);
            }

            // now that we have the full message, refresh the headers
            for (MessagingListener l : getListeners(listener)) {
                l.loadMessageForViewHeadersAvailable(account, folder, uid, message);
            }

            for (MessagingListener l : getListeners(listener)) {
                l.loadMessageForViewBodyAvailable(account, folder, uid, message);
            }
            for (MessagingListener l : getListeners(listener)) {
                l.loadMessageForViewFinished(account, folder, uid, message);
            }
            return true;
        } catch (Exception e) {
            for (MessagingListener l : getListeners(listener)) {
                l.loadMessageForViewFailed(account, folder, uid, e);
            }
            notifyUserIfCertificateProblem(mApplication, e, account, true);
            addErrorMessage(account, null, e);
            return false;
        } finally {
            closeFolder(remoteFolder);
            closeFolder(localFolder);
        }
    }

    public void loadMessageForView(final Account account, final String folder, final String uid,
                                   final MessagingListener listener) {
        for (MessagingListener l : getListeners(listener)) {
            l.loadMessageForViewStarted(account, folder, uid);
        }
        threadPool.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    LocalStore localStore = account.getLocalStore();
                    LocalFolder localFolder = localStore.getFolder(folder);
                    localFolder.open(OpenMode.READ_WRITE);

                    LocalMessage message = localFolder.getMessage(uid);
                    if (message == null
                    || message.getId() == 0) {
                        throw new IllegalArgumentException("Message not found: folder=" + folder + ", uid=" + uid);
                    }
                    // IMAP search results will usually need to be downloaded before viewing.
                    // TODO: limit by account.getMaximumAutoDownloadMessageSize().
                    if (!message.isSet(Flag.X_DOWNLOADED_FULL) &&
                            !message.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                        if (loadMessageForViewRemoteSynchronous(account, folder, uid, listener,
                                false, true)) {

                            markMessageAsReadOnView(account, message);
                        }
                        return;
                    }

                    markMessageAsReadOnView(account, message);

                    for (MessagingListener l : getListeners(listener)) {
                        l.loadMessageForViewHeadersAvailable(account, folder, uid, message);
                    }

                    FetchProfile fp = new FetchProfile();
                    fp.add(FetchProfile.Item.ENVELOPE);
                    fp.add(FetchProfile.Item.BODY);
                    localFolder.fetch(new Message[] {
                                          message
                                      }, fp, null);
                    localFolder.close();

                    for (MessagingListener l : getListeners(listener)) {
                        l.loadMessageForViewBodyAvailable(account, folder, uid, message);
                    }

                    for (MessagingListener l : getListeners(listener)) {
                        l.loadMessageForViewFinished(account, folder, uid, message);
                    }

                } catch (Exception e) {
                    for (MessagingListener l : getListeners(listener)) {
                        l.loadMessageForViewFailed(account, folder, uid, e);
                    }
                    addErrorMessage(account, null, e);

                }
            }
        });
    }

    /**
     * Mark the provided message as read if not disabled by the account setting.
     *
     * @param account
     *         The account the message belongs to.
     * @param message
     *         The message to mark as read. This {@link Message} instance will be modify by calling
     *         {@link Message#setFlag(Flag, boolean)} on it.
     *
     * @throws MessagingException
     *
     * @see Account#isMarkMessageAsReadOnView()
     */
    private void markMessageAsReadOnView(Account account, Message message)
            throws MessagingException {

        if (account.isMarkMessageAsReadOnView() && !message.isSet(Flag.SEEN)) {
            List<Long> messageIds = Collections.singletonList(message.getId());
            setFlagInCache(account, messageIds, Flag.SEEN, true);
            setFlagSynchronous(account, messageIds, Flag.SEEN, true, false);

            ((LocalMessage) message).setFlagInternal(Flag.SEEN, true);
        }
    }

    /**
     * Attempts to load the attachment specified by part from the given account and message.
     * @param account
     * @param message
     * @param part
     * @param listener
     */
    public void loadAttachment(
        final Account account,
        final Message message,
        final Part part,
        final Object tag,
        final MessagingListener listener) {
        /*
         * Check if the attachment has already been downloaded. If it has there's no reason to
         * download it, so we just tell the listener that it's ready to go.
         */

        if (part.getBody() != null) {
            for (MessagingListener l : getListeners(listener)) {
                l.loadAttachmentStarted(account, message, part, tag, false);
            }

            for (MessagingListener l : getListeners(listener)) {
                l.loadAttachmentFinished(account, message, part, tag);
            }
            return;
        }



        for (MessagingListener l : getListeners(listener)) {
            l.loadAttachmentStarted(account, message, part, tag, true);
        }

        put("loadAttachment", listener, new Runnable() {
            @Override
            public void run() {
                Folder remoteFolder = null;
                LocalFolder localFolder = null;
                try {
                    LocalStore localStore = account.getLocalStore();

                    List<Part> attachments = MimeUtility.collectAttachments(message);
                    for (Part attachment : attachments) {
                        attachment.setBody(null);
                    }
                    Store remoteStore = account.getRemoteStore();
                    localFolder = localStore.getFolder(message.getFolder().getName());
                    remoteFolder = remoteStore.getFolder(message.getFolder().getName());
                    remoteFolder.open(OpenMode.READ_WRITE);

                    //FIXME: This is an ugly hack that won't be needed once the Message objects have been united.
                    Message remoteMessage = remoteFolder.getMessage(message.getUid());
                    remoteMessage.setBody(message.getBody());
                    remoteFolder.fetchPart(remoteMessage, part, null);

                    localFolder.updateMessage((LocalMessage)message);
                    for (MessagingListener l : getListeners(listener)) {
                        l.loadAttachmentFinished(account, message, part, tag);
                    }
                } catch (MessagingException me) {
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Exception loading attachment", me);

                    for (MessagingListener l : getListeners(listener)) {
                        l.loadAttachmentFailed(account, message, part, tag, me.getMessage());
                    }
                    notifyUserIfCertificateProblem(mApplication, me, account, true);
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
     * @param account
     * @param message
     * @param listener
     */
    public void sendMessage(final Account account,
                            final Message message,
                            MessagingListener listener) {
        try {
            LocalStore localStore = account.getLocalStore();
            LocalFolder localFolder = localStore.getFolder(account.getOutboxFolderName());
            localFolder.open(OpenMode.READ_WRITE);
            localFolder.appendMessages(new Message[] { message });
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
        final Preferences prefs = Preferences.getPreferences(mApplication.getApplicationContext());
        for (Account account : prefs.getAvailableAccounts()) {
            sendPendingMessages(account, listener);
        }
    }


    /**
     * Attempt to send any messages that are sitting in the Outbox.
     * @param account
     * @param listener
     */
    public void sendPendingMessages(final Account account,
                                    MessagingListener listener) {
        putBackground("sendPendingMessages", listener, new Runnable() {
            @Override
            public void run() {
                if (!account.isAvailable(mApplication)) {
                    throw new UnavailableAccountException();
                }
                if (messagesPendingSend(account)) {


                    notifyWhileSending(account);

                    try {
                        sendPendingMessagesSynchronous(account);
                    } finally {
                        notifyWhileSendingDone(account);
                    }
                }
            }
        });
    }

    private void cancelNotification(int id) {
        NotificationManager notifMgr =
            (NotificationManager) mApplication.getSystemService(Context.NOTIFICATION_SERVICE);

        notifMgr.cancel(id);
    }

    private void notifyWhileSendingDone(Account account) {
        if (account.isShowOngoing()) {
            cancelNotification(K9.FETCHING_EMAIL_NOTIFICATION - account.getAccountNumber());
        }
    }

    /**
     * Display an ongoing notification while a message is being sent.
     *
     * @param account
     *         The account the message is sent from. Never {@code null}.
     */
    private void notifyWhileSending(Account account) {
        if (!account.isShowOngoing()) {
            return;
        }

        NotificationManager notifMgr =
            (NotificationManager) mApplication.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationBuilder(mApplication);
        builder.setSmallIcon(R.drawable.ic_notify_check_mail);
        builder.setWhen(System.currentTimeMillis());
        builder.setOngoing(true);
        builder.setTicker(mApplication.getString(R.string.notification_bg_send_ticker,
                account.getDescription()));

        builder.setContentTitle(mApplication.getString(R.string.notification_bg_send_title));
        builder.setContentText(account.getDescription());

        TaskStackBuilder stack = buildMessageListBackStack(mApplication, account,
                account.getInboxFolderName());
        builder.setContentIntent(stack.getPendingIntent(0, 0));

        if (K9.NOTIFICATION_LED_WHILE_SYNCING) {
            configureNotification(builder, null, null,
                    account.getNotificationSetting().getLedColor(),
                    K9.NOTIFICATION_LED_BLINK_FAST, true);
        }

        notifMgr.notify(K9.FETCHING_EMAIL_NOTIFICATION - account.getAccountNumber(),
                builder.build());
    }

    private void notifySendTempFailed(Account account, Exception lastFailure) {
        notifySendFailed(account, lastFailure, account.getOutboxFolderName());
    }

    private void notifySendPermFailed(Account account, Exception lastFailure) {
        notifySendFailed(account, lastFailure, account.getDraftsFolderName());
    }

    /**
     * Display a notification when sending a message has failed.
     *
     * @param account
     *         The account that was used to sent the message.
     * @param lastFailure
     *         The {@link Exception} instance that indicated sending the message has failed.
     * @param openFolder
     *         The name of the folder to open when the notification is clicked.
     */
    private void notifySendFailed(Account account, Exception lastFailure, String openFolder) {
        NotificationManager notifMgr =
                (NotificationManager) mApplication.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationBuilder(mApplication);
        builder.setSmallIcon(R.drawable.ic_notify_new_mail);
        builder.setWhen(System.currentTimeMillis());
        builder.setAutoCancel(true);
        builder.setTicker(mApplication.getString(R.string.send_failure_subject));
        builder.setContentTitle(mApplication.getString(R.string.send_failure_subject));
        builder.setContentText(getRootCauseMessage(lastFailure));

        TaskStackBuilder stack = buildFolderListBackStack(mApplication, account);
        builder.setContentIntent(stack.getPendingIntent(0, 0));

        configureNotification(builder,  null, null, K9.NOTIFICATION_LED_FAILURE_COLOR,
                K9.NOTIFICATION_LED_BLINK_FAST, true);

        notifMgr.notify(K9.SEND_FAILED_NOTIFICATION - account.getAccountNumber(),
                builder.build());
    }

    /**
     * Display an ongoing notification while checking for new messages on the server.
     *
     * @param account
     *         The account that is checked for new messages. Never {@code null}.
     * @param folder
     *         The folder that is being checked for new messages. Never {@code null}.
     */
    private void notifyFetchingMail(final Account account, final Folder folder) {
        if (!account.isShowOngoing()) {
            return;
        }

        final NotificationManager notifMgr =
                (NotificationManager) mApplication.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationBuilder(mApplication);
        builder.setSmallIcon(R.drawable.ic_notify_check_mail);
        builder.setWhen(System.currentTimeMillis());
        builder.setOngoing(true);
        builder.setTicker(mApplication.getString(
                R.string.notification_bg_sync_ticker, account.getDescription(), folder.getName()));
        builder.setContentTitle(mApplication.getString(R.string.notification_bg_sync_title));
        builder.setContentText(account.getDescription() +
                mApplication.getString(R.string.notification_bg_title_separator) +
                folder.getName());

        TaskStackBuilder stack = buildMessageListBackStack(mApplication, account,
                account.getInboxFolderName());
        builder.setContentIntent(stack.getPendingIntent(0, 0));

        if (K9.NOTIFICATION_LED_WHILE_SYNCING) {
            configureNotification(builder,  null, null,
                    account.getNotificationSetting().getLedColor(),
                    K9.NOTIFICATION_LED_BLINK_FAST, true);
        }

        notifMgr.notify(K9.FETCHING_EMAIL_NOTIFICATION - account.getAccountNumber(),
                builder.build());
    }

    private void notifyFetchingMailCancel(final Account account) {
        if (account.isShowOngoing()) {
            cancelNotification(K9.FETCHING_EMAIL_NOTIFICATION - account.getAccountNumber());
        }
    }

    public boolean messagesPendingSend(final Account account) {
        Folder localFolder = null;
        try {
            localFolder = account.getLocalStore().getFolder(
                              account.getOutboxFolderName());
            if (!localFolder.exists()) {
                return false;
            }

            localFolder.open(OpenMode.READ_WRITE);

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
     * @param account
     */
    public void sendPendingMessagesSynchronous(final Account account) {
        Folder localFolder = null;
        Exception lastFailure = null;
        try {
            Store localStore = account.getLocalStore();
            localFolder = localStore.getFolder(
                              account.getOutboxFolderName());
            if (!localFolder.exists()) {
                return;
            }
            for (MessagingListener l : getListeners()) {
                l.sendPendingMessagesStarted(account);
            }
            localFolder.open(OpenMode.READ_WRITE);

            Message[] localMessages = localFolder.getMessages(null);
            int progress = 0;
            int todo = localMessages.length;
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
                Log.i(K9.LOG_TAG, "Scanning folder '" + account.getOutboxFolderName() + "' (" + ((LocalFolder)localFolder).getId() + ") for messages to send");

            Transport transport = Transport.getInstance(account);
            for (Message message : localMessages) {
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
                        notifySendTempFailed(account, new MessagingException(message.getSubject()));
                        continue;
                    }



                    localFolder.fetch(new Message[] { message }, fp, null);
                    try {


                        if (message.getHeader(K9.IDENTITY_HEADER) != null) {
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
                            LocalFolder localSentFolder = (LocalFolder) localStore.getFolder(account.getSentFolderName());
                            if (K9.DEBUG)
                                Log.i(K9.LOG_TAG, "Moving sent message to folder '" + account.getSentFolderName() + "' (" + localSentFolder.getId() + ") ");

                            localFolder.moveMessages(new Message[] { message }, localSentFolder);

                            if (K9.DEBUG)
                                Log.i(K9.LOG_TAG, "Moved sent message to folder '" + account.getSentFolderName() + "' (" + localSentFolder.getId() + ") ");

                            PendingCommand command = new PendingCommand();
                            command.command = PENDING_COMMAND_APPEND;
                            command.arguments = new String[] { localSentFolder.getName(), message.getUid() };
                            queuePendingCommand(account, command);
                            processPendingCommands(account);
                        }

                    } catch (Exception e) {
                        // 5.x.x errors from the SMTP server are "PERMFAIL"
                        // move the message over to drafts rather than leaving it in the outbox
                        // This is a complete hack, but is worlds better than the previous
                        // "don't even bother" functionality
                        if (getRootCauseMessage(e).startsWith("5")) {
                            localFolder.moveMessages(new Message[] { message }, (LocalFolder) localStore.getFolder(account.getDraftsFolderName()));
                        }

                        notifyUserIfCertificateProblem(mApplication, e, account, false);
                        message.setFlag(Flag.X_SEND_FAILED, true);
                        Log.e(K9.LOG_TAG, "Failed to send message", e);
                        for (MessagingListener l : getListeners()) {
                            l.synchronizeMailboxFailed(account, localFolder.getName(), getRootCauseMessage(e));
                        }
                        lastFailure = e;
                    }
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Failed to fetch message for sending", e);
                    for (MessagingListener l : getListeners()) {
                        l.synchronizeMailboxFailed(account, localFolder.getName(), getRootCauseMessage(e));
                    }
                    lastFailure = e;
                }
            }
            for (MessagingListener l : getListeners()) {
                l.sendPendingMessagesCompleted(account);
            }
            if (lastFailure != null) {
                if (getRootCauseMessage(lastFailure).startsWith("5")) {
                    notifySendPermFailed(account, lastFailure);
                } else {
                    notifySendTempFailed(account, lastFailure);
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
                cancelNotification(K9.SEND_FAILED_NOTIFICATION - account.getAccountNumber());
            }
            closeFolder(localFolder);
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

        Preferences preferences = Preferences.getPreferences(mApplication);
        LocalSearch search = searchAccount.getRelatedSearch();

        // Collect accounts that belong to the search
        String[] accountUuids = search.getAccountUuids();
        Account[] accounts;
        if (search.searchAllAccounts()) {
            accounts = preferences.getAccounts();
        } else {
            accounts = new Account[accountUuids.length];
            for (int i = 0, len = accountUuids.length; i < len; i++) {
                String accountUuid = accountUuids[i];
                accounts[i] = preferences.getAccount(accountUuid);
            }
        }

        ContentResolver cr = mApplication.getContentResolver();

        int unreadMessageCount = 0;
        int flaggedMessageCount = 0;

        String[] projection = {
                StatsColumns.UNREAD_COUNT,
                StatsColumns.FLAGGED_COUNT
        };

        for (Account account : accounts) {
            StringBuilder query = new StringBuilder();
            List<String> queryArgs = new ArrayList<String>();
            ConditionsTreeNode conditions = search.getConditions();
            SqlQueryBuilder.buildWhereClause(account, conditions, query, queryArgs);

            String selection = query.toString();
            String[] selectionArgs = queryArgs.toArray(EMPTY_STRING_ARRAY);

            Uri uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI,
                    "account/" + account.getUuid() + "/stats");

            // Query content provider to get the account stats
            Cursor cursor = cr.query(uri, projection, selection, selectionArgs, null);
            try {
                if (cursor.moveToFirst()) {
                    unreadMessageCount += cursor.getInt(0);
                    flaggedMessageCount += cursor.getInt(1);
                }
            } finally {
                cursor.close();
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



    public boolean isMoveCapable(Message message) {
        return !message.getUid().startsWith(K9.LOCAL_UID_PREFIX);
    }
    public boolean isCopyCapable(Message message) {
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
    public void moveMessages(final Account account, final String srcFolder,
            final List<Message> messages, final String destFolder,
            final MessagingListener listener) {

        suppressMessages(account, messages);

        putBackground("moveMessages", null, new Runnable() {
            @Override
            public void run() {
                moveOrCopyMessageSynchronous(account, srcFolder, messages, destFolder, false,
                        listener);
            }
        });
    }

    public void moveMessagesInThread(final Account account, final String srcFolder,
            final List<Message> messages, final String destFolder) {

        suppressMessages(account, messages);

        putBackground("moveMessagesInThread", null, new Runnable() {
            @Override
            public void run() {
                try {
                    List<Message> messagesInThreads = collectMessagesInThreads(account, messages);
                    moveOrCopyMessageSynchronous(account, srcFolder, messagesInThreads, destFolder,
                            false, null);
                } catch (MessagingException e) {
                    addErrorMessage(account, "Exception while moving messages", e);
                }
            }
        });
    }

    public void moveMessage(final Account account, final String srcFolder, final Message message,
            final String destFolder, final MessagingListener listener) {

        moveMessages(account, srcFolder, Collections.singletonList(message), destFolder, listener);
    }

    public void copyMessages(final Account account, final String srcFolder,
            final List<Message> messages, final String destFolder,
            final MessagingListener listener) {

        putBackground("copyMessages", null, new Runnable() {
            @Override
            public void run() {
                moveOrCopyMessageSynchronous(account, srcFolder, messages, destFolder, true,
                        listener);
            }
        });
    }

    public void copyMessagesInThread(final Account account, final String srcFolder,
            final List<Message> messages, final String destFolder) {

        putBackground("copyMessagesInThread", null, new Runnable() {
            @Override
            public void run() {
                try {
                    List<Message> messagesInThreads = collectMessagesInThreads(account, messages);
                    moveOrCopyMessageSynchronous(account, srcFolder, messagesInThreads, destFolder,
                            true, null);
                } catch (MessagingException e) {
                    addErrorMessage(account, "Exception while copying messages", e);
                }
            }
        });
    }

    public void copyMessage(final Account account, final String srcFolder, final Message message,
            final String destFolder, final MessagingListener listener) {

        copyMessages(account, srcFolder, Collections.singletonList(message), destFolder, listener);
    }

    private void moveOrCopyMessageSynchronous(final Account account, final String srcFolder,
            final List<Message> inMessages, final String destFolder, final boolean isCopy,
            MessagingListener listener) {

        try {
            Map<String, String> uidMap = new HashMap<String, String>();
            Store localStore = account.getLocalStore();
            Store remoteStore = account.getRemoteStore();
            if (!isCopy && (!remoteStore.isMoveCapable() || !localStore.isMoveCapable())) {
                return;
            }
            if (isCopy && (!remoteStore.isCopyCapable() || !localStore.isCopyCapable())) {
                return;
            }

            Folder localSrcFolder = localStore.getFolder(srcFolder);
            Folder localDestFolder = localStore.getFolder(destFolder);

            boolean unreadCountAffected = false;
            List<String> uids = new LinkedList<String>();
            for (Message message : inMessages) {
                String uid = message.getUid();
                if (!uid.startsWith(K9.LOCAL_UID_PREFIX)) {
                    uids.add(uid);
                }

                if (!unreadCountAffected && !message.isSet(Flag.SEEN)) {
                    unreadCountAffected = true;
                }
            }

            Message[] messages = localSrcFolder.getMessages(uids.toArray(EMPTY_STRING_ARRAY), null);
            if (messages.length > 0) {
                Map<String, Message> origUidMap = new HashMap<String, Message>();

                for (Message message : messages) {
                    origUidMap.put(message.getUid(), message);
                }

                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "moveOrCopyMessageSynchronous: source folder = " + srcFolder
                          + ", " + messages.length + " messages, " + ", destination folder = " + destFolder + ", isCopy = " + isCopy);

                if (isCopy) {
                    FetchProfile fp = new FetchProfile();
                    fp.add(FetchProfile.Item.ENVELOPE);
                    fp.add(FetchProfile.Item.BODY);
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
                    for (Map.Entry<String, Message> entry : origUidMap.entrySet()) {
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

                queueMoveOrCopy(account, srcFolder, destFolder, isCopy, origUidMap.keySet().toArray(EMPTY_STRING_ARRAY), uidMap);
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

    public void expunge(final Account account, final String folder, final MessagingListener listener) {
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
            localFolder.open(OpenMode.READ_WRITE);
            String uid = localFolder.getMessageUidById(id);
            if (uid != null) {
                Message message = localFolder.getMessage(uid);
                if (message != null) {
                    deleteMessages(Collections.singletonList(message), null);
                }
            }
        } catch (MessagingException me) {
            addErrorMessage(account, null, me);
        } finally {
            closeFolder(localFolder);
        }
    }

    public void deleteThreads(final List<Message> messages) {
        actOnMessages(messages, new MessageActor() {

            @Override
            public void act(final Account account, final Folder folder,
                    final List<Message> accountMessages) {

                suppressMessages(account, messages);

                putBackground("deleteThreads", null, new Runnable() {
                    @Override
                    public void run() {
                        deleteThreadsSynchronous(account, folder.getName(), accountMessages);
                    }
                });
            }
        });
    }

    public void deleteThreadsSynchronous(Account account, String folderName,
            List<Message> messages) {

        try {
            List<Message> messagesToDelete = collectMessagesInThreads(account, messages);

            deleteMessagesSynchronous(account, folderName,
                    messagesToDelete.toArray(EMPTY_MESSAGE_ARRAY), null);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Something went wrong while deleting threads", e);
        }
    }

    public List<Message> collectMessagesInThreads(Account account, List<Message> messages)
            throws MessagingException {

        LocalStore localStore = account.getLocalStore();

        List<Message> messagesInThreads = new ArrayList<Message>();
        for (Message message : messages) {
            LocalMessage localMessage = (LocalMessage) message;
            long rootId = localMessage.getRootId();
            long threadId = (rootId == -1) ? localMessage.getThreadId() : rootId;

            Message[] messagesInThread = localStore.getMessagesInThread(threadId);
            Collections.addAll(messagesInThreads, messagesInThread);
        }

        return messagesInThreads;
    }

    public void deleteMessages(final List<Message> messages, final MessagingListener listener) {
        actOnMessages(messages, new MessageActor() {

            @Override
            public void act(final Account account, final Folder folder,
            final List<Message> accountMessages) {
                suppressMessages(account, messages);

                putBackground("deleteMessages", null, new Runnable() {
                    @Override
                    public void run() {
                        deleteMessagesSynchronous(account, folder.getName(),
                                accountMessages.toArray(EMPTY_MESSAGE_ARRAY), listener);
                    }
                });
            }

        });

    }

    private void deleteMessagesSynchronous(final Account account, final String folder, final Message[] messages,
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

                localFolder.setFlags(messages, new Flag[] { Flag.DELETED }, true);
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
            } else if (account.getDeletePolicy() == Account.DELETE_POLICY_ON_DELETE) {
                if (folder.equals(account.getTrashFolderName())) {
                    queueSetFlag(account, folder, Boolean.toString(true), Flag.DELETED.toString(), uids);
                } else {
                    queueMoveOrCopy(account, folder, account.getTrashFolderName(), false, uids, uidMap);
                }
                processPendingCommands(account);
            } else if (account.getDeletePolicy() == Account.DELETE_POLICY_MARK_AS_READ) {
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

    private String[] getUidsFromMessages(Message[] messages) {
        String[] uids = new String[messages.length];
        for (int i = 0; i < messages.length; i++) {
            uids[i] = messages[i].getUid();
        }
        return uids;
    }

    private void processPendingEmptyTrash(PendingCommand command, Account account) throws MessagingException {
        Store remoteStore = account.getRemoteStore();

        Folder remoteFolder = remoteStore.getFolder(account.getTrashFolderName());
        try {
            if (remoteFolder.exists()) {
                remoteFolder.open(OpenMode.READ_WRITE);
                remoteFolder.setFlags(new Flag [] { Flag.DELETED }, true);
                if (Account.EXPUNGE_IMMEDIATELY.equals(account.getExpungePolicy())) {
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
                    localFolder.open(OpenMode.READ_WRITE);

                    boolean isTrashLocalOnly = isTrashLocalOnly(account);
                    if (isTrashLocalOnly) {
                        localFolder.clearAllMessages();
                    } else {
                        localFolder.setFlags(new Flag[] { Flag.DELETED }, true);
                    }

                    for (MessagingListener l : getListeners()) {
                        l.emptyTrashCompleted(account);
                    }

                    if (!isTrashLocalOnly) {
                        List<String> args = new ArrayList<String>();
                        PendingCommand command = new PendingCommand();
                        command.command = PENDING_COMMAND_EMPTY_TRASH;
                        command.arguments = args.toArray(EMPTY_STRING_ARRAY);
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

    public void sendAlternate(final Context context, Account account, Message message) {
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "About to load message " + account.getDescription() + ":" + message.getFolder().getName()
                  + ":" + message.getUid() + " for sendAlternate");

        loadMessageForView(account, message.getFolder().getName(),
        message.getUid(), new MessagingListener() {
            @Override
            public void loadMessageForViewBodyAvailable(Account account, String folder, String uid,
            Message message) {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Got message " + account.getDescription() + ":" + folder
                          + ":" + message.getUid() + " for sendAlternate");

                try {
                    Intent msg = new Intent(Intent.ACTION_SEND);
                    String quotedText = null;
                    Part part = MimeUtility.findFirstPartByMimeType(message,
                                "text/plain");
                    if (part == null) {
                        part = MimeUtility.findFirstPartByMimeType(message, "text/html");
                    }
                    if (part != null) {
                        quotedText = MimeUtility.getTextFromPart(part);
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
                } catch (MessagingException me) {
                    Log.e(K9.LOG_TAG, "Unable to send email through alternate program", me);
                }
            }
        });

    }

    /**
     * Checks mail for one or multiple accounts. If account is null all accounts
     * are checked.
     *
     * @param context
     * @param account
     * @param listener
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
                        accounts = new ArrayList<Account>(1);
                        accounts.add(account);
                    } else {
                        accounts = prefs.getAvailableAccounts();
                    }

                    for (final Account account : accounts) {
                        checkMailForAccount(context, account, ignoreLastCheckedTime, prefs, listener);
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
                                     final Preferences prefs,
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
                folder.open(Folder.OpenMode.READ_WRITE);
                folder.refresh(prefs);

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
                            notifyAccountCancel(context, account);
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
                    tLocalFolder.open(Folder.OpenMode.READ_WRITE);

                    if (!ignoreLastCheckedTime && tLocalFolder.getLastChecked() >
                    (System.currentTimeMillis() - accountInterval)) {
                        if (K9.DEBUG)
                            Log.v(K9.LOG_TAG, "Not running Command for folder " + folder.getName()
                                  + ", previously synced @ " + new Date(folder.getLastChecked())
                                  + " which would be too recent for the account period");
                        return;
                    }
                    notifyFetchingMail(account, folder);
                    try {
                        synchronizeMailboxSynchronous(account, folder.getName(), listener, null);
                    } finally {
                        notifyFetchingMailCancel(account);
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

        return true;
    }

    /**
     * Get the pending notification data for an account.
     * See {@link NotificationData}.
     *
     * @param account The account to retrieve the pending data for
     * @param previousUnreadMessageCount The number of currently pending messages, which will be used
     *                                    if there's no pending data yet. If passed as null, a new instance
     *                                    won't be created if currently not existent.
     * @return A pending data instance, or null if one doesn't exist and
     *          previousUnreadMessageCount was passed as null.
     */
    private NotificationData getNotificationData(Account account, Integer previousUnreadMessageCount) {
        NotificationData data;

        synchronized (notificationData) {
            data = notificationData.get(account.getAccountNumber());
            if (data == null && previousUnreadMessageCount != null) {
                data = new NotificationData(previousUnreadMessageCount);
                notificationData.put(account.getAccountNumber(), data);
            }
        }

        return data;
    }

    private CharSequence getMessageSender(Context context, Account account, Message message) {
        try {
            boolean isSelf = false;
            final Contacts contacts = K9.showContactName() ? Contacts.getInstance(context) : null;
            final Address[] fromAddrs = message.getFrom();

            if (fromAddrs != null) {
                isSelf = account.isAnIdentity(fromAddrs);
                if (!isSelf && fromAddrs.length > 0) {
                    return fromAddrs[0].toFriendly(contacts).toString();
                }
            }

            if (isSelf) {
                // show To: if the message was sent from me
                Address[] rcpts = message.getRecipients(Message.RecipientType.TO);

                if (rcpts != null && rcpts.length > 0) {
                    return context.getString(R.string.message_to_fmt,
                            rcpts[0].toFriendly(contacts).toString());
                }

                return context.getString(R.string.general_no_sender);
            }
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to get sender information for notification.", e);
        }

        return null;
    }

    private CharSequence getMessageSubject(Context context, Message message) {
        String subject = message.getSubject();
        if (!TextUtils.isEmpty(subject)) {
            return subject;
        }

        return context.getString(R.string.general_no_subject);
    }

    private static TextAppearanceSpan sEmphasizedSpan;
    private TextAppearanceSpan getEmphasizedSpan(Context context) {
        if (sEmphasizedSpan == null) {
            sEmphasizedSpan = new TextAppearanceSpan(context,
                    R.style.TextAppearance_StatusBar_EventContent_Emphasized);
        }
        return sEmphasizedSpan;
    }

    private CharSequence getMessagePreview(Context context, Message message) {
        CharSequence subject = getMessageSubject(context, message);
        String snippet = message.getPreview();

        if (TextUtils.isEmpty(subject)) {
            return snippet;
        } else if (TextUtils.isEmpty(snippet)) {
            return subject;
        }

        SpannableStringBuilder preview = new SpannableStringBuilder();
        preview.append(subject);
        preview.append('\n');
        preview.append(snippet);

        preview.setSpan(getEmphasizedSpan(context), 0, subject.length(), 0);

        return preview;
    }

    private CharSequence buildMessageSummary(Context context, CharSequence sender, CharSequence subject) {
        if (sender == null) {
            return subject;
        }

        SpannableStringBuilder summary = new SpannableStringBuilder();
        summary.append(sender);
        summary.append(" ");
        summary.append(subject);

        summary.setSpan(getEmphasizedSpan(context), 0, sender.length(), 0);

        return summary;
    }

    private static final boolean platformShowsNumberInNotification() {
        // Honeycomb and newer don't show the number as overlay on the notification icon.
        // However, the number will appear in the detailed notification view.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static final boolean platformSupportsExtendedNotifications() {
        // supported in Jellybean
        // TODO: use constant once target SDK is set to >= 16
        return Build.VERSION.SDK_INT >= 16;
    }

    private Message findNewestMessageForNotificationLocked(Context context,
            Account account, NotificationData data) {
        if (!data.messages.isEmpty()) {
            return data.messages.getFirst();
        }

        if (!data.droppedMessages.isEmpty()) {
            return data.droppedMessages.getFirst().restoreToLocalMessage(context);
        }

        return null;
    }

    /**
     * Creates a notification of a newly received message.
     */
    private void notifyAccount(Context context, Account account,
            Message message, int previousUnreadMessageCount) {
        final NotificationData data = getNotificationData(account, previousUnreadMessageCount);
        synchronized (data) {
            notifyAccountWithDataLocked(context, account, message, data);
        }
    }

    private void notifyAccountWithDataLocked(Context context, Account account,
            Message message, NotificationData data) {
        boolean updateSilently = false;

        if (message == null) {
            /* this can happen if a message we previously notified for is read or deleted remotely */
            message = findNewestMessageForNotificationLocked(context, account, data);
            updateSilently = true;
            if (message == null) {
                // seemingly both the message list as well as the overflow list is empty;
                // it probably is a good idea to cancel the notification in that case
                notifyAccountCancel(context, account);
                return;
            }
        } else {
            data.addMessage(message);
        }

        final KeyguardManager keyguardService = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        final CharSequence sender = getMessageSender(context, account, message);
        final CharSequence subject = getMessageSubject(context, message);
        CharSequence summary = buildMessageSummary(context, sender, subject);

        // If privacy mode active and keyguard active
        // OR
        // GlobalPreference is ALWAYS hide subject
        // OR
        // If we could not set a per-message notification, revert to a default message
        if ((K9.getNotificationHideSubject() == NotificationHideSubject.WHEN_LOCKED &&
                    keyguardService.inKeyguardRestrictedInputMode()) ||
                (K9.getNotificationHideSubject() == NotificationHideSubject.ALWAYS) ||
                summary.length() == 0) {
            summary = context.getString(R.string.notification_new_title);
        }

        NotificationManager notifMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationBuilder(context);
        builder.setSmallIcon(R.drawable.ic_notify_new_mail);
        builder.setWhen(System.currentTimeMillis());
        if (!updateSilently) {
            builder.setTicker(summary);
        }

        final int newMessages = data.getNewMessageCount();
        final int unreadCount = data.unreadBeforeNotification + newMessages;

        if (account.isNotificationShowsUnreadCount() || platformShowsNumberInNotification()) {
            builder.setNumber(unreadCount);
        }

        String accountDescr = (account.getDescription() != null) ?
                account.getDescription() : account.getEmail();
        final ArrayList<MessageReference> allRefs = data.getAllMessageRefs();

        if (platformSupportsExtendedNotifications()) {
            if (newMessages > 1) {
                // multiple messages pending, show inbox style
                NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle(builder);
                for (Message m : data.messages) {
                    style.addLine(buildMessageSummary(context,
                            getMessageSender(context, account, m),
                            getMessageSubject(context, m)));
                }
                if (!data.droppedMessages.isEmpty()) {
                    style.setSummaryText(context.getString(R.string.notification_additional_messages,
                            data.droppedMessages.size(), accountDescr));
                }
                String title = context.getString(R.string.notification_new_messages_title, newMessages);
                style.setBigContentTitle(title);
                builder.setContentTitle(title);
                builder.setSubText(accountDescr);
                builder.setStyle(style);
            } else {
                // single message pending, show big text
                NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle(builder);
                CharSequence preview = getMessagePreview(context, message);
                if (preview != null) {
                    style.bigText(preview);
                }
                builder.setContentText(subject);
                builder.setSubText(accountDescr);
                builder.setContentTitle(sender);
                builder.setStyle(style);

                builder.addAction(R.drawable.ic_action_single_message_options_dark,
                        context.getString(R.string.notification_action_reply),
                        NotificationActionService.getReplyIntent(context, account, message.makeMessageReference()));
            }

            builder.addAction(R.drawable.ic_action_mark_as_read_dark,
                    context.getString(R.string.notification_action_read),
                    NotificationActionService.getReadAllMessagesIntent(context, account, allRefs));

            NotificationQuickDelete deleteOption = K9.getNotificationQuickDeleteBehaviour();
            boolean showDeleteAction = deleteOption == NotificationQuickDelete.ALWAYS ||
                    (deleteOption == NotificationQuickDelete.FOR_SINGLE_MSG && newMessages == 1);

            if (showDeleteAction) {
                // we need to pass the action directly to the activity, otherwise the
                // status bar won't be pulled up and we won't see the confirmation (if used)
                builder.addAction(R.drawable.ic_action_delete_dark,
                        context.getString(R.string.notification_action_delete),
                        NotificationDeleteConfirmation.getIntent(context, account, allRefs));
            }
        } else {
            String accountNotice = context.getString(R.string.notification_new_one_account_fmt,
                    unreadCount, accountDescr);
            builder.setContentTitle(accountNotice);
            builder.setContentText(summary);
        }

        for (Message m : data.messages) {
            if (m.isSet(Flag.FLAGGED)) {
                builder.setPriority(NotificationCompat.PRIORITY_HIGH);
                break;
            }
        }

        TaskStackBuilder stack;
        boolean treatAsSingleMessageNotification;

        if (platformSupportsExtendedNotifications()) {
            // in the new-style notifications, we focus on the new messages, not the unread ones
            treatAsSingleMessageNotification = newMessages == 1;
        } else {
            // in the old-style notifications, we focus on unread messages, as we don't have a
            // good way to express the new message count
            treatAsSingleMessageNotification = unreadCount == 1;
        }

        if (treatAsSingleMessageNotification) {
            stack = buildMessageViewBackStack(context, message.makeMessageReference());
        } else if (account.goToUnreadMessageSearch()) {
            stack = buildUnreadBackStack(context, account);
        } else {
            String initialFolder = message.getFolder().getName();
            /* only go to folder if all messages are in the same folder, else go to folder list */
            for (MessageReference ref : allRefs) {
                if (!TextUtils.equals(initialFolder, ref.folderName)) {
                    initialFolder = null;
                    break;
                }
            }

            stack = buildMessageListBackStack(context, account, initialFolder);
        }

        builder.setContentIntent(stack.getPendingIntent(
                account.getAccountNumber(),
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT));
        builder.setDeleteIntent(NotificationActionService.getAcknowledgeIntent(context, account));

        // Only ring or vibrate if we have not done so already on this account and fetch
        boolean ringAndVibrate = false;
        if (!updateSilently && !account.isRingNotified()) {
            account.setRingNotified(true);
            ringAndVibrate = true;
        }

        NotificationSetting n = account.getNotificationSetting();

        configureNotification(
                builder,
                (n.shouldRing()) ?  n.getRingtone() : null,
                (n.shouldVibrate()) ? n.getVibration() : null,
                (n.isLed()) ? Integer.valueOf(n.getLedColor()) : null,
                K9.NOTIFICATION_LED_BLINK_SLOW,
                ringAndVibrate);

        notifMgr.notify(account.getAccountNumber(), builder.build());
    }

    private TaskStackBuilder buildAccountsBackStack(Context context) {
        TaskStackBuilder stack = TaskStackBuilder.create(context);
        if (!skipAccountsInBackStack(context)) {
            stack.addNextIntent(new Intent(context, Accounts.class).putExtra(Accounts.EXTRA_STARTUP, false));
        }
        return stack;
    }

    private TaskStackBuilder buildFolderListBackStack(Context context, Account account) {
        TaskStackBuilder stack = buildAccountsBackStack(context);
        stack.addNextIntent(FolderList.actionHandleAccountIntent(context, account, false));
        return stack;
    }

    private TaskStackBuilder buildUnreadBackStack(Context context, final Account account) {
        TaskStackBuilder stack = buildAccountsBackStack(context);
        String description = context.getString(R.string.search_title,
                account.getDescription(), context.getString(R.string.unread_modifier));
        LocalSearch search = new LocalSearch(description);
        search.addAccountUuid(account.getUuid());
        search.and(Searchfield.READ, "1", Attribute.NOT_EQUALS);
        stack.addNextIntent(MessageList.intentDisplaySearch(context, search, true, false, false));
        return stack;
    }

    private TaskStackBuilder buildMessageListBackStack(Context context, Account account, String folder) {
        TaskStackBuilder stack = skipFolderListInBackStack(context, account, folder)
                ? buildAccountsBackStack(context)
                : buildFolderListBackStack(context, account);

        if (folder != null) {
            LocalSearch search = new LocalSearch(folder);
            search.addAllowedFolder(folder);
            search.addAccountUuid(account.getUuid());
            stack.addNextIntent(MessageList.intentDisplaySearch(context, search, false, true, true));
        }
        return stack;
    }

    private TaskStackBuilder buildMessageViewBackStack(Context context, MessageReference message) {
        Account account = Preferences.getPreferences(context).getAccount(message.accountUuid);
        TaskStackBuilder stack = buildMessageListBackStack(context, account, message.folderName);
        stack.addNextIntent(MessageList.actionDisplayMessageIntent(context, message));
        return stack;
    }

    private boolean skipFolderListInBackStack(Context context, Account account, String folder) {
        return folder != null && folder.equals(account.getAutoExpandFolderName());
    }

    private boolean skipAccountsInBackStack(Context context) {
        return Preferences.getPreferences(context).getAccounts().length == 1;
    }

    /**
     * Configure the notification sound and LED
     *
     * @param builder
     *         {@link NotificationCompat.Builder} instance used to configure the notification.
     *         Never {@code null}.
     * @param ringtone
     *          String name of ringtone. {@code null}, if no ringtone should be played.
     * @param vibrationPattern
     *         {@code long[]} vibration pattern. {@code null}, if no vibration should be played.
     * @param ledColor
     *         Color to flash LED. {@code null}, if no LED flash should happen.
     * @param ledSpeed
     *         Either {@link K9#NOTIFICATION_LED_BLINK_SLOW} or
     *         {@link K9#NOTIFICATION_LED_BLINK_FAST}.
     * @param ringAndVibrate
     *          {@code true}, if ringtone/vibration are allowed. {@code false}, otherwise.
     */
    private void configureNotification(NotificationCompat.Builder builder, String ringtone,
            long[] vibrationPattern, Integer ledColor, int ledSpeed, boolean ringAndVibrate) {

        // if it's quiet time, then we shouldn't be ringing, buzzing or flashing
        if (K9.isQuietTime()) {
            return;
        }

        if (ringAndVibrate) {
            if (ringtone != null && !TextUtils.isEmpty(ringtone)) {
                builder.setSound(Uri.parse(ringtone));
            }

            if (vibrationPattern != null) {
                builder.setVibrate(vibrationPattern);
            }
        }

        if (ledColor != null) {
            int ledOnMS;
            int ledOffMS;
            if (ledSpeed == K9.NOTIFICATION_LED_BLINK_SLOW) {
                ledOnMS = K9.NOTIFICATION_LED_ON_TIME;
                ledOffMS = K9.NOTIFICATION_LED_OFF_TIME;
            } else {
                ledOnMS = K9.NOTIFICATION_LED_FAST_ON_TIME;
                ledOffMS = K9.NOTIFICATION_LED_FAST_OFF_TIME;
            }

            builder.setLights(ledColor, ledOnMS, ledOffMS);
        }
    }

    /** Cancel a notification of new email messages */
    public void notifyAccountCancel(Context context, Account account) {
        NotificationManager notifMgr =
            (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifMgr.cancel(account.getAccountNumber());
        notifMgr.cancel(-1000 - account.getAccountNumber());
        notificationData.remove(account.getAccountNumber());
    }

    /**
     * Save a draft message.
     * @param account Account we are saving for.
     * @param message Message to save.
     * @return Message representing the entry in the local store.
     */
    public Message saveDraft(final Account account, final Message message, long existingDraftId) {
        Message localMessage = null;
        try {
            LocalStore localStore = account.getLocalStore();
            LocalFolder localFolder = localStore.getFolder(account.getDraftsFolderName());
            localFolder.open(OpenMode.READ_WRITE);

            if (existingDraftId != INVALID_MESSAGE_ID) {
                String uid = localFolder.getMessageUidById(existingDraftId);
                message.setUid(uid);
            }

            // Save the message to the store.
            localFolder.appendMessages(new Message[] {
                                           message
                                       });
            // Fetch the message back from the store.  This is the Message that's returned to the caller.
            localMessage = localFolder.getMessage(message.getUid());
            localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);

            PendingCommand command = new PendingCommand();
            command.command = PENDING_COMMAND_APPEND;
            command.arguments = new String[] {
                localFolder.getName(),
                localMessage.getUid()
            };
            queuePendingCommand(account, command);
            processPendingCommands(account);

        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to save message as draft.", e);
            addErrorMessage(account, null, e);
        }
        return localMessage;
    }

    public long getId(Message message) {
        long id;
        if (message instanceof LocalMessage) {
            id = ((LocalMessage) message).getId();
        } else {
            Log.w(K9.LOG_TAG, "MessagingController.getId() called without a LocalMessage");
            id = INVALID_MESSAGE_ID;
        }

        return id;
    }

    public boolean modeMismatch(Account.FolderMode aMode, Folder.FolderClass fMode) {
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

    static AtomicInteger sequencing = new AtomicInteger(0);
    static class Command implements Comparable<Command> {
        public Runnable runnable;

        public MessagingListener listener;

        public String description;

        boolean isForeground;

        int sequence = sequencing.getAndIncrement();

        @Override
        public int compareTo(Command other) {
            if (other.isForeground && !isForeground) {
                return 1;
            } else if (!other.isForeground && isForeground) {
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
            Preferences prefs = Preferences.getPreferences(mApplication);

            Account.FolderMode aDisplayMode = account.getFolderDisplayMode();
            Account.FolderMode aPushMode = account.getFolderPushMode();

            List<String> names = new ArrayList<String>();

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
                folder.open(Folder.OpenMode.READ_WRITE);
                folder.refresh(prefs);

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
                PushReceiver receiver = new MessagingControllerPushReceiver(mApplication, account, this);
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
                    localFolder.open(OpenMode.READ_WRITE);

                    account.setRingNotified(false);
                    int newCount = downloadMessages(account, remoteFolder, localFolder, messages, flagSyncOnly);

                    int unreadMessageCount = localFolder.getUnreadMessageCount();

                    localFolder.setLastPush(System.currentTimeMillis());
                    localFolder.setStatus(null);

                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "messagesArrived newCount = " + newCount + ", unread count = " + unreadMessageCount);

                    if (unreadMessageCount == 0) {
                        notifyAccountCancel(mApplication, account);
                    }

                    for (MessagingListener l : getListeners()) {
                        l.folderStatusChanged(account, remoteFolder.getName(), unreadMessageCount);
                    }

                } catch (Exception e) {
                    String rootMessage = getRootCauseMessage(e);
                    String errorMessage = "Push failed: " + rootMessage;
                    try {
                        // Oddly enough, using a local variable gets rid of a
                        // potential null pointer access warning with Eclipse.
                        LocalFolder folder = localFolder;
                        folder.setStatus(errorMessage);
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

    enum MemorizingState { STARTED, FINISHED, FAILED }

    static class Memory {
        Account account;
        String folderName;
        MemorizingState syncingState = null;
        MemorizingState sendingState = null;
        MemorizingState pushingState = null;
        MemorizingState processingState = null;
        String failureMessage = null;

        int syncingTotalMessagesInMailbox;
        int syncingNumNewMessages;

        int folderCompleted = 0;
        int folderTotal = 0;
        String processingCommandTitle = null;

        Memory(Account nAccount, String nFolderName) {
            account = nAccount;
            folderName = nFolderName;
        }

        String getKey() {
            return getMemoryKey(account, folderName);
        }


    }
    static String getMemoryKey(Account taccount, String tfolderName) {
        return taccount.getDescription() + ":" + tfolderName;
    }
    static class MemorizingListener extends MessagingListener {
        HashMap<String, Memory> memories = new HashMap<String, Memory>(31);

        Memory getMemory(Account account, String folderName) {
            Memory memory = memories.get(getMemoryKey(account, folderName));
            if (memory == null) {
                memory = new Memory(account, folderName);
                memories.put(memory.getKey(), memory);
            }
            return memory;
        }

        @Override
        public synchronized void synchronizeMailboxStarted(Account account, String folder) {
            Memory memory = getMemory(account, folder);
            memory.syncingState = MemorizingState.STARTED;
            memory.folderCompleted = 0;
            memory.folderTotal = 0;
        }

        @Override
        public synchronized void synchronizeMailboxFinished(Account account, String folder,
                int totalMessagesInMailbox, int numNewMessages) {
            Memory memory = getMemory(account, folder);
            memory.syncingState = MemorizingState.FINISHED;
            memory.syncingTotalMessagesInMailbox = totalMessagesInMailbox;
            memory.syncingNumNewMessages = numNewMessages;
        }

        @Override
        public synchronized void synchronizeMailboxFailed(Account account, String folder,
                String message) {

            Memory memory = getMemory(account, folder);
            memory.syncingState = MemorizingState.FAILED;
            memory.failureMessage = message;
        }
        synchronized void refreshOther(MessagingListener other) {
            if (other != null) {

                Memory syncStarted = null;
                Memory sendStarted = null;
                Memory processingStarted = null;

                for (Memory memory : memories.values()) {

                    if (memory.syncingState != null) {
                        switch (memory.syncingState) {
                        case STARTED:
                            syncStarted = memory;
                            break;
                        case FINISHED:
                            other.synchronizeMailboxFinished(memory.account, memory.folderName,
                                                             memory.syncingTotalMessagesInMailbox, memory.syncingNumNewMessages);
                            break;
                        case FAILED:
                            other.synchronizeMailboxFailed(memory.account, memory.folderName,
                                                           memory.failureMessage);
                            break;
                        }
                    }

                    if (memory.sendingState != null) {
                        switch (memory.sendingState) {
                        case STARTED:
                            sendStarted = memory;
                            break;
                        case FINISHED:
                            other.sendPendingMessagesCompleted(memory.account);
                            break;
                        case FAILED:
                            other.sendPendingMessagesFailed(memory.account);
                            break;
                        }
                    }
                    if (memory.pushingState != null) {
                        switch (memory.pushingState) {
                        case STARTED:
                            other.setPushActive(memory.account, memory.folderName, true);
                            break;
                        case FINISHED:
                            other.setPushActive(memory.account, memory.folderName, false);
                            break;
                        case FAILED:
                            break;
                        }
                    }
                    if (memory.processingState != null) {
                        switch (memory.processingState) {
                        case STARTED:
                            processingStarted = memory;
                            break;
                        case FINISHED:
                        case FAILED:
                            other.pendingCommandsFinished(memory.account);
                            break;
                        }
                    }
                }
                Memory somethingStarted = null;
                if (syncStarted != null) {
                    other.synchronizeMailboxStarted(syncStarted.account, syncStarted.folderName);
                    somethingStarted = syncStarted;
                }
                if (sendStarted != null) {
                    other.sendPendingMessagesStarted(sendStarted.account);
                    somethingStarted = sendStarted;
                }
                if (processingStarted != null) {
                    other.pendingCommandsProcessing(processingStarted.account);
                    if (processingStarted.processingCommandTitle != null) {
                        other.pendingCommandStarted(processingStarted.account, processingStarted.processingCommandTitle);

                    } else {
                        other.pendingCommandCompleted(processingStarted.account, processingStarted.processingCommandTitle);
                    }
                    somethingStarted = processingStarted;
                }
                if (somethingStarted != null && somethingStarted.folderTotal > 0) {
                    other.synchronizeMailboxProgress(somethingStarted.account, somethingStarted.folderName, somethingStarted.folderCompleted, somethingStarted.folderTotal);
                }

            }
        }
        @Override
        public synchronized void setPushActive(Account account, String folderName, boolean active) {
            Memory memory = getMemory(account, folderName);
            memory.pushingState = (active ? MemorizingState.STARTED : MemorizingState.FINISHED);
        }

        @Override
        public synchronized void sendPendingMessagesStarted(Account account) {
            Memory memory = getMemory(account, null);
            memory.sendingState = MemorizingState.STARTED;
            memory.folderCompleted = 0;
            memory.folderTotal = 0;
        }

        @Override
        public synchronized void sendPendingMessagesCompleted(Account account) {
            Memory memory = getMemory(account, null);
            memory.sendingState = MemorizingState.FINISHED;
        }

        @Override
        public synchronized void sendPendingMessagesFailed(Account account) {
            Memory memory = getMemory(account, null);
            memory.sendingState = MemorizingState.FAILED;
        }


        @Override
        public synchronized void synchronizeMailboxProgress(Account account, String folderName, int completed, int total) {
            Memory memory = getMemory(account, folderName);
            memory.folderCompleted = completed;
            memory.folderTotal = total;
        }


        @Override
        public synchronized void pendingCommandsProcessing(Account account) {
            Memory memory = getMemory(account, null);
            memory.processingState = MemorizingState.STARTED;
            memory.folderCompleted = 0;
            memory.folderTotal = 0;
        }
        @Override
        public synchronized void pendingCommandsFinished(Account account) {
            Memory memory = getMemory(account, null);
            memory.processingState = MemorizingState.FINISHED;
        }
        @Override
        public synchronized void pendingCommandStarted(Account account, String commandTitle) {
            Memory memory = getMemory(account, null);
            memory.processingCommandTitle = commandTitle;
        }

        @Override
        public synchronized void pendingCommandCompleted(Account account, String commandTitle) {
            Memory memory = getMemory(account, null);
            memory.processingCommandTitle = null;
        }

    }

    private void actOnMessages(List<Message> messages, MessageActor actor) {
        Map<Account, Map<Folder, List<Message>>> accountMap = new HashMap<Account, Map<Folder, List<Message>>>();

        for (Message message : messages) {
            Folder folder = message.getFolder();
            Account account = folder.getAccount();

            Map<Folder, List<Message>> folderMap = accountMap.get(account);
            if (folderMap == null) {
                folderMap = new HashMap<Folder, List<Message>>();
                accountMap.put(account, folderMap);
            }
            List<Message> messageList = folderMap.get(folder);
            if (messageList == null) {
                messageList = new LinkedList<Message>();
                folderMap.put(folder, messageList);
            }

            messageList.add(message);
        }
        for (Map.Entry<Account, Map<Folder, List<Message>>> entry : accountMap.entrySet()) {
            Account account = entry.getKey();

            //account.refresh(Preferences.getPreferences(K9.app));
            Map<Folder, List<Message>> folderMap = entry.getValue();
            for (Map.Entry<Folder, List<Message>> folderEntry : folderMap.entrySet()) {
                Folder folder = folderEntry.getKey();
                List<Message> messageList = folderEntry.getValue();
                actor.act(account, folder, messageList);
            }
        }
    }

    interface MessageActor {
        public void act(final Account account, final Folder folder, final List<Message> messages);
    }
}
