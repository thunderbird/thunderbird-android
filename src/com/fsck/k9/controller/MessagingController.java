
package com.fsck.k9.controller;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Application;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.PowerManager;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9;
import com.fsck.k9.NotificationSetting;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.SearchSpecification;
import com.fsck.k9.activity.FolderList;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.helper.power.TracingPowerManager;
import com.fsck.k9.helper.power.TracingPowerManager.TracingWakeLock;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Folder.FolderType;
import com.fsck.k9.mail.Folder.OpenMode;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.Pusher;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mail.store.UnavailableAccountException;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.UnavailableStorageException;
import com.fsck.k9.mail.store.LocalStore.LocalFolder;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;
import com.fsck.k9.mail.store.LocalStore.PendingCommand;


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
public class MessagingController implements Runnable
{

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
    private static final String PENDING_COMMAND_EMPTY_TRASH = "com.fsck.k9.MessagingController.emptyTrash";
    private static final String PENDING_COMMAND_SET_FLAG_BULK = "com.fsck.k9.MessagingController.setFlagBulk";
    private static final String PENDING_COMMAND_SET_FLAG = "com.fsck.k9.MessagingController.setFlag";
    private static final String PENDING_COMMAND_APPEND = "com.fsck.k9.MessagingController.append";
    private static final String PENDING_COMMAND_MARK_ALL_AS_READ = "com.fsck.k9.MessagingController.markAllAsRead";
    private static final String PENDING_COMMAND_EXPUNGE = "com.fsck.k9.MessagingController.expunge";

    private static MessagingController inst = null;
    private BlockingQueue<Command> mCommands = new PriorityBlockingQueue<Command>();

    private Thread mThread;
    private Set<MessagingListener> mListeners = new CopyOnWriteArraySet<MessagingListener>();

    private HashMap<SORT_TYPE, Boolean> sortAscending = new HashMap<SORT_TYPE, Boolean>();

    private ConcurrentHashMap<String, AtomicInteger> sendCount = new ConcurrentHashMap<String, AtomicInteger>();

    ConcurrentHashMap<Account, Pusher> pushers = new ConcurrentHashMap<Account, Pusher>();

    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public enum SORT_TYPE
    {
        SORT_DATE(R.string.sort_earliest_first, R.string.sort_latest_first, false),
        SORT_SUBJECT(R.string.sort_subject_alpha, R.string.sort_subject_re_alpha, true),
        SORT_SENDER(R.string.sort_sender_alpha, R.string.sort_sender_re_alpha, true),
        SORT_UNREAD(R.string.sort_unread_first, R.string.sort_unread_last, true),
        SORT_FLAGGED(R.string.sort_flagged_first, R.string.sort_flagged_last, true),
        SORT_ATTACHMENT(R.string.sort_attach_first, R.string.sort_unattached_first, true);

        private int ascendingToast;
        private int descendingToast;
        private boolean defaultAscending;

        SORT_TYPE(int ascending, int descending, boolean ndefaultAscending)
        {
            ascendingToast = ascending;
            descendingToast = descending;
            defaultAscending = ndefaultAscending;
        }

        public int getToast(boolean ascending)
        {
            if (ascending)
            {
                return ascendingToast;
            }
            else
            {
                return descendingToast;
            }
        }
        public boolean isDefaultAscending()
        {
            return defaultAscending;
        }
    };
    private SORT_TYPE sortType = SORT_TYPE.SORT_DATE;

    private MessagingListener checkMailListener = null;

    private MemorizingListener memorizingListener = new MemorizingListener();

    private boolean mBusy;
    private Application mApplication;

    // Key is accountUuid:folderName:messageUid   ,   value is unimportant
    private ConcurrentHashMap<String, String> deletedUids = new ConcurrentHashMap<String, String>();

    private String createMessageKey(Account account, String folder, Message message)
    {
        return createMessageKey(account, folder, message.getUid());
    }

    private String createMessageKey(Account account, String folder, String uid)
    {
        return account.getUuid() + ":" + folder + ":" + uid;
    }

    private void suppressMessage(Account account, String folder, Message message)
    {

        if (account == null || folder == null || message == null)
        {
            return;
        }
        String messKey = createMessageKey(account, folder, message);
        deletedUids.put(messKey, "true");
    }

    private void unsuppressMessage(Account account, String folder, String uid)
    {
        if (account == null || folder == null || uid == null)
        {
            return;
        }
        String messKey = createMessageKey(account, folder, uid);
        deletedUids.remove(messKey);
    }


    private boolean isMessageSuppressed(Account account, String folder, Message message)
    {
        if (account == null || folder == null || message == null)
        {
            return false;
        }
        String messKey = createMessageKey(account, folder, message);

        if (deletedUids.containsKey(messKey))
        {
            return true;
        }

        return false;
    }

    private MessagingController(Application application)
    {
        mApplication = application;
        mThread = new Thread(this);
        mThread.start();
        if (memorizingListener != null)
        {
            addListener(memorizingListener);
        }
    }

    /**
     * Gets or creates the singleton instance of MessagingController. Application is used to
     * provide a Context to classes that need it.
     * @param application
     * @return
     */
    public synchronized static MessagingController getInstance(Application application)
    {
        if (inst == null)
        {
            inst = new MessagingController(application);
        }
        return inst;
    }

    public boolean isBusy()
    {
        return mBusy;
    }

    public void run()
    {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true)
        {
            String commandDescription = null;
            try
            {
                final Command command = mCommands.take();

                if (command != null)
                {
                    commandDescription = command.description;

                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Running " + (command.isForeground ? "Foreground" : "Background") + " command '" + command.description + "', seq = " + command.sequence);

                    mBusy = true;
                    try
                    {
                        command.runnable.run();
                    }
                    catch (UnavailableAccountException e)
                    {
                        // retry later
                        new Thread()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    sleep(30 * 1000);
                                    mCommands.put(command);
                                }
                                catch (InterruptedException e)
                                {
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

                    for (MessagingListener l : getListeners(command.listener))
                    {
                        l.controllerCommandCompleted(mCommands.size() > 0);
                    }
                }
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Error running command '" + commandDescription + "'", e);
            }
            mBusy = false;
        }
    }

    private void put(String description, MessagingListener listener, Runnable runnable)
    {
        putCommand(mCommands, description, listener, runnable, true);
    }

    private void putBackground(String description, MessagingListener listener, Runnable runnable)
    {
        putCommand(mCommands, description, listener, runnable, false);
    }

    private void putCommand(BlockingQueue<Command> queue, String description, MessagingListener listener, Runnable runnable, boolean isForeground)
    {
        int retries = 10;
        Exception e = null;
        while (retries-- > 0)
        {
            try
            {
                Command command = new Command();
                command.listener = listener;
                command.runnable = runnable;
                command.description = description;
                command.isForeground = isForeground;
                queue.put(command);
                return;
            }
            catch (InterruptedException ie)
            {
                try
                {
                    Thread.sleep(200);
                }
                catch (InterruptedException ne)
                {
                }
                e = ie;
            }
        }
        throw new Error(e);
    }


    public void addListener(MessagingListener listener)
    {
        mListeners.add(listener);
        refreshListener(listener);
    }

    public void refreshListener(MessagingListener listener)
    {
        if (memorizingListener != null && listener != null)
        {
            memorizingListener.refreshOther(listener);
        }
    }

    public void removeListener(MessagingListener listener)
    {
        mListeners.remove(listener);
    }

    public Set<MessagingListener> getListeners()
    {
        return mListeners;
    }


    public Set<MessagingListener> getListeners(MessagingListener listener)
    {
        if (listener == null)
        {
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
     * @param includeRemote
     * @param listener
     * @throws MessagingException
     */
    public void listFolders(final Account account, final boolean refreshRemote, final MessagingListener listener)
    {
        threadPool.execute(new Runnable()
        {
            public void run()
            {
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
     * @param includeRemote
     * @param listener
     * @throws MessagingException
     */
    public void listFoldersSynchronous(final Account account, final boolean refreshRemote, final MessagingListener listener)
    {
        for (MessagingListener l : getListeners(listener))
        {
            l.listFoldersStarted(account);
        }
        List<? extends Folder> localFolders = null;
        if (!account.isAvailable(mApplication))
        {
            Log.i(K9.LOG_TAG, "not listing folders of unavailable account");
        }
        else
        {
            try
            {
                Store localStore = account.getLocalStore();
                localFolders = localStore.getPersonalNamespaces(false);

                Folder[] folderArray = localFolders.toArray(EMPTY_FOLDER_ARRAY);

                if (refreshRemote || localFolders == null || localFolders.size() == 0)
                {
                    doRefreshRemote(account, listener);
                    return;
                }

                for (MessagingListener l : getListeners(listener))
                {
                    l.listFolders(account, folderArray);
                }
            }
            catch (Exception e)
            {
                for (MessagingListener l : getListeners(listener))
                {
                    l.listFoldersFailed(account, e.getMessage());
                }

                addErrorMessage(account, null, e);
                return;
            }
            finally
            {
                if (localFolders != null)
                {
                    for (Folder localFolder : localFolders)
                    {
                        closeFolder(localFolder);
                    }
                }
            }
        }

        for (MessagingListener l : getListeners(listener))
        {
            l.listFoldersFinished(account);
        }
    }

    private void doRefreshRemote(final Account account, MessagingListener listener)
    {
        put("doRefreshRemote", listener, new Runnable()
        {
            public void run()
            {
                List<? extends Folder> localFolders = null;
                try
                {
                    Store store = account.getRemoteStore();

                    List<? extends Folder> remoteFolders = store.getPersonalNamespaces(false);

                    LocalStore localStore = account.getLocalStore();
                    HashSet<String> remoteFolderNames = new HashSet<String>();
                    for (int i = 0, count = remoteFolders.size(); i < count; i++)
                    {
                        LocalFolder localFolder = localStore.getFolder(remoteFolders.get(i).getName());
                        if (!localFolder.exists())
                        {
                            localFolder.create(FolderType.HOLDS_MESSAGES, account.getDisplayCount());
                        }
                        remoteFolderNames.add(remoteFolders.get(i).getName());
                    }

                    localFolders = localStore.getPersonalNamespaces(false);

                    /*
                     * Clear out any folders that are no longer on the remote store.
                     */
                    for (Folder localFolder : localFolders)
                    {
                        String localFolderName = localFolder.getName();
                        if (localFolderName.equalsIgnoreCase(K9.INBOX) ||
                                localFolderName.equals(account.getTrashFolderName()) ||
                                localFolderName.equals(account.getOutboxFolderName()) ||
                                localFolderName.equals(account.getDraftsFolderName()) ||
                                localFolderName.equals(account.getSentFolderName()) ||
                                localFolderName.equals(account.getErrorFolderName()))
                        {
                            continue;
                        }
                        if (!remoteFolderNames.contains(localFolder.getName()))
                        {
                            localFolder.delete(false);
                        }
                    }

                    localFolders = localStore.getPersonalNamespaces(false);
                    Folder[] folderArray = localFolders.toArray(EMPTY_FOLDER_ARRAY);

                    for (MessagingListener l : getListeners())
                    {
                        l.listFolders(account, folderArray);
                    }
                    for (MessagingListener l : getListeners())
                    {
                        l.listFoldersFinished(account);
                    }
                }
                catch (Exception e)
                {
                    for (MessagingListener l : getListeners())
                    {
                        l.listFoldersFailed(account, "");
                    }
                    addErrorMessage(account, null, e);
                }
                finally
                {
                    if (localFolders != null)
                    {
                        for (Folder localFolder : localFolders)
                        {
                            closeFolder(localFolder);
                        }
                    }
                }
            }
        });
    }



    /**
     * List the messages in the local message store for the given folder asynchronously.
     *
     * @param account
     * @param folder
     * @param listener
     * @throws MessagingException
     */
    public void listLocalMessages(final Account account, final String folder, final MessagingListener listener)
    {
        threadPool.execute(new Runnable()
        {
            public void run()
            {
                listLocalMessagesSynchronous(account, folder, listener);
            }
        });
    }


    /**
     * List the messages in the local message store for the given folder synchronously.
     *
     * @param account
     * @param folder
     * @param listener
     * @throws MessagingException
     */
    public void listLocalMessagesSynchronous(final Account account, final String folder, final MessagingListener listener)
    {

        for (MessagingListener l : getListeners(listener))
        {
            l.listLocalMessagesStarted(account, folder);
        }

        Folder localFolder = null;
        MessageRetrievalListener retrievalListener =
            new MessageRetrievalListener()
        {
            List<Message> pendingMessages = new ArrayList<Message>();


            int totalDone = 0;


            public void messageStarted(String message, int number, int ofTotal) {}
            public void messageFinished(Message message, int number, int ofTotal)
            {

                if (!isMessageSuppressed(account, folder, message))
                {
                    pendingMessages.add(message);
                    totalDone++;
                    if (pendingMessages.size() > 10)
                    {
                        addPendingMessages();
                    }

                }
                else
                {
                    for (MessagingListener l : getListeners(listener))
                    {
                        l.listLocalMessagesRemoveMessage(account, folder, message);
                    }
                }
            }
            public void messagesFinished(int number)
            {
                addPendingMessages();
            }
            private void addPendingMessages()
            {
                for (MessagingListener l : getListeners(listener))
                {
                    l.listLocalMessagesAddMessages(account, folder, pendingMessages);
                }
                pendingMessages.clear();
            }
        };



        try
        {
            Store localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folder);
            localFolder.open(OpenMode.READ_WRITE);

            localFolder.getMessages(
                retrievalListener,
                false // Skip deleted messages
            );
            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "Got ack that callbackRunner finished");

            for (MessagingListener l : getListeners(listener))
            {
                l.listLocalMessagesFinished(account, folder);
            }
        }
        catch (Exception e)
        {
            for (MessagingListener l : getListeners(listener))
            {
                l.listLocalMessagesFailed(account, folder, e.getMessage());
            }
            addErrorMessage(account, null, e);
        }
        finally
        {
            closeFolder(localFolder);
        }
    }

    public void searchLocalMessages(SearchSpecification searchSpecification, final Message[] messages, final MessagingListener listener)
    {
        searchLocalMessages(searchSpecification.getAccountUuids(), searchSpecification.getFolderNames(), messages,
                            searchSpecification.getQuery(), searchSpecification.isIntegrate(), searchSpecification.getRequiredFlags(), searchSpecification.getForbiddenFlags(), listener);
    }


    /**
     * Find all messages in any local account which match the query 'query'
     * @param folderNames TODO
     * @param query
     * @param listener
     * @param searchAccounts TODO
     * @param account TODO
     * @param account
     * @throws MessagingException
     */
    public void searchLocalMessages(final String[] accountUuids, final String[] folderNames, final Message[] messages, final String query, final boolean integrate,
                                    final Flag[] requiredFlags, final Flag[] forbiddenFlags, final MessagingListener listener)
    {
        if (K9.DEBUG)
        {
            Log.i(K9.LOG_TAG, "searchLocalMessages ("
                  + "accountUuids=" + Utility.combine(accountUuids, ',')
                  + ", folderNames = " + Utility.combine(folderNames, ',')
                  + ", messages.size() = " + (messages != null ? messages.length : null)
                  + ", query = " + query
                  + ", integrate = " + integrate
                  + ", requiredFlags = " + Utility.combine(requiredFlags, ',')
                  + ", forbiddenFlags = " + Utility.combine(forbiddenFlags, ',')
                  + ")");
        }

        threadPool.execute(new Runnable()
        {
            public void run()
            {
                searchLocalMessagesSynchronous(accountUuids,folderNames, messages,  query, integrate, requiredFlags, forbiddenFlags, listener);
            }
        });
    }
    public void searchLocalMessagesSynchronous(final String[] accountUuids, final String[] folderNames, final Message[] messages, final String query, final boolean integrate, final Flag[] requiredFlags, final Flag[] forbiddenFlags, final MessagingListener listener)
    {

        final AccountStats stats = new AccountStats();
        final Set<String> accountUuidsSet = new HashSet<String>();
        if (accountUuids != null)
        {
            for (String accountUuid : accountUuids)
            {
                accountUuidsSet.add(accountUuid);
            }
        }
        final Preferences prefs = Preferences.getPreferences(mApplication.getApplicationContext());
        Account[] accounts = prefs.getAccounts();
        List<LocalFolder> foldersToSearch = null;
        boolean displayableOnly = false;
        boolean noSpecialFolders = true;
        for (final Account account : accounts)
        {
            if (!account.isAvailable(mApplication))
            {
                Log.d(K9.LOG_TAG, "searchLocalMessagesSynchronous() ignores account that is not available");
                continue;
            }
            if (accountUuids != null && !accountUuidsSet.contains(account.getUuid()))
            {
                continue;
            }

            if (accountUuids != null && accountUuidsSet.contains(account.getUuid()))
            {
                displayableOnly = true;
                noSpecialFolders = true;
            }
            else if (!integrate && folderNames == null)
            {
                Account.Searchable searchableFolders = account.getSearchableFolders();
                switch (searchableFolders)
                {
                    case NONE:
                        continue;
                    case DISPLAYABLE:
                        displayableOnly = true;
                        break;

                }
            }
            List<Message> messagesToSearch = null;
            if (messages != null)
            {
                messagesToSearch = new LinkedList<Message>();
                for (Message message : messages)
                {
                    if (message.getFolder().getAccount().getUuid().equals(account.getUuid()))
                    {
                        messagesToSearch.add(message);
                    }
                }
                if (messagesToSearch.isEmpty())
                {
                    continue;
                }
            }
            if (listener != null)
            {
                listener.listLocalMessagesStarted(account, null);
            }

            if (integrate || displayableOnly || folderNames != null || noSpecialFolders)
            {
                List<LocalFolder> tmpFoldersToSearch = new LinkedList<LocalFolder>();
                try
                {
                    LocalStore store = account.getLocalStore();
                    List<? extends Folder> folders = store.getPersonalNamespaces(false);
                    Set<String> folderNameSet = null;
                    if (folderNames != null)
                    {
                        folderNameSet = new HashSet<String>();
                        for (String folderName : folderNames)
                        {
                            folderNameSet.add(folderName);
                        }
                    }
                    for (Folder folder : folders)
                    {
                        LocalFolder localFolder = (LocalFolder)folder;
                        boolean include = true;
                        folder.refresh(prefs);
                        String localFolderName = localFolder.getName();
                        if (integrate)
                        {
                            include = localFolder.isIntegrate();
                        }
                        else
                        {
                            if (folderNameSet != null)
                            {
                                if (!folderNameSet.contains(localFolderName))

                                {
                                    include = false;
                                }
                            }
                            // Never exclude the INBOX (see issue 1817)
                            else if (noSpecialFolders && !localFolderName.equals(K9.INBOX) && (
                                         localFolderName.equals(account.getTrashFolderName()) ||
                                         localFolderName.equals(account.getOutboxFolderName()) ||
                                         localFolderName.equals(account.getDraftsFolderName()) ||
                                         localFolderName.equals(account.getSentFolderName()) ||
                                         localFolderName.equals(account.getErrorFolderName())))
                            {
                                include = false;
                            }
                            else if (displayableOnly && modeMismatch(account.getFolderDisplayMode(), folder.getDisplayClass()))
                            {
                                include = false;
                            }
                        }

                        if (include)
                        {
                            tmpFoldersToSearch.add(localFolder);
                        }
                    }
                    if (tmpFoldersToSearch.size() < 1)
                    {
                        continue;
                    }
                    foldersToSearch = tmpFoldersToSearch;
                }
                catch (MessagingException me)
                {
                    Log.e(K9.LOG_TAG, "Unable to restrict search folders in Account " + account.getDescription() + ", searching all", me);
                    addErrorMessage(account, null, me);
                }

            }

            MessageRetrievalListener retrievalListener = new MessageRetrievalListener()
            {
                public void messageStarted(String message, int number, int ofTotal) {}
                public void messageFinished(Message message, int number, int ofTotal)
                {
                    if (!isMessageSuppressed(message.getFolder().getAccount(), message.getFolder().getName(), message))
                    {
                        List<Message> messages = new ArrayList<Message>();

                        messages.add(message);
                        stats.unreadMessageCount += (!message.isSet(Flag.SEEN)) ? 1 : 0;
                        stats.flaggedMessageCount += (message.isSet(Flag.FLAGGED)) ? 1 : 0;
                        if (listener != null)
                        {
                            listener.listLocalMessagesAddMessages(account, null, messages);
                        }
                    }

                }
                public void messagesFinished(int number)
                {

                }
            };

            try
            {
                String[] queryFields = {"html_content","subject","sender_list"};
                LocalStore localStore = account.getLocalStore();
                localStore.searchForMessages(retrievalListener, queryFields
                                             , query, foldersToSearch,
                                             messagesToSearch == null ? null : messagesToSearch.toArray(EMPTY_MESSAGE_ARRAY),
                                             requiredFlags, forbiddenFlags);

            }
            catch (Exception e)
            {
                if (listener != null)
                {
                    listener.listLocalMessagesFailed(account, null, e.getMessage());
                }
                addErrorMessage(account, null, e);
            }
            finally
            {
                if (listener != null)
                {
                    listener.listLocalMessagesFinished(account, null);
                }
            }
        }
        if (listener != null)
        {
            listener.searchStats(stats);
        }
    }
    public void loadMoreMessages(Account account, String folder, MessagingListener listener)
    {
        try
        {
            LocalStore localStore = account.getLocalStore();
            LocalFolder localFolder = localStore.getFolder(folder);
            if (localFolder.getVisibleLimit() > 0 )
            {
                localFolder.setVisibleLimit(localFolder.getVisibleLimit() + account.getDisplayCount());
            }
            synchronizeMailbox(account, folder, listener, null);
        }
        catch (MessagingException me)
        {
            addErrorMessage(account, null, me);

            throw new RuntimeException("Unable to set visible limit on folder", me);
        }
    }

    public void resetVisibleLimits(Collection<Account> accounts)
    {
        for (Account account : accounts)
        {
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
    public void synchronizeMailbox(final Account account, final String folder, final MessagingListener listener, final Folder providedRemoteFolder)
    {
        putBackground("synchronizeMailbox", listener, new Runnable()
        {
            public void run()
            {
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
    private void synchronizeMailboxSynchronous(final Account account, final String folder, final MessagingListener listener, Folder providedRemoteFolder)
    {
        Folder remoteFolder = null;
        LocalFolder tLocalFolder = null;

        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Synchronizing folder " + account.getDescription() + ":" + folder);

        for (MessagingListener l : getListeners(listener))
        {
            l.synchronizeMailboxStarted(account, folder);
        }
        /*
         * We don't ever sync the Outbox or errors folder
         */
        if (folder.equals(account.getOutboxFolderName()) || folder.equals(account.getErrorFolderName()))
        {
            for (MessagingListener l : getListeners(listener))
            {
                l.synchronizeMailboxFinished(account, folder, 0, 0);
            }

            return;
        }

        Exception commandException = null;
        try
        {
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "SYNC: About to process pending commands for account " +
                      account.getDescription());

            try
            {
                processPendingCommandsSynchronous(account);
            }
            catch (Exception e)
            {
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
            Message[] localMessages = localFolder.getMessages(null);
            HashMap<String, Message> localUidMap = new HashMap<String, Message>();
            for (Message message : localMessages)
            {
                localUidMap.put(message.getUid(), message);
            }

            if (providedRemoteFolder != null)
            {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "SYNC: using providedRemoteFolder " + folder);

                remoteFolder = providedRemoteFolder;
            }
            else
            {
                Store remoteStore = account.getRemoteStore();

                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "SYNC: About to get remote folder " + folder);
                remoteFolder = remoteStore.getFolder(folder);



                if (!  verifyOrCreateRemoteSpecialFolder(account, folder, remoteFolder, listener))
                {
                    return;
                }


                /*
                 * Synchronization process:
                Open the folder
                Upload any local messages that are marked as PENDING_UPLOAD (Drafts, Sent, Trash)
                Get the message count
                Get the list of the newest K9.DEFAULT_VISIBLE_LIMIT messages
                getMessages(messageCount - K9.DEFAULT_VISIBLE_LIMIT, messageCount)
                See if we have each message locally, if not fetch it's flags and envelope
                Get and update the unread count for the folder
                Update the remote flags of any messages we have locally with an internal date
                newer than the remote message.
                Get the current flags for any messages we have locally but did not just download
                Update local flags
                For any message we have locally but not remotely, delete the local message to keep
                cache clean.
                Download larger parts of any new messages.
                (Optional) Download small attachments in the background.
                 */

                /*
                 * Open the remote folder. This pre-loads certain metadata like message count.
                 */
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "SYNC: About to open remote folder " + folder);

                remoteFolder.open(OpenMode.READ_WRITE);
                if (Account.EXPUNGE_ON_POLL.equals(account.getExpungePolicy()))
                {
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

            if (visibleLimit < 0)
            {
                visibleLimit = K9.DEFAULT_VISIBLE_LIMIT;
            }

            Message[] remoteMessageArray = EMPTY_MESSAGE_ARRAY;
            final ArrayList<Message> remoteMessages = new ArrayList<Message>();
            //  final ArrayList<Message> unsyncedMessages = new ArrayList<Message>();
            HashMap<String, Message> remoteUidMap = new HashMap<String, Message>();

            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "SYNC: Remote message count for folder " + folder + " is " + remoteMessageCount);
            final Date earliestDate = account.getEarliestPollDate();
            if (remoteMessageCount > 0)
            {
                /*
                 * Message numbers start at 1.
                 */
                int remoteStart;
                if (visibleLimit > 0 )
                {
                    remoteStart = Math.max(0, remoteMessageCount - visibleLimit) + 1;
                }
                else
                {
                    remoteStart = 1;
                }
                int remoteEnd = remoteMessageCount;

                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "SYNC: About to get messages " + remoteStart + " through " + remoteEnd + " for folder " + folder);

                final AtomicInteger headerProgress = new AtomicInteger(0);
                for (MessagingListener l : getListeners(listener))
                {
                    l.synchronizeMailboxHeadersStarted(account, folder);
                }


                remoteMessageArray = remoteFolder.getMessages(remoteStart, remoteEnd, earliestDate, null);

                int messageCount = remoteMessageArray.length;

                for (Message thisMess : remoteMessageArray)
                {
                    headerProgress.incrementAndGet();
                    for (MessagingListener l : getListeners(listener))
                    {
                        l.synchronizeMailboxHeadersProgress(account, folder, headerProgress.get(), messageCount);
                    }
                    Message localMessage = localUidMap.get(thisMess.getUid());
                    if (localMessage == null || !localMessage.olderThan(earliestDate))
                    {
                        remoteMessages.add(thisMess);
                        remoteUidMap.put(thisMess.getUid(), thisMess);
                    }
                }
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "SYNC: Got " + remoteUidMap.size() + " messages for folder " + folder);

                remoteMessageArray = null;
                for (MessagingListener l : getListeners(listener))
                {
                    l.synchronizeMailboxHeadersFinished(account, folder, headerProgress.get(), remoteUidMap.size());
                }

            }
            else if (remoteMessageCount < 0)
            {
                throw new Exception("Message count " + remoteMessageCount + " for folder " + folder);
            }

            /*
             * Remove any messages that are in the local store but no longer on the remote store or are too old
             */
            if (account.syncRemoteDeletions())
            {
                for (Message localMessage : localMessages)
                {
                    if (remoteUidMap.get(localMessage.getUid()) == null)
                    {
                        localMessage.destroy();

                        for (MessagingListener l : getListeners(listener))
                        {
                            l.synchronizeMailboxRemovedMessage(account, folder, localMessage);
                        }
                    }
                }
            }
            localMessages = null;

            /*
             * Now we download the actual content of messages.
             */
            int newMessages = downloadMessages(account, remoteFolder, localFolder, remoteMessages, false);

            int unreadMessageCount = setLocalUnreadCountToRemote(localFolder, remoteFolder,  newMessages);
            setLocalFlaggedCountToRemote(localFolder, remoteFolder);


            for (MessagingListener l : getListeners())
            {
                l.folderStatusChanged(account, folder, unreadMessageCount);
            }

            /*
             * Notify listeners that we're finally done.
             */

            localFolder.setLastChecked(System.currentTimeMillis());
            localFolder.setStatus(null);

            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "Done synchronizing folder " +
                      account.getDescription() + ":" + folder + " @ " + new Date() +
                      " with " + newMessages + " new messages");

            for (MessagingListener l : getListeners(listener))
            {
                l.synchronizeMailboxFinished(account, folder, remoteMessageCount, newMessages);
            }


            if (commandException != null)
            {
                String rootMessage = getRootCauseMessage(commandException);
                Log.e(K9.LOG_TAG, "Root cause failure in " + account.getDescription() + ":" +
                      tLocalFolder.getName() + " was '" + rootMessage + "'");
                localFolder.setStatus(rootMessage);
                for (MessagingListener l : getListeners(listener))
                {
                    l.synchronizeMailboxFailed(account, folder, rootMessage);
                }
            }

            if (K9.DEBUG)
                Log.i(K9.LOG_TAG, "Done synchronizing folder " + account.getDescription() + ":" + folder);

        }
        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "synchronizeMailbox", e);
            // If we don't set the last checked, it can try too often during
            // failure conditions
            String rootMessage = getRootCauseMessage(e);
            if (tLocalFolder != null)
            {
                try
                {
                    tLocalFolder.setStatus(rootMessage);
                    tLocalFolder.setLastChecked(System.currentTimeMillis());
                }
                catch (MessagingException me)
                {
                    Log.e(K9.LOG_TAG, "Could not set last checked on folder " + account.getDescription() + ":" +
                          tLocalFolder.getName(), e);
                }
            }

            for (MessagingListener l : getListeners(listener))
            {
                l.synchronizeMailboxFailed(
                    account,
                    folder,
                    rootMessage);
            }
            addErrorMessage(account, null, e);
            Log.e(K9.LOG_TAG, "Failed synchronizing folder " +
                  account.getDescription() + ":" + folder + " @ " + new Date());

        }
        finally
        {
            if (providedRemoteFolder == null)
            {
                closeFolder(remoteFolder);
            }

            closeFolder(tLocalFolder);
        }

    }


    private void closeFolder(Folder f)
    {
        if (f != null)
        {
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
    private boolean verifyOrCreateRemoteSpecialFolder(final Account account, final String folder, final Folder remoteFolder, final MessagingListener listener) throws MessagingException
    {
        if (folder.equals(account.getTrashFolderName()) ||
                folder.equals(account.getSentFolderName()) ||
                folder.equals(account.getDraftsFolderName()))
        {
            if (!remoteFolder.exists())
            {
                if (!remoteFolder.create(FolderType.HOLDS_MESSAGES))
                {
                    for (MessagingListener l : getListeners(listener))
                    {
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
    private int setLocalUnreadCountToRemote(LocalFolder localFolder, Folder remoteFolder, int newMessageCount) throws MessagingException
    {
        int remoteUnreadMessageCount = remoteFolder.getUnreadMessageCount();
        if (remoteUnreadMessageCount != -1)
        {
            localFolder.setUnreadMessageCount(remoteUnreadMessageCount);
        }
        else
        {
            int unreadCount = 0;
            Message[] messages = localFolder.getMessages(null, false);
            for (Message message : messages)
            {
                if (!message.isSet(Flag.SEEN) && !message.isSet(Flag.DELETED))
                {
                    unreadCount++;
                }
            }
            localFolder.setUnreadMessageCount(unreadCount);
        }
        return localFolder.getUnreadMessageCount();
    }

    private void setLocalFlaggedCountToRemote(LocalFolder localFolder, Folder remoteFolder) throws MessagingException
    {
        int remoteFlaggedMessageCount = remoteFolder.getFlaggedMessageCount();
        if (remoteFlaggedMessageCount != -1)
        {
            localFolder.setFlaggedMessageCount(remoteFlaggedMessageCount);
        }
        else
        {
            int flaggedCount = 0;
            Message[] messages = localFolder.getMessages(null, false);
            for (Message message : messages)
            {
                if (message.isSet(Flag.FLAGGED) && !message.isSet(Flag.DELETED))
                {
                    flaggedCount++;
                }
            }
            localFolder.setFlaggedMessageCount(flaggedCount);
        }
    }

    private int downloadMessages(final Account account, final Folder remoteFolder,
                                 final LocalFolder localFolder, List<Message> inputMessages, boolean flagSyncOnly) throws MessagingException
    {
        final Date earliestDate = account.getEarliestPollDate();
        if (earliestDate != null)
        {
            if (K9.DEBUG)
            {
                Log.d(K9.LOG_TAG, "Only syncing messages after " + earliestDate);
            }
        }
        final String folder = remoteFolder.getName();

        int unreadBeforeStart = 0;
        try
        {
            AccountStats stats = account.getStats(mApplication);
            unreadBeforeStart = stats.unreadMessageCount;

        }
        catch (MessagingException e)
        {
            Log.e(K9.LOG_TAG, "Unable to getUnreadMessageCount for account: " + account, e);
        }

        ArrayList<Message> syncFlagMessages = new ArrayList<Message>();
        List<Message> unsyncedMessages = new ArrayList<Message>();
        final AtomicInteger newMessages = new AtomicInteger(0);

        List<Message> messages = new ArrayList<Message>(inputMessages);

        for (Message message : messages)
        {
            evaluateMessageForDownload(message, folder,localFolder, remoteFolder, account,unsyncedMessages, syncFlagMessages ,flagSyncOnly);
        }

        final AtomicInteger progress = new AtomicInteger(0);
        final int todo = unsyncedMessages.size() + syncFlagMessages.size();
        for (MessagingListener l : getListeners())
        {
            l.synchronizeMailboxProgress(account, folder, progress.get(), todo);
        }

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Have " + unsyncedMessages.size() + " unsynced messages");

        messages.clear();
        final ArrayList<Message> largeMessages = new ArrayList<Message>();
        final ArrayList<Message> smallMessages = new ArrayList<Message>();
        if (unsyncedMessages.size() > 0)
        {

            /*
             * Reverse the order of the messages. Depending on the server this may get us
             * fetch results for newest to oldest. If not, no harm done.
             */
            Collections.reverse(unsyncedMessages);
            int visibleLimit = localFolder.getVisibleLimit();
            int listSize = unsyncedMessages.size();

            if ((visibleLimit > 0) && (listSize > visibleLimit))
            {
                unsyncedMessages = unsyncedMessages.subList(listSize - visibleLimit, listSize);
            }

            FetchProfile fp = new FetchProfile();
            if (remoteFolder.supportsFetchingFlags())
            {
                fp.add(FetchProfile.Item.FLAGS);
            }
            fp.add(FetchProfile.Item.ENVELOPE);

            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "SYNC: About to fetch " + unsyncedMessages.size() + " unsynced messages for folder " + folder);


            fetchUnsyncedMessages(account, remoteFolder, localFolder, unsyncedMessages, smallMessages,largeMessages, progress, todo, fp);

            // If a message didn't exist, messageFinished won't be called, but we shouldn't try again
            // If we got here, nothing failed
            for (Message message : unsyncedMessages)
            {
                String newPushState = remoteFolder.getNewPushState(localFolder.getPushState(), message);
                if (newPushState != null)
                {
                    localFolder.setPushState(newPushState);
                }
            }
            if (K9.DEBUG)
            {
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

        refreshLocalMessageFlags(account,remoteFolder,localFolder,syncFlagMessages,progress,todo);

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Synced remote messages for folder " + folder + ", " + newMessages.get() + " new messages");

        localFolder.purgeToVisibleLimit(new MessageRemovalListener()
        {
            public void messageRemoved(Message message)
            {
                for (MessagingListener l : getListeners())
                {
                    l.synchronizeMailboxRemovedMessage(account, folder, message);
                }
            }

        });

        return newMessages.get();
    }
    private void evaluateMessageForDownload(final Message message, final String folder,
                                            final LocalFolder localFolder,
                                            final Folder remoteFolder,
                                            final Account account,
                                            final List unsyncedMessages,
                                            final ArrayList syncFlagMessages,
                                            boolean flagSyncOnly) throws MessagingException
    {
        if (message.isSet(Flag.DELETED))
        {
            syncFlagMessages.add(message);
            return;
        }
        else if (isMessageSuppressed(account, folder, message))
        {
            return;
        }

        Message localMessage = localFolder.getMessage(message.getUid());

        if (localMessage == null)
        {
            if (!flagSyncOnly)
            {
                if (!message.isSet(Flag.X_DOWNLOADED_FULL) && !message.isSet(Flag.X_DOWNLOADED_PARTIAL))
                {
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Message with uid " + message.getUid() + " has not yet been downloaded");

                    unsyncedMessages.add(message);
                }
                else
                {
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Message with uid " + message.getUid() + " is partially or fully downloaded");

                    // Store the updated message locally
                    localFolder.appendMessages(new Message[] { message });

                    localMessage = localFolder.getMessage(message.getUid());

                    localMessage.setFlag(Flag.X_DOWNLOADED_FULL, message.isSet(Flag.X_DOWNLOADED_FULL));
                    localMessage.setFlag(Flag.X_DOWNLOADED_PARTIAL, message.isSet(Flag.X_DOWNLOADED_PARTIAL));

                    for (MessagingListener l : getListeners())
                    {
                        l.synchronizeMailboxAddOrUpdateMessage(account, folder, localMessage);
                        if (!localMessage.isSet(Flag.SEEN))
                        {
                            l.synchronizeMailboxNewMessage(account, folder, localMessage);
                        }
                    }
                }
            }
        }
        else if (!localMessage.isSet(Flag.DELETED))
        {
            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "Message with uid " + message.getUid() + " is present in the local store");

            if (!localMessage.isSet(Flag.X_DOWNLOADED_FULL) && !localMessage.isSet(Flag.X_DOWNLOADED_PARTIAL))
            {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "Message with uid " + message.getUid()
                          + " is not downloaded, even partially; trying again");

                unsyncedMessages.add(message);
            }
            else
            {
                String newPushState = remoteFolder.getNewPushState(localFolder.getPushState(), message);
                if (newPushState != null)
                {
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
                                       FetchProfile fp) throws MessagingException
    {
        final String folder = remoteFolder.getName();

        final Date earliestDate = account.getEarliestPollDate();
        remoteFolder.fetch(unsyncedMessages.toArray(EMPTY_MESSAGE_ARRAY), fp,
                           new MessageRetrievalListener()
        {
            public void messageFinished(Message message, int number, int ofTotal)
            {
                try
                {
                    String newPushState = remoteFolder.getNewPushState(localFolder.getPushState(), message);
                    if (newPushState != null)
                    {
                        localFolder.setPushState(newPushState);
                    }
                    if (message.isSet(Flag.DELETED) || message.olderThan(earliestDate))
                    {

                        if (K9.DEBUG)
                        {
                            if (message.isSet(Flag.DELETED))
                            {
                                Log.v(K9.LOG_TAG, "Newly downloaded message " + account + ":" + folder + ":" + message.getUid()
                                      + " was marked deleted on server, skipping");
                            }
                            else
                            {
                                Log.d(K9.LOG_TAG, "Newly downloaded message " + message.getUid() + " is older than "
                                      + earliestDate + ", skipping");
                            }
                        }
                        progress.incrementAndGet();
                        for (MessagingListener l : getListeners())
                        {
                            l.synchronizeMailboxProgress(account, folder, progress.get(), todo);
                        }
                        return;
                    }

                    if (message.getSize() > account.getMaximumAutoDownloadMessageSize())
                    {
                        largeMessages.add(message);
                    }
                    else
                    {
                        smallMessages.add(message);
                    }

                    // And include it in the view
                    if (message.getSubject() != null && message.getFrom() != null)
                    {
                        /*
                         * We check to make sure that we got something worth
                         * showing (subject and from) because some protocols
                         * (POP) may not be able to give us headers for
                         * ENVELOPE, only size.
                         */
                        if (!isMessageSuppressed(account, folder, message))
                        {
                            // Store the new message locally
                            localFolder.appendMessages(new Message[] { message });

                            Message localMessage = localFolder.getMessage(message.getUid());
                            syncFlags(localMessage, message);
                            if (K9.DEBUG)
                                Log.v(K9.LOG_TAG, "About to notify listeners that we got a new unsynced message "
                                      + account + ":" + folder + ":" + message.getUid());
                            for (MessagingListener l : getListeners())
                            {
                                l.synchronizeMailboxAddOrUpdateMessage(account, folder, localMessage);
                            }


                        }

                    }

                }
                catch (Exception e)
                {
                    Log.e(K9.LOG_TAG, "Error while storing downloaded message.", e);
                    addErrorMessage(account, null, e);

                }
            }

            public void messageStarted(String uid, int number, int ofTotal) {}
            public void messagesFinished(int total) {}
        });
    }


    private boolean shouldImportMessage(final Account account, final String folder, final Message message, final AtomicInteger progress, final Date earliestDate)
    {

        if (isMessageSuppressed(account, folder, message))
        {
            if (K9.DEBUG)
            {
                Log.d(K9.LOG_TAG, "Message " + message.getUid() + " was suppressed "+
                      "but just downloaded. "+
                      "The race condition means we wasted some bandwidth. Oh well.");
            }
            return false;

        }
        if (message.olderThan(earliestDate))
        {
            if (K9.DEBUG)
            {
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
                                       FetchProfile fp) throws MessagingException
    {
        final String folder = remoteFolder.getName();

        final Date earliestDate = account.getEarliestPollDate();

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Fetching small messages for folder " + folder);

        remoteFolder.fetch(smallMessages.toArray(new Message[smallMessages.size()]),
                           fp, new MessageRetrievalListener()
        {
            public void messageFinished(Message message, int number, int ofTotal)
            {
                try
                {

                    if (!shouldImportMessage(account, folder, message, progress, earliestDate))
                    {
                        progress.incrementAndGet();

                        return;
                    }

                    // Store the updated message locally
                    localFolder.appendMessages(new Message[] { message });

                    Message localMessage = localFolder.getMessage(message.getUid());
                    progress.incrementAndGet();

                    // Set a flag indicating this message has now be fully downloaded
                    localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "About to notify listeners that we got a new small message "
                              + account + ":" + folder + ":" + message.getUid());

                    // Update the listener with what we've found
                    for (MessagingListener l : getListeners())
                    {
                        l.synchronizeMailboxAddOrUpdateMessage(account, folder, localMessage);
                        l.synchronizeMailboxProgress(account, folder, progress.get(), todo);
                        if (!localMessage.isSet(Flag.SEEN))
                        {
                            l.synchronizeMailboxNewMessage(account, folder, localMessage);
                        }
                    }
                    // Send a notification of this message

                    if (shouldNotifyForMessage(account, message))
                    {
                        newMessages.incrementAndGet();
                        notifyAccount(mApplication, account, message, unreadBeforeStart, newMessages);
                    }

                }
                catch (MessagingException me)
                {
                    addErrorMessage(account, null, me);
                    Log.e(K9.LOG_TAG, "SYNC: fetch small messages", me);
                }
            }

            public void messageStarted(String uid, int number, int ofTotal) {}

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
                                       FetchProfile fp) throws MessagingException
    {
        final String folder = remoteFolder.getName();

        final Date earliestDate = account.getEarliestPollDate();

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Fetching large messages for folder " + folder);

        remoteFolder.fetch(largeMessages.toArray(new Message[largeMessages.size()]), fp, null);
        for (Message message : largeMessages)
        {

            if (!shouldImportMessage(account, folder, message, progress, earliestDate))
            {
                progress.incrementAndGet();
                continue;
            }

            if (message.getBody() == null)
            {
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
                if (!message.isSet(Flag.X_DOWNLOADED_FULL))
                {
                    /*
                     * Mark the message as fully downloaded if the message size is smaller than
                     * the account's autodownload size limit, otherwise mark as only a partial
                     * download.  This will prevent the system from downloading the same message
                     * twice.
                     */
                    if (message.getSize() < account.getMaximumAutoDownloadMessageSize())
                    {
                        localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);
                    }
                    else
                    {
                        // Set a flag indicating that the message has been partially downloaded and
                        // is ready for view.
                        localMessage.setFlag(Flag.X_DOWNLOADED_PARTIAL, true);
                    }
                }
            }
            else
            {
                /*
                 * We have a structure to deal with, from which
                 * we can pull down the parts we want to actually store.
                 * Build a list of parts we are interested in. Text parts will be downloaded
                 * right now, attachments will be left for later.
                 */

                ArrayList<Part> viewables = new ArrayList<Part>();
                ArrayList<Part> attachments = new ArrayList<Part>();
                MimeUtility.collectParts(message, viewables, attachments);

                /*
                 * Now download the parts we're interested in storing.
                 */
                for (Part part : viewables)
                {
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
            Message localMessage = localFolder.getMessage(message.getUid());
            for (MessagingListener l : getListeners())
            {
                l.synchronizeMailboxAddOrUpdateMessage(account, folder, localMessage);
                l.synchronizeMailboxProgress(account, folder, progress.get(), todo);
                if (!localMessage.isSet(Flag.SEEN))
                {
                    l.synchronizeMailboxNewMessage(account, folder, localMessage);
                }
            }

            // Send a notification of this message
            if (shouldNotifyForMessage(account, message))
            {
                newMessages.incrementAndGet();
                notifyAccount(mApplication, account, message, unreadBeforeStart, newMessages);
            }

        }//for large messsages
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "SYNC: Done fetching large messages for folder " + folder);

    }

    private void refreshLocalMessageFlags(final Account account, final Folder remoteFolder,
                                          final LocalFolder localFolder,
                                          ArrayList<Message> syncFlagMessages,
                                          final AtomicInteger progress,
                                          final int todo
                                         ) throws MessagingException
    {

        final String folder = remoteFolder.getName();
        if (remoteFolder.supportsFetchingFlags())
        {
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "SYNC: About to sync flags for "
                      + syncFlagMessages.size() + " remote messages for folder " + folder);

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.FLAGS);

            List<Message> undeletedMessages = new LinkedList<Message>();
            for (Message message : syncFlagMessages)
            {
                if (!message.isSet(Flag.DELETED))
                {
                    undeletedMessages.add(message);
                }
            }

            remoteFolder.fetch(undeletedMessages.toArray(EMPTY_MESSAGE_ARRAY), fp, null);
            for (Message remoteMessage : syncFlagMessages)
            {
                Message localMessage = localFolder.getMessage(remoteMessage.getUid());
                boolean messageChanged = syncFlags(localMessage, remoteMessage);
                if (messageChanged)
                {
                    if (localMessage.isSet(Flag.DELETED) || isMessageSuppressed(account, folder, localMessage))
                    {
                        for (MessagingListener l : getListeners())
                        {
                            l.synchronizeMailboxRemovedMessage(account, folder, localMessage);
                        }
                    }
                    else
                    {
                        for (MessagingListener l : getListeners())
                        {
                            l.synchronizeMailboxAddOrUpdateMessage(account, folder, localMessage);
                        }
                    }

                }
                progress.incrementAndGet();
                for (MessagingListener l : getListeners())
                {
                    l.synchronizeMailboxProgress(account, folder, progress.get(), todo);
                }
            }
        }
    }

    private boolean syncFlags(Message localMessage, Message remoteMessage) throws MessagingException
    {
        boolean messageChanged = false;
        if (localMessage == null || localMessage.isSet(Flag.DELETED))
        {
            return false;
        }
        if (remoteMessage.isSet(Flag.DELETED))
        {
            if (localMessage.getFolder().getAccount().syncRemoteDeletions())
            {
                localMessage.setFlag(Flag.DELETED, true);
                messageChanged = true;
            }
        }
        else
        {
            for (Flag flag : new Flag[] { Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED })
            {
                if (remoteMessage.isSet(flag) != localMessage.isSet(flag))
                {
                    localMessage.setFlag(flag, remoteMessage.isSet(flag));
                    messageChanged = true;
                }
            }
        }
        return messageChanged;
    }
    private String getRootCauseMessage(Throwable t)
    {
        Throwable rootCause = t;
        Throwable nextCause = rootCause;
        do
        {
            nextCause = rootCause.getCause();
            if (nextCause != null)
            {
                rootCause = nextCause;
            }
        }
        while (nextCause != null);
        return rootCause.getMessage();
    }

    private void queuePendingCommand(Account account, PendingCommand command)
    {
        try
        {
            LocalStore localStore = account.getLocalStore();
            localStore.addPendingCommand(command);
        }
        catch (Exception e)
        {
            addErrorMessage(account, null, e);

            throw new RuntimeException("Unable to enqueue pending command", e);
        }
    }

    private void processPendingCommands(final Account account)
    {
        putBackground("processPendingCommands", null, new Runnable()
        {
            public void run()
            {
                try
                {
                    processPendingCommandsSynchronous(account);
                }
                catch (UnavailableStorageException e)
                {
                    Log.i(K9.LOG_TAG, "Failed to process pending command because storage is not available - trying again later.");
                    throw new UnavailableAccountException(e);
                }
                catch (MessagingException me)
                {
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

    private void processPendingCommandsSynchronous(Account account) throws MessagingException
    {
        LocalStore localStore = account.getLocalStore();
        ArrayList<PendingCommand> commands = localStore.getPendingCommands();

        int progress = 0;
        int todo = commands.size();
        if (todo == 0)
        {
            return;
        }

        for (MessagingListener l : getListeners())
        {
            l.pendingCommandsProcessing(account);
            l.synchronizeMailboxProgress(account, null, progress, todo);
        }

        PendingCommand processingCommand = null;
        try
        {
            for (PendingCommand command : commands)
            {
                processingCommand = command;
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Processing pending command '" + command + "'");

                String[] components = command.command.split("\\.");
                String commandTitle = components[components.length - 1];
                for (MessagingListener l : getListeners())
                {
                    l.pendingCommandStarted(account, commandTitle);
                }
                /*
                 * We specifically do not catch any exceptions here. If a command fails it is
                 * most likely due to a server or IO error and it must be retried before any
                 * other command processes. This maintains the order of the commands.
                 */
                try
                {
                    if (PENDING_COMMAND_APPEND.equals(command.command))
                    {
                        processPendingAppend(command, account);
                    }
                    else if (PENDING_COMMAND_SET_FLAG_BULK.equals(command.command))
                    {
                        processPendingSetFlag(command, account);
                    }
                    else if (PENDING_COMMAND_SET_FLAG.equals(command.command))
                    {
                        processPendingSetFlagOld(command, account);
                    }
                    else if (PENDING_COMMAND_MARK_ALL_AS_READ.equals(command.command))
                    {
                        processPendingMarkAllAsRead(command, account);
                    }
                    else if (PENDING_COMMAND_MOVE_OR_COPY_BULK.equals(command.command))
                    {
                        processPendingMoveOrCopy(command, account);
                    }
                    else if (PENDING_COMMAND_MOVE_OR_COPY.equals(command.command))
                    {
                        processPendingMoveOrCopyOld(command, account);
                    }
                    else if (PENDING_COMMAND_EMPTY_TRASH.equals(command.command))
                    {
                        processPendingEmptyTrash(command, account);
                    }
                    else if (PENDING_COMMAND_EXPUNGE.equals(command.command))
                    {
                        processPendingExpunge(command, account);
                    }
                    localStore.removePendingCommand(command);
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "Done processing pending command '" + command + "'");
                }
                catch (MessagingException me)
                {
                    if (me.isPermanentFailure())
                    {
                        addErrorMessage(account, null, me);
                        Log.e(K9.LOG_TAG, "Failure of command '" + command + "' was permanent, removing command from queue");
                        localStore.removePendingCommand(processingCommand);
                    }
                    else
                    {
                        throw me;
                    }
                }
                finally
                {
                    progress++;
                    for (MessagingListener l : getListeners())
                    {
                        l.synchronizeMailboxProgress(account, null, progress, todo);
                        l.pendingCommandCompleted(account, commandTitle);
                    }
                }
            }
        }
        catch (MessagingException me)
        {
            addErrorMessage(account, null, me);
            Log.e(K9.LOG_TAG, "Could not process command '" + processingCommand + "'", me);
            throw me;
        }
        finally
        {
            for (MessagingListener l : getListeners())
            {
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
    throws MessagingException
    {
        Folder remoteFolder = null;
        LocalFolder localFolder = null;
        try
        {

            String folder = command.arguments[0];
            String uid = command.arguments[1];

            if (account.getErrorFolderName().equals(folder))
            {
                return;
            }

            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folder);
            LocalMessage localMessage = (LocalMessage) localFolder.getMessage(uid);

            if (localMessage == null)
            {
                return;
            }

            Store remoteStore = account.getRemoteStore();
            remoteFolder = remoteStore.getFolder(folder);
            if (!remoteFolder.exists())
            {
                if (!remoteFolder.create(FolderType.HOLDS_MESSAGES))
                {
                    return;
                }
            }
            remoteFolder.open(OpenMode.READ_WRITE);
            if (remoteFolder.getMode() != OpenMode.READ_WRITE)
            {
                return;
            }

            Message remoteMessage = null;
            if (!localMessage.getUid().startsWith(K9.LOCAL_UID_PREFIX))
            {
                remoteMessage = remoteFolder.getMessage(localMessage.getUid());
            }

            if (remoteMessage == null)
            {
                if (localMessage.isSet(Flag.X_REMOTE_COPY_STARTED))
                {
                    Log.w(K9.LOG_TAG, "Local message with uid " + localMessage.getUid() +
                          " has flag " + Flag.X_REMOTE_COPY_STARTED + " already set, checking for remote message with " +
                          " same message id");
                    String rUid = remoteFolder.getUidFromMessageId(localMessage);
                    if (rUid != null)
                    {
                        Log.w(K9.LOG_TAG, "Local message has flag " + Flag.X_REMOTE_COPY_STARTED + " already set, and there is a remote message with " +
                              " uid " + rUid + ", assuming message was already copied and aborting this copy");

                        String oldUid = localMessage.getUid();
                        localMessage.setUid(rUid);
                        localFolder.changeUid(localMessage);
                        for (MessagingListener l : getListeners())
                        {
                            l.messageUidChanged(account, folder, oldUid, localMessage.getUid());
                        }
                        return;
                    }
                    else
                    {
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
                for (MessagingListener l : getListeners())
                {
                    l.messageUidChanged(account, folder, oldUid, localMessage.getUid());
                }
            }
            else
            {
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
                if (remoteDate != null && remoteDate.compareTo(localDate) > 0)
                {
                    /*
                     * If the remote message is newer than ours we'll just
                     * delete ours and move on. A sync will get the server message
                     * if we need to be able to see it.
                     */
                    localMessage.destroy();
                }
                else
                {
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
                    for (MessagingListener l : getListeners())
                    {
                        l.messageUidChanged(account, folder, oldUid, localMessage.getUid());
                    }
                    if (remoteDate != null)
                    {
                        remoteMessage.setFlag(Flag.DELETED, true);
                        if (Account.EXPUNGE_IMMEDIATELY.equals(account.getExpungePolicy()))
                        {
                            remoteFolder.expunge();
                        }
                    }
                }
            }
        }
        finally
        {
            closeFolder(remoteFolder);
            closeFolder(localFolder);
        }
    }
    private void queueMoveOrCopy(Account account, String srcFolder, String destFolder, boolean isCopy, String uids[])
    {
        if (account.getErrorFolderName().equals(srcFolder))
        {
            return;
        }
        PendingCommand command = new PendingCommand();
        command.command = PENDING_COMMAND_MOVE_OR_COPY_BULK;

        int length = 3 + uids.length;
        command.arguments = new String[length];
        command.arguments[0] = srcFolder;
        command.arguments[1] = destFolder;
        command.arguments[2] = Boolean.toString(isCopy);
        for (int i = 0; i < uids.length; i++)
        {
            command.arguments[3 + i] = uids[i];
        }
        queuePendingCommand(account, command);
    }
    /**
     * Process a pending trash message command.
     *
     * @param command arguments = (String folder, String uid)
     * @param account
     * @throws MessagingException
     */
    private void processPendingMoveOrCopy(PendingCommand command, Account account)
    throws MessagingException
    {
        Folder remoteSrcFolder = null;
        Folder remoteDestFolder = null;
        try
        {
            String srcFolder = command.arguments[0];
            if (account.getErrorFolderName().equals(srcFolder))
            {
                return;
            }
            String destFolder = command.arguments[1];
            String isCopyS = command.arguments[2];
            Store remoteStore = account.getRemoteStore();
            remoteSrcFolder = remoteStore.getFolder(srcFolder);

            List<Message> messages = new ArrayList<Message>();
            for (int i = 3; i < command.arguments.length; i++)
            {
                String uid = command.arguments[i];
                if (!uid.startsWith(K9.LOCAL_UID_PREFIX))
                {
                    messages.add(remoteSrcFolder.getMessage(uid));
                }
            }

            boolean isCopy = false;
            if (isCopyS != null)
            {
                isCopy = Boolean.parseBoolean(isCopyS);
            }

            if (!remoteSrcFolder.exists())
            {
                throw new MessagingException("processingPendingMoveOrCopy: remoteFolder " + srcFolder + " does not exist", true);
            }
            remoteSrcFolder.open(OpenMode.READ_WRITE);
            if (remoteSrcFolder.getMode() != OpenMode.READ_WRITE)
            {
                throw new MessagingException("processingPendingMoveOrCopy: could not open remoteSrcFolder " + srcFolder + " read/write", true);
            }

            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "processingPendingMoveOrCopy: source folder = " + srcFolder
                      + ", " + messages.size() + " messages, destination folder = " + destFolder + ", isCopy = " + isCopy);

            if (!isCopy && destFolder.equals(account.getTrashFolderName()))
            {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "processingPendingMoveOrCopy doing special case for deleting message");

                String destFolderName = destFolder;
                if (K9.FOLDER_NONE.equals(destFolderName))
                {
                    destFolderName = null;
                }
                remoteSrcFolder.delete(messages.toArray(EMPTY_MESSAGE_ARRAY), destFolderName);
            }
            else
            {
                remoteDestFolder = remoteStore.getFolder(destFolder);

                if (isCopy)
                {
                    remoteSrcFolder.copyMessages(messages.toArray(EMPTY_MESSAGE_ARRAY), remoteDestFolder);
                }
                else
                {
                    remoteSrcFolder.moveMessages(messages.toArray(EMPTY_MESSAGE_ARRAY), remoteDestFolder);
                }
            }
            if (!isCopy && Account.EXPUNGE_IMMEDIATELY.equals(account.getExpungePolicy()))
            {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "processingPendingMoveOrCopy expunging folder " + account.getDescription() + ":" + srcFolder);

                remoteSrcFolder.expunge();
            }
        }
        finally
        {
            closeFolder(remoteSrcFolder);
            closeFolder(remoteDestFolder);
        }


    }

    private void queueSetFlag(final Account account, final String folderName, final String newState, final String flag, final String[] uids)
    {
        putBackground("queueSetFlag " + account.getDescription() + ":" + folderName, null, new Runnable()
        {
            public void run()
            {
                PendingCommand command = new PendingCommand();
                command.command = PENDING_COMMAND_SET_FLAG_BULK;
                int length = 3 + uids.length;
                command.arguments = new String[length];
                command.arguments[0] = folderName;
                command.arguments[1] = newState;
                command.arguments[2] = flag;
                for (int i = 0; i < uids.length; i++)
                {
                    command.arguments[3 + i] = uids[i];
                }
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
    throws MessagingException
    {
        String folder = command.arguments[0];

        if (account.getErrorFolderName().equals(folder))
        {
            return;
        }

        boolean newState = Boolean.parseBoolean(command.arguments[1]);

        Flag flag = Flag.valueOf(command.arguments[2]);

        Store remoteStore = account.getRemoteStore();
        Folder remoteFolder = remoteStore.getFolder(folder);
        if (!remoteFolder.exists() ||
                /*
                 * Don't proceed if the remote folder doesn't support flags and
                 * the flag to be changed isn't the deleted flag. This avoids
                 * unnecessary connections to POP3 servers.
                 */
                // TODO: This should actually call a supportsSettingFlag(flag) method.
                (!remoteFolder.supportsFetchingFlags() && !Flag.DELETED.equals(flag)))
        {
            return;
        }

        try
        {
            remoteFolder.open(OpenMode.READ_WRITE);
            if (remoteFolder.getMode() != OpenMode.READ_WRITE)
            {
                return;
            }
            List<Message> messages = new ArrayList<Message>();
            for (int i = 3; i < command.arguments.length; i++)
            {
                String uid = command.arguments[i];
                if (!uid.startsWith(K9.LOCAL_UID_PREFIX))
                {
                    messages.add(remoteFolder.getMessage(uid));
                }
            }

            if (messages.size() == 0)
            {
                return;
            }
            remoteFolder.setFlags(messages.toArray(EMPTY_MESSAGE_ARRAY), new Flag[] { flag }, newState);
        }
        finally
        {
            closeFolder(remoteFolder);
        }
    }

    // TODO: This method is obsolete and is only for transition from K-9 2.0 to K-9 2.1
    // Eventually, it should be removed
    private void processPendingSetFlagOld(PendingCommand command, Account account)
    throws MessagingException
    {
        String folder = command.arguments[0];
        String uid = command.arguments[1];

        if (account.getErrorFolderName().equals(folder))
        {
            return;
        }
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "processPendingSetFlagOld: folder = " + folder + ", uid = " + uid);

        boolean newState = Boolean.parseBoolean(command.arguments[2]);

        Flag flag = Flag.valueOf(command.arguments[3]);
        Folder remoteFolder = null;
        try
        {
            Store remoteStore = account.getRemoteStore();
            remoteFolder = remoteStore.getFolder(folder);
            if (!remoteFolder.exists())
            {
                return;
            }
            remoteFolder.open(OpenMode.READ_WRITE);
            if (remoteFolder.getMode() != OpenMode.READ_WRITE)
            {
                return;
            }
            Message remoteMessage = null;
            if (!uid.startsWith(K9.LOCAL_UID_PREFIX))
            {
                remoteMessage = remoteFolder.getMessage(uid);
            }
            if (remoteMessage == null)
            {
                return;
            }
            remoteMessage.setFlag(flag, newState);
        }
        finally
        {
            closeFolder(remoteFolder);
        }
    }
    private void queueExpunge(final Account account, final String folderName)
    {
        putBackground("queueExpunge " + account.getDescription() + ":" + folderName, null, new Runnable()
        {
            public void run()
            {
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
    throws MessagingException
    {
        String folder = command.arguments[0];

        if (account.getErrorFolderName().equals(folder))
        {
            return;
        }
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "processPendingExpunge: folder = " + folder);

        Store remoteStore = account.getRemoteStore();
        Folder remoteFolder = remoteStore.getFolder(folder);
        try
        {
            if (!remoteFolder.exists())
            {
                return;
            }
            remoteFolder.open(OpenMode.READ_WRITE);
            if (remoteFolder.getMode() != OpenMode.READ_WRITE)
            {
                return;
            }
            remoteFolder.expunge();
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "processPendingExpunge: complete for folder = " + folder);
        }
        finally
        {
            closeFolder(remoteFolder);
        }
    }


    // TODO: This method is obsolete and is only for transition from K-9 2.0 to K-9 2.1
    // Eventually, it should be removed
    private void processPendingMoveOrCopyOld(PendingCommand command, Account account)
    throws MessagingException
    {
        String srcFolder = command.arguments[0];
        String uid = command.arguments[1];
        String destFolder = command.arguments[2];
        String isCopyS = command.arguments[3];

        boolean isCopy = false;
        if (isCopyS != null)
        {
            isCopy = Boolean.parseBoolean(isCopyS);
        }

        if (account.getErrorFolderName().equals(srcFolder))
        {
            return;
        }

        Store remoteStore = account.getRemoteStore();
        Folder remoteSrcFolder = remoteStore.getFolder(srcFolder);
        Folder remoteDestFolder = remoteStore.getFolder(destFolder);

        if (!remoteSrcFolder.exists())
        {
            throw new MessagingException("processPendingMoveOrCopyOld: remoteFolder " + srcFolder + " does not exist", true);
        }
        remoteSrcFolder.open(OpenMode.READ_WRITE);
        if (remoteSrcFolder.getMode() != OpenMode.READ_WRITE)
        {
            throw new MessagingException("processPendingMoveOrCopyOld: could not open remoteSrcFolder " + srcFolder + " read/write", true);
        }

        Message remoteMessage = null;
        if (!uid.startsWith(K9.LOCAL_UID_PREFIX))
        {
            remoteMessage = remoteSrcFolder.getMessage(uid);
        }
        if (remoteMessage == null)
        {
            throw new MessagingException("processPendingMoveOrCopyOld: remoteMessage " + uid + " does not exist", true);
        }

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "processPendingMoveOrCopyOld: source folder = " + srcFolder
                  + ", uid = " + uid + ", destination folder = " + destFolder + ", isCopy = " + isCopy);

        if (!isCopy && destFolder.equals(account.getTrashFolderName()))
        {
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "processPendingMoveOrCopyOld doing special case for deleting message");

            remoteMessage.delete(account.getTrashFolderName());
            remoteSrcFolder.close();
            return;
        }

        remoteDestFolder.open(OpenMode.READ_WRITE);
        if (remoteDestFolder.getMode() != OpenMode.READ_WRITE)
        {
            throw new MessagingException("processPendingMoveOrCopyOld: could not open remoteDestFolder " + srcFolder + " read/write", true);
        }

        if (isCopy)
        {
            remoteSrcFolder.copyMessages(new Message[] { remoteMessage }, remoteDestFolder);
        }
        else
        {
            remoteSrcFolder.moveMessages(new Message[] { remoteMessage }, remoteDestFolder);
        }
        remoteSrcFolder.close();
        remoteDestFolder.close();
    }

    private void processPendingMarkAllAsRead(PendingCommand command, Account account) throws MessagingException
    {
        String folder = command.arguments[0];
        Folder remoteFolder = null;
        LocalFolder localFolder = null;
        try
        {
            Store localStore = account.getLocalStore();
            localFolder = (LocalFolder) localStore.getFolder(folder);
            localFolder.open(OpenMode.READ_WRITE);
            Message[] messages = localFolder.getMessages(null, false);
            for (Message message : messages)
            {
                if (!message.isSet(Flag.SEEN))
                {
                    message.setFlag(Flag.SEEN, true);
                    for (MessagingListener l : getListeners())
                    {
                        l.listLocalMessagesUpdateMessage(account, folder, message);
                    }
                }
            }
            localFolder.setUnreadMessageCount(0);
            for (MessagingListener l : getListeners())
            {
                l.folderStatusChanged(account, folder, 0);
            }


            if (account.getErrorFolderName().equals(folder))
            {
                return;
            }

            Store remoteStore = account.getRemoteStore();
            remoteFolder = remoteStore.getFolder(folder);

            if (!remoteFolder.exists())
            {
                return;
            }
            remoteFolder.open(OpenMode.READ_WRITE);
            if (remoteFolder.getMode() != OpenMode.READ_WRITE)
            {
                return;
            }

            remoteFolder.setFlags(new Flag[] {Flag.SEEN}, true);
            remoteFolder.close();
        }
        catch (UnsupportedOperationException uoe)
        {
            Log.w(K9.LOG_TAG, "Could not mark all server-side as read because store doesn't support operation", uoe);
        }
        finally
        {
            closeFolder(localFolder);
            closeFolder(remoteFolder);
        }
    }

    static long uidfill = 0;
    static AtomicBoolean loopCatch = new AtomicBoolean();
    public void addErrorMessage(Account account, String subject, Throwable t)
    {
        if (!loopCatch.compareAndSet(false, true))
        {
            return;
        }
        try
        {
            if (t == null)
            {
                return;
            }

            CharArrayWriter baos = new CharArrayWriter(t.getStackTrace().length * 10);
            PrintWriter ps = new PrintWriter(baos);
            t.printStackTrace(ps);
            ps.close();

            if (subject == null)
            {
                subject = getRootCauseMessage(t);
            }

            addErrorMessage(account, subject, baos.toString());
        }
        catch (Throwable it)
        {
            Log.e(K9.LOG_TAG, "Could not save error message to " + account.getErrorFolderName(), it);
        }
        finally
        {
            loopCatch.set(false);
        }
    }

    public void addErrorMessage(Account account, String subject, String body)
    {
        if (!K9.ENABLE_ERROR_FOLDER)
        {
            return;
        }
        if (!loopCatch.compareAndSet(false, true))
        {
            return;
        }
        try
        {
            if (body == null || body.length() < 1)
            {
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

        }
        catch (Throwable it)
        {
            Log.e(K9.LOG_TAG, "Could not save error message to " + account.getErrorFolderName(), it);
        }
        finally
        {
            loopCatch.set(false);
        }
    }



    public void markAllMessagesRead(final Account account, final String folder)
    {

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

    public void setFlag(
        final Message[] messages,
        final Flag flag,
        final boolean newState)
    {
        actOnMessages(messages, new MessageActor()
        {
            @Override
            public void act(final Account account, final Folder folder,
                            final List<Message> messages)
            {
                String[] uids = new String[messages.size()];
                for (int i = 0; i < messages.size(); i++)
                {
                    uids[i] = messages.get(i).getUid();
                }
                setFlag(account, folder.getName(), uids, flag, newState);
            }

        });

    }

    public void setFlag(
        final Account account,
        final String folderName,
        final String[] uids,
        final Flag flag,
        final boolean newState)
    {
        // TODO: put this into the background, but right now that causes odd behavior
        // because the FolderMessageList doesn't have its own cache of the flag states
        Folder localFolder = null;
        try
        {
            Store localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folderName);
            localFolder.open(OpenMode.READ_WRITE);
            ArrayList<Message> messages = new ArrayList<Message>();
            for (String uid : uids)
            {
                // Allows for re-allowing sending of messages that could not be sent
                if (flag == Flag.FLAGGED && !newState
                        && uid != null
                        && account.getOutboxFolderName().equals(folderName))
                {
                    sendCount.remove(uid);
                }
                Message msg = localFolder.getMessage(uid);
                if (msg != null)
                {
                    messages.add(msg);
                }
            }

            localFolder.setFlags(messages.toArray(EMPTY_MESSAGE_ARRAY), new Flag[] {flag}, newState);


            for (MessagingListener l : getListeners())
            {
                l.folderStatusChanged(account, folderName, localFolder.getUnreadMessageCount());
            }

            if (account.getErrorFolderName().equals(folderName))
            {
                return;
            }

            queueSetFlag(account, folderName, Boolean.toString(newState), flag.toString(), uids);
            processPendingCommands(account);
        }
        catch (MessagingException me)
        {
            addErrorMessage(account, null, me);

            throw new RuntimeException(me);
        }
        finally
        {
            closeFolder(localFolder);
        }
    }//setMesssageFlag

    public void clearAllPending(final Account account)
    {
        try
        {
            Log.w(K9.LOG_TAG, "Clearing pending commands!");
            LocalStore localStore = account.getLocalStore();
            localStore.removePendingCommands();
        }
        catch (MessagingException me)
        {
            Log.e(K9.LOG_TAG, "Unable to clear pending command", me);
            addErrorMessage(account, null, me);
        }
    }

    public void loadMessageForViewRemote(final Account account, final String folder,
                                         final String uid, final MessagingListener listener)
    {
        put("loadMessageForViewRemote", listener, new Runnable()
        {
            public void run()
            {
                Folder remoteFolder = null;
                LocalFolder localFolder = null;
                try
                {
                    LocalStore localStore = account.getLocalStore();
                    localFolder = localStore.getFolder(folder);
                    localFolder.open(OpenMode.READ_WRITE);

                    Message message = localFolder.getMessage(uid);

                    if (message.isSet(Flag.X_DOWNLOADED_FULL))
                    {
                        /*
                         * If the message has been synchronized since we were called we'll
                         * just hand it back cause it's ready to go.
                         */
                        FetchProfile fp = new FetchProfile();
                        fp.add(FetchProfile.Item.ENVELOPE);
                        fp.add(FetchProfile.Item.BODY);
                        localFolder.fetch(new Message[] { message }, fp, null);
                    }
                    else
                    {
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
                        fp.add(FetchProfile.Item.ENVELOPE);
                        message = localFolder.getMessage(uid);
                        localFolder.fetch(new Message[] { message }, fp, null);

                        // Mark that this message is now fully synched
                        message.setFlag(Flag.X_DOWNLOADED_FULL, true);
                    }

                    // now that we have the full message, refresh the headers
                    for (MessagingListener l : getListeners(listener))
                    {
                        l.loadMessageForViewHeadersAvailable(account, folder, uid, message);
                    }

                    for (MessagingListener l : getListeners(listener))
                    {
                        l.loadMessageForViewBodyAvailable(account, folder, uid, message);
                    }
                    for (MessagingListener l : getListeners(listener))
                    {
                        l.loadMessageForViewFinished(account, folder, uid, message);
                    }
                }
                catch (Exception e)
                {
                    for (MessagingListener l : getListeners(listener))
                    {
                        l.loadMessageForViewFailed(account, folder, uid, e);
                    }
                    addErrorMessage(account, null, e);

                }
                finally
                {
                    closeFolder(remoteFolder);
                    closeFolder(localFolder);
                }
            }//run
        });
    }

    public void loadMessageForView(final Account account, final String folder, final String uid,
                                   final MessagingListener listener)
    {
        for (MessagingListener l : getListeners(listener))
        {
            l.loadMessageForViewStarted(account, folder, uid);
        }
        threadPool.execute(new Runnable()
        {
            public void run()
            {

                try
                {
                    LocalStore localStore = account.getLocalStore();
                    LocalFolder localFolder = localStore.getFolder(folder);
                    localFolder.open(OpenMode.READ_WRITE);

                    LocalMessage message = (LocalMessage)localFolder.getMessage(uid);
                    if (message==null
                            || message.getId()==0)
                    {
                        throw new IllegalArgumentException("Message not found: folder=" + folder + ", uid=" + uid);
                    }
                    if (!message.isSet(Flag.SEEN))
                    {
                        message.setFlag(Flag.SEEN, true);
                        setFlag(new Message[] { message }, Flag.SEEN, true);
                    }

                    for (MessagingListener l : getListeners(listener))
                    {
                        l.loadMessageForViewHeadersAvailable(account, folder, uid, message);
                    }

                    FetchProfile fp = new FetchProfile();
                    fp.add(FetchProfile.Item.ENVELOPE);
                    fp.add(FetchProfile.Item.BODY);
                    localFolder.fetch(new Message[]
                                      {
                                          message
                                      }, fp, null);
                    localFolder.close();

                    for (MessagingListener l : getListeners(listener))
                    {
                        l.loadMessageForViewBodyAvailable(account, folder, uid, message);
                    }

                    for (MessagingListener l : getListeners(listener))
                    {
                        l.loadMessageForViewFinished(account, folder, uid, message);
                    }

                }
                catch (Exception e)
                {
                    for (MessagingListener l : getListeners(listener))
                    {
                        l.loadMessageForViewFailed(account, folder, uid, e);
                    }
                    addErrorMessage(account, null, e);

                }
            }
        });
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
        final MessagingListener listener)
    {
        /*
         * Check if the attachment has already been downloaded. If it has there's no reason to
         * download it, so we just tell the listener that it's ready to go.
         */

            if (part.getBody() != null)
            {
                for (MessagingListener l : getListeners())
                {
                    l.loadAttachmentStarted(account, message, part, tag, false);
                }
                if (listener != null)
                {
                    listener.loadAttachmentStarted(account, message, part, tag, false);
                }

                for (MessagingListener l : getListeners())
                {
                    l.loadAttachmentFinished(account, message, part, tag);
                }

                if (listener != null)
                {
                    listener.loadAttachmentFinished(account, message, part, tag);
                }
                return;
            }



        for (MessagingListener l : getListeners())
        {
            l.loadAttachmentStarted(account, message, part, tag, true);
        }
        if (listener != null)
        {
            listener.loadAttachmentStarted(account, message, part, tag, false);
        }

        put("loadAttachment", listener, new Runnable()
        {
            public void run()
            {
                Folder remoteFolder = null;
                LocalFolder localFolder = null;
                try
                {
                    LocalStore localStore = account.getLocalStore();

                    /*
                     * We clear out any attachments already cached in the entire store and then
                     * we update the passed in message to reflect that there are no cached
                     * attachments. This is in support of limiting the account to having one
                     * attachment downloaded at a time.
                     */
                    localStore.pruneCachedAttachments();
                    ArrayList<Part> viewables = new ArrayList<Part>();
                    ArrayList<Part> attachments = new ArrayList<Part>();
                    MimeUtility.collectParts(message, viewables, attachments);
                    for (Part attachment : attachments)
                    {
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
                    for (MessagingListener l : getListeners())
                    {
                        l.loadAttachmentFinished(account, message, part, tag);
                    }
                    if (listener != null)
                    {
                        listener.loadAttachmentFinished(account, message, part, tag);
                    }
                }
                catch (MessagingException me)
                {
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Exception loading attachment", me);

                    for (MessagingListener l : getListeners())
                    {
                        l.loadAttachmentFailed(account, message, part, tag, me.getMessage());
                    }
                    if (listener != null)
                    {
                        listener.loadAttachmentFailed(account, message, part, tag, me.getMessage());
                    }
                    addErrorMessage(account, null, me);

                }
                finally
                {
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
                            MessagingListener listener)
    {
        try
        {
            LocalStore localStore = account.getLocalStore();
            LocalFolder localFolder = localStore.getFolder(account.getOutboxFolderName());
            localFolder.open(OpenMode.READ_WRITE);
            localFolder.appendMessages(new Message[] { message });
            Message localMessage = localFolder.getMessage(message.getUid());
            localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);
            localFolder.close();
            sendPendingMessages(account, listener);
        }
        catch (Exception e)
        {
            /*
            for (MessagingListener l : getListeners())
            {
                // TODO general failed
            }
            */
            addErrorMessage(account, null, e);

        }
    }


    public void sendPendingMessages(MessagingListener listener)
    {
        final Preferences prefs = Preferences.getPreferences(mApplication.getApplicationContext());
        Account[] accounts = prefs.getAccounts();
        for (Account account : accounts)
        {
            sendPendingMessages(account, listener);
        }
    }


    /**
     * Attempt to send any messages that are sitting in the Outbox.
     * @param account
     * @param listener
     */
    public void sendPendingMessages(final Account account,
                                    MessagingListener listener)
    {
        putBackground("sendPendingMessages", listener, new Runnable()
        {
            public void run()
            {
                if (!account.isAvailable(mApplication))
                {
                    throw new UnavailableAccountException();
                }
                if (messagesPendingSend(account))
                {


                    notifyWhileSending(account);

                    try
                    {
                        sendPendingMessagesSynchronous(account);
                    }
                    finally
                    {
                        notifyWhileSendingDone(account);
                    }
                }
            }
        });
    }

    private void cancelNotification(int id)
    {
        NotificationManager notifMgr =
            (NotificationManager)mApplication.getSystemService(Context.NOTIFICATION_SERVICE);
        notifMgr.cancel(id);
    }

    private void notifyWhileSendingDone(Account account)
    {
        if (account.isShowOngoing())
        {
            cancelNotification(K9.FETCHING_EMAIL_NOTIFICATION - account.getAccountNumber());


        }
    }
    private void notifyWhileSending(Account account)
    {
        if (!account.isShowOngoing())
        {
            return;
        }
        NotificationManager notifMgr =
            (NotificationManager)mApplication.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notif = new Notification(R.drawable.ic_menu_refresh,
                                              mApplication.getString(R.string.notification_bg_send_ticker, account.getDescription()), System.currentTimeMillis());
        Intent intent = MessageList.actionHandleFolderIntent(mApplication, account, K9.INBOX);
        PendingIntent pi = PendingIntent.getActivity(mApplication, 0, intent, 0);
        notif.setLatestEventInfo(mApplication, mApplication.getString(R.string.notification_bg_send_title),
                                 account.getDescription() , pi);
        notif.flags = Notification.FLAG_ONGOING_EVENT;

        if (K9.NOTIFICATION_LED_WHILE_SYNCING)
        {
            configureNotification(notif,  null, null, account.getNotificationSetting().getLedColor(), K9.NOTIFICATION_LED_BLINK_FAST, true);
        }

        notifMgr.notify(K9.FETCHING_EMAIL_NOTIFICATION - account.getAccountNumber(), notif);
    }

    private void notifySendFailed(Account account, Exception lastFailure)
    {
        NotificationManager notifMgr = (NotificationManager)mApplication.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notif = new Notification(R.drawable.stat_notify_email_generic, mApplication.getString(R.string.send_failure_subject), System.currentTimeMillis());

        Intent i = FolderList.actionHandleNotification(mApplication, account, account.getOutboxFolderName());

        PendingIntent pi = PendingIntent.getActivity(mApplication, 0, i, 0);

        notif.setLatestEventInfo(mApplication, mApplication.getString(R.string.send_failure_subject), lastFailure.getMessage(), pi);

        configureNotification(notif,  null, null, K9.NOTIFICATION_LED_SENDING_FAILURE_COLOR, K9.NOTIFICATION_LED_BLINK_FAST, true);
        notif.flags |= Notification.FLAG_AUTO_CANCEL;
        notifMgr.notify(-1500 - account.getAccountNumber(), notif);
    }


    private void notifyFetchingMail(final Account account, final Folder folder)
    {
        if (account.isShowOngoing())
        {
            final NotificationManager notifMgr = (NotificationManager)mApplication
                                                 .getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notif = new Notification(R.drawable.ic_menu_refresh,
                                                  mApplication.getString(R.string.notification_bg_sync_ticker, account.getDescription(), folder.getName()),
                                                  System.currentTimeMillis());
            Intent intent = MessageList.actionHandleFolderIntent(mApplication, account, K9.INBOX);
            PendingIntent pi = PendingIntent.getActivity(mApplication, 0, intent, 0);
            notif.setLatestEventInfo(mApplication, mApplication.getString(R.string.notification_bg_sync_title), account.getDescription()
                                     + mApplication.getString(R.string.notification_bg_title_separator) + folder.getName(), pi);
            notif.flags = Notification.FLAG_ONGOING_EVENT;

            if (K9.NOTIFICATION_LED_WHILE_SYNCING)
            {
                configureNotification(notif,  null, null, account.getNotificationSetting().getLedColor(), K9.NOTIFICATION_LED_BLINK_FAST, true);
            }

            notifMgr.notify(K9.FETCHING_EMAIL_NOTIFICATION - account.getAccountNumber(), notif);
        }
    }
    private void notifyFetchingMailCancel (final Account account)
    {
        if (account.isShowOngoing())
        {
            cancelNotification(K9.FETCHING_EMAIL_NOTIFICATION - account.getAccountNumber());
        }
    }

    public boolean messagesPendingSend(final Account account)
    {
        Folder localFolder = null;
        try
        {
            localFolder = account.getLocalStore().getFolder(
                              account.getOutboxFolderName());
            if (!localFolder.exists())
            {
                return false;
            }

            localFolder.open(OpenMode.READ_WRITE);

            if (localFolder.getMessageCount() > 0)
            {
                return true;
            }
        }
        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "Exception while checking for unsent messages", e);
        }
        finally
        {
            closeFolder(localFolder);
        }
        return false;
    }

    /**
     * Attempt to send any messages that are sitting in the Outbox.
     * @param account
     * @param listener
     */
    public void sendPendingMessagesSynchronous(final Account account)
    {
        Folder localFolder = null;
        try
        {
            Store localStore = account.getLocalStore();
            localFolder = localStore.getFolder(
                              account.getOutboxFolderName());
            if (!localFolder.exists())
            {
                return;
            }
            for (MessagingListener l : getListeners())
            {
                l.sendPendingMessagesStarted(account);
            }
            localFolder.open(OpenMode.READ_WRITE);

            Message[] localMessages = localFolder.getMessages(null);
            Exception lastFailure = null;
            int progress = 0;
            int todo = localMessages.length;
            for (MessagingListener l : getListeners())
            {
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
            for (Message message : localMessages)
            {
                if (message.isSet(Flag.DELETED))
                {
                    message.destroy();
                    continue;
                }
                try
                {
                    AtomicInteger count = new AtomicInteger(0);
                    AtomicInteger oldCount = sendCount.putIfAbsent(message.getUid(), count);
                    if (oldCount != null)
                    {
                        count = oldCount;
                    }

                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Send count for message " + message.getUid() + " is " + count.get());

                    localFolder.fetch(new Message[] { message }, fp, null);
                    try
                    {
                        message.setFlag(Flag.X_SEND_IN_PROGRESS, true);
                        if (K9.DEBUG)
                            Log.i(K9.LOG_TAG, "Sending message with UID " + message.getUid());
                        transport.sendMessage(message);
                        message.setFlag(Flag.X_SEND_IN_PROGRESS, false);
                        message.setFlag(Flag.SEEN, true);
                        progress++;
                        for (MessagingListener l : getListeners())
                        {
                            l.synchronizeMailboxProgress(account, account.getSentFolderName(), progress, todo);
                        }
                        if (K9.FOLDER_NONE.equals(account.getSentFolderName()))
                        {
                            if (K9.DEBUG)
                                Log.i(K9.LOG_TAG, "Sent folder set to " + K9.FOLDER_NONE + ", deleting sent message");
                            message.setFlag(Flag.DELETED, true);
                        }
                        else
                        {
                            LocalFolder localSentFolder = (LocalFolder) localStore.getFolder( account.getSentFolderName());
                            if (K9.DEBUG)
                                Log.i(K9.LOG_TAG, "Moving sent message to folder '" + account.getSentFolderName() + "' (" + localSentFolder.getId() + ") ");

                            localFolder.moveMessages( new Message[] { message }, localSentFolder);

                            if (K9.DEBUG)
                                Log.i(K9.LOG_TAG, "Moved sent message to folder '" + account.getSentFolderName() + "' (" + localSentFolder.getId() + ") ");

                            PendingCommand command = new PendingCommand();
                            command.command = PENDING_COMMAND_APPEND;
                            command.arguments = new String[] { localSentFolder.getName(), message.getUid() };
                            queuePendingCommand(account, command);
                            processPendingCommands(account);
                        }

                    }
                    catch (Exception e)
                    {
                        message.setFlag(Flag.X_SEND_FAILED, true);
                        Log.e(K9.LOG_TAG, "Failed to send message", e);
                        for (MessagingListener l : getListeners())
                        {
                            l.synchronizeMailboxFailed( account, localFolder.getName(), getRootCauseMessage(e));
                        }
                        lastFailure = e;
                    }
                }
                catch (Exception e)
                {
                    Log.e(K9.LOG_TAG, "Failed to fetch message for sending", e);
                    for (MessagingListener l : getListeners())
                    {
                        l.synchronizeMailboxFailed( account, localFolder.getName(), getRootCauseMessage(e));
                    }
                    lastFailure = e;
                }
            }
            if (localFolder.getMessageCount() == 0)
            {
                // No longer delete the empty local outbox every time we finish sending mail
                // There's no real win to it and it makes the folder selection UI extra stupid
                // (We'd need a textentry widget to set the Outbox folder rather than a folder select widget)
                // localFolder.delete(false);
            }
            for (MessagingListener l : getListeners())
            {
                l.sendPendingMessagesCompleted(account);
            }
            if (lastFailure != null)
            {
                notifySendFailed(account, lastFailure);
            }
        }
        catch (UnavailableStorageException e)
        {
            Log.i(K9.LOG_TAG, "Failed to send pending messages because storage is not available - trying again later.");
            throw new UnavailableAccountException(e);
        }
        catch (Exception e)
        {
            for (MessagingListener l : getListeners())
            {
                l.sendPendingMessagesFailed(account);
            }
            addErrorMessage(account, null, e);

        }
        finally
        {
            closeFolder(localFolder);
        }
    }

    public void getAccountStats(final Context context, final Account account,
                                final MessagingListener l)
    {
        Runnable unreadRunnable = new Runnable()
        {
            public void run()
            {
                try
                {
                    AccountStats stats = account.getStats(context);
                    l.accountStatusChanged(account, stats);
                }
                catch (MessagingException me)
                {
                    Log.e(K9.LOG_TAG, "Count not get unread count for account " + account.getDescription(),
                          me);
                }

            }
        };

        put("getAccountStats:" + account.getDescription(), l, unreadRunnable);
    }

    public void getFolderUnreadMessageCount(final Account account, final String folderName,
                                            final MessagingListener l)
    {
        Runnable unreadRunnable = new Runnable()
        {
            public void run()
            {

                int unreadMessageCount = 0;
                try
                {
                    Folder localFolder = account.getLocalStore().getFolder(folderName);
                    unreadMessageCount = localFolder.getUnreadMessageCount();
                }
                catch (MessagingException me)
                {
                    Log.e(K9.LOG_TAG, "Count not get unread count for account " + account.getDescription(), me);
                }
                l.folderStatusChanged(account, folderName, unreadMessageCount);
            }
        };


        put("getFolderUnread:" + account.getDescription() + ":" + folderName, l, unreadRunnable);
    }



    public boolean isMoveCapable(Message message)
    {
        return !message.getUid().startsWith(K9.LOCAL_UID_PREFIX);
    }
    public boolean isCopyCapable(Message message)
    {
        return isMoveCapable(message);
    }

    public boolean isMoveCapable(final Account account)
    {
        try
        {
            Store localStore = account.getLocalStore();
            Store remoteStore = account.getRemoteStore();
            return localStore.isMoveCapable() && remoteStore.isMoveCapable();
        }
        catch (MessagingException me)
        {

            Log.e(K9.LOG_TAG, "Exception while ascertaining move capability", me);
            return false;
        }
    }
    public boolean isCopyCapable(final Account account)
    {
        try
        {
            Store localStore = account.getLocalStore();
            Store remoteStore = account.getRemoteStore();
            return localStore.isCopyCapable() && remoteStore.isCopyCapable();
        }
        catch (MessagingException me)
        {
            Log.e(K9.LOG_TAG, "Exception while ascertaining copy capability", me);
            return false;
        }
    }
    public void moveMessages(final Account account, final String srcFolder, final Message[] messages, final String destFolder,
                             final MessagingListener listener)
    {
        for (Message message : messages)
        {
            suppressMessage(account, srcFolder, message);
        }
        putBackground("moveMessages", null, new Runnable()
        {
            public void run()
            {
                moveOrCopyMessageSynchronous(account, srcFolder, messages, destFolder, false, listener);
            }
        });
    }

    public void moveMessage(final Account account, final String srcFolder, final Message message, final String destFolder,
                            final MessagingListener listener)
    {
        moveMessages(account, srcFolder, new Message[] { message }, destFolder, listener);
    }

    public void copyMessages(final Account account, final String srcFolder, final Message[] messages, final String destFolder,
                             final MessagingListener listener)
    {
        putBackground("copyMessages", null, new Runnable()
        {
            public void run()
            {
                moveOrCopyMessageSynchronous(account, srcFolder, messages, destFolder, true, listener);
            }
        });
    }
    public void copyMessage(final Account account, final String srcFolder, final Message message, final String destFolder,
                            final MessagingListener listener)
    {
        copyMessages(account, srcFolder, new Message[] { message }, destFolder, listener);
    }

    private void moveOrCopyMessageSynchronous(final Account account, final String srcFolder, final Message[] inMessages,
            final String destFolder, final boolean isCopy, MessagingListener listener)
    {
        try
        {
            Store localStore = account.getLocalStore();
            Store remoteStore = account.getRemoteStore();
            if (!isCopy && (!remoteStore.isMoveCapable() || !localStore.isMoveCapable()))
            {
                return;
            }
            if (isCopy && (!remoteStore.isCopyCapable() || !localStore.isCopyCapable()))
            {
                return;
            }

            Folder localSrcFolder = localStore.getFolder(srcFolder);
            Folder localDestFolder = localStore.getFolder(destFolder);

            List<String> uids = new LinkedList<String>();
            for (Message message : inMessages)
            {
                String uid = message.getUid();
                if (!uid.startsWith(K9.LOCAL_UID_PREFIX))
                {
                    uids.add(uid);
                }
            }

            Message[] messages = localSrcFolder.getMessages(uids.toArray(EMPTY_STRING_ARRAY), null);
            if (messages.length > 0)
            {
                Map<String, Message> origUidMap = new HashMap<String, Message>();

                for (Message message : messages)
                {
                    origUidMap.put(message.getUid(), message);
                }

                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "moveOrCopyMessageSynchronous: source folder = " + srcFolder
                          + ", " + messages.length + " messages, " + ", destination folder = " + destFolder + ", isCopy = " + isCopy);

                if (isCopy)
                {
                    FetchProfile fp = new FetchProfile();
                    fp.add(FetchProfile.Item.ENVELOPE);
                    fp.add(FetchProfile.Item.BODY);
                    localSrcFolder.fetch(messages, fp, null);
                    localSrcFolder.copyMessages(messages, localDestFolder);
                }
                else
                {
                    localSrcFolder.moveMessages(messages, localDestFolder);
                    for (String origUid : origUidMap.keySet())
                    {
                        for (MessagingListener l : getListeners())
                        {
                            l.messageUidChanged(account, srcFolder, origUid, origUidMap.get(origUid).getUid());
                        }
                        unsuppressMessage(account, srcFolder, origUid);
                    }
                }

                queueMoveOrCopy(account, srcFolder, destFolder, isCopy, origUidMap.keySet().toArray(EMPTY_STRING_ARRAY));
            }

            processPendingCommands(account);
        }
        catch (UnavailableStorageException e)
        {
            Log.i(K9.LOG_TAG, "Failed to move/copy message because storage is not available - trying again later.");
            throw new UnavailableAccountException(e);
        }
        catch (MessagingException me)
        {
            addErrorMessage(account, null, me);

            throw new RuntimeException("Error moving message", me);
        }
    }

    public void expunge(final Account account, final String folder, final MessagingListener listener)
    {
        putBackground("expunge", null, new Runnable()
        {
            public void run()
            {
                queueExpunge(account, folder);
            }
        });
    }

    public void deleteDraft(final Account account, String uid)
    {
        LocalFolder localFolder = null;
        try
        {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(account.getDraftsFolderName());
            localFolder.open(OpenMode.READ_WRITE);
            Message message = localFolder.getMessage(uid);
            if (message != null)
            {
                deleteMessages(new Message[] { message }, null);
            }
        }
        catch (MessagingException me)
        {
            addErrorMessage(account, null, me);
        }
        finally
        {
            closeFolder(localFolder);
        }
    }

    public void deleteMessages(final Message[] messages, final MessagingListener listener)
    {
        actOnMessages(messages, new MessageActor()
        {

            @Override
            public void act(final Account account, final Folder folder,
                            final List<Message> messages)
            {
                for (Message message : messages)
                {
                    suppressMessage(account, folder.getName(), message);
                }

                putBackground("deleteMessages", null, new Runnable()
                {
                    public void run()
                    {
                        deleteMessagesSynchronous(account, folder.getName(), messages.toArray(EMPTY_MESSAGE_ARRAY), listener);
                    }
                });
            }

        });

    }

    private void deleteMessagesSynchronous(final Account account, final String folder, final Message[] messages,
                                           MessagingListener listener)
    {
        Folder localFolder = null;
        Folder localTrashFolder = null;
        String[] uids = getUidsFromMessages(messages);
        try
        {
            //We need to make these callbacks before moving the messages to the trash
            //as messages get a new UID after being moved
            for (Message message : messages)
            {
                if (listener != null)
                {
                    listener.messageDeleted(account, folder, message);
                }
                for (MessagingListener l : getListeners())
                {
                    l.messageDeleted(account, folder, message);
                }
            }
            Store localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folder);
            if (folder.equals(account.getTrashFolderName()) || K9.FOLDER_NONE.equals(account.getTrashFolderName()))
            {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Deleting messages in trash folder or trash set to -None-, not copying");

                localFolder.setFlags(messages, new Flag[] { Flag.DELETED }, true);
            }
            else
            {
                localTrashFolder = localStore.getFolder(account.getTrashFolderName());
                if (!localTrashFolder.exists())
                {
                    localTrashFolder.create(Folder.FolderType.HOLDS_MESSAGES);
                }
                if (localTrashFolder.exists())
                {
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "Deleting messages in normal folder, moving");

                    localFolder.moveMessages(messages, localTrashFolder);

                }
            }

            for (MessagingListener l : getListeners())
            {
                l.folderStatusChanged(account, folder, localFolder.getUnreadMessageCount());
                if (localTrashFolder != null)
                {
                    l.folderStatusChanged(account, account.getTrashFolderName(), localTrashFolder.getUnreadMessageCount());
                }
            }

            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "Delete policy for account " + account.getDescription() + " is " + account.getDeletePolicy());

            if (folder.equals(account.getOutboxFolderName()))
            {
                for (Message message : messages)
                {
                    // If the message was in the Outbox, then it has been copied to local Trash, and has
                    // to be copied to remote trash
                    PendingCommand command = new PendingCommand();
                    command.command = PENDING_COMMAND_APPEND;
                    command.arguments =
                        new String[]
                    {
                        account.getTrashFolderName(),
                        message.getUid()
                    };
                    queuePendingCommand(account, command);
                }
                processPendingCommands(account);
            }
            else if ( account.getDeletePolicy() == Account.DELETE_POLICY_ON_DELETE)
            {
                if (folder.equals(account.getTrashFolderName()))
                {
                    queueSetFlag(account, folder, Boolean.toString(true), Flag.DELETED.toString(), uids);
                }
                else
                {
                    queueMoveOrCopy(account, folder, account.getTrashFolderName(), false, uids);
                }
                processPendingCommands(account);
            }
            else if (account.getDeletePolicy() == Account.DELETE_POLICY_MARK_AS_READ)
            {
                queueSetFlag(account, folder, Boolean.toString(true), Flag.SEEN.toString(), uids);
                processPendingCommands(account);
            }
            else
            {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Delete policy " + account.getDeletePolicy() + " prevents delete from server");
            }
            for (String uid : uids)
            {
                unsuppressMessage(account, folder, uid);
            }
        }
        catch (UnavailableStorageException e)
        {
            Log.i(K9.LOG_TAG, "Failed to delete message because storage is not available - trying again later.");
            throw new UnavailableAccountException(e);
        }
        catch (MessagingException me)
        {
            addErrorMessage(account, null, me);

            throw new RuntimeException("Error deleting message from local store.", me);
        }
        finally
        {
            closeFolder(localFolder);
            closeFolder(localTrashFolder);
        }
    }

    private String[] getUidsFromMessages(Message[] messages)
    {
        String[] uids = new String[messages.length];
        for (int i = 0; i < messages.length; i++)
        {
            uids[i] = messages[i].getUid();
        }
        return uids;
    }

    private void processPendingEmptyTrash(PendingCommand command, Account account) throws MessagingException
    {
        Store remoteStore = account.getRemoteStore();

        Folder remoteFolder = remoteStore.getFolder(account.getTrashFolderName());
        try
        {
            if (remoteFolder.exists())
            {
                remoteFolder.open(OpenMode.READ_WRITE);
                remoteFolder.setFlags(new Flag [] { Flag.DELETED }, true);
                if (Account.EXPUNGE_IMMEDIATELY.equals(account.getExpungePolicy()))
                {
                    remoteFolder.expunge();
                }
            }
        }
        finally
        {
            closeFolder(remoteFolder);
        }
    }

    public void emptyTrash(final Account account, MessagingListener listener)
    {
        putBackground("emptyTrash", listener, new Runnable()
        {
            public void run()
            {
                Folder localFolder = null;
                try
                {
                    Store localStore = account.getLocalStore();
                    localFolder = localStore.getFolder(account.getTrashFolderName());
                    localFolder.open(OpenMode.READ_WRITE);
                    localFolder.setFlags(new Flag[] { Flag.DELETED }, true);

                    for (MessagingListener l : getListeners())
                    {
                        l.emptyTrashCompleted(account);
                    }
                    List<String> args = new ArrayList<String>();
                    PendingCommand command = new PendingCommand();
                    command.command = PENDING_COMMAND_EMPTY_TRASH;
                    command.arguments = args.toArray(EMPTY_STRING_ARRAY);
                    queuePendingCommand(account, command);
                    processPendingCommands(account);
                }
                catch (UnavailableStorageException e)
                {
                    Log.i(K9.LOG_TAG, "Failed to empty trash because storage is not available - trying again later.");
                    throw new UnavailableAccountException(e);
                }
                catch (Exception e)
                {
                    Log.e(K9.LOG_TAG, "emptyTrash failed", e);

                    addErrorMessage(account, null, e);
                }
                finally
                {
                    closeFolder(localFolder);
                }
            }
        });
    }

    public void sendAlternate(final Context context, Account account, Message message)
    {
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "About to load message " + account.getDescription() + ":" + message.getFolder().getName()
                  + ":" + message.getUid() + " for sendAlternate");

        loadMessageForView(account, message.getFolder().getName(),
                           message.getUid(), new MessagingListener()
        {
            @Override
            public void loadMessageForViewBodyAvailable(Account account, String folder, String uid,
                    Message message)
            {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Got message " + account.getDescription() + ":" + folder
                          + ":" + message.getUid() + " for sendAlternate");

                try
                {
                    Intent msg=new Intent(Intent.ACTION_SEND);
                    String quotedText = null;
                    Part part = MimeUtility.findFirstPartByMimeType(message,
                                "text/plain");
                    if (part == null)
                    {
                        part = MimeUtility.findFirstPartByMimeType(message, "text/html");
                    }
                    if (part != null)
                    {
                        quotedText = MimeUtility.getTextFromPart(part);
                    }
                    if (quotedText != null)
                    {
                        msg.putExtra(Intent.EXTRA_TEXT, quotedText);
                    }
                    msg.putExtra(Intent.EXTRA_SUBJECT, "Fwd: " + message.getSubject());
                    msg.setType("text/plain");
                    context.startActivity(Intent.createChooser(msg, context.getString(R.string.send_alternate_chooser_title)));
                }
                catch (MessagingException me)
                {
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
                          final MessagingListener listener)
    {

        TracingWakeLock twakeLock = null;
        if (useManualWakeLock)
        {
            TracingPowerManager pm = TracingPowerManager.getPowerManager(context);

            twakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "K9 MessagingController.checkMail");
            twakeLock.setReferenceCounted(false);
            twakeLock.acquire(K9.MANUAL_WAKE_LOCK_TIMEOUT);
        }
        final TracingWakeLock wakeLock = twakeLock;

        for (MessagingListener l : getListeners())
        {
            l.checkMailStarted(context, account);
        }
        putBackground("checkMail", listener, new Runnable()
        {
            public void run()
            {

                try
                {
                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Starting mail check");
                    Preferences prefs = Preferences.getPreferences(context);

                    Account[] accounts;
                    if (account != null)
                    {
                        accounts = new Account[]
                        {
                            account
                        };
                    }
                    else
                    {
                        accounts = prefs.getAccounts();
                    }

                    for (final Account account : accounts)
                    {
                        checkMailForAccount(context, account,ignoreLastCheckedTime, prefs, listener);
                    }

                }
                catch (Exception e)
                {
                    Log.e(K9.LOG_TAG, "Unable to synchronize mail", e);
                    addErrorMessage(account, null, e);
                }
                putBackground("finalize sync", null, new Runnable()
                {
                    public void run()
                    {

                        if (K9.DEBUG)
                            Log.i(K9.LOG_TAG, "Finished mail sync");

                        if (wakeLock != null)
                        {
                            wakeLock.release();
                        }
                        for (MessagingListener l : getListeners())
                        {
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
                                     final MessagingListener listener)
    {
        if (!account.isAvailable(context))
        {
            if (K9.DEBUG)
            {
                Log.i(K9.LOG_TAG, "Skipping synchronizing unavailable account " + account.getDescription());
            }
            return;
        }
        final long accountInterval = account.getAutomaticCheckIntervalMinutes() * 60 * 1000;
        if (!ignoreLastCheckedTime && accountInterval <= 0)
        {
            if (K9.DEBUG)
                Log.i(K9.LOG_TAG, "Skipping synchronizing account " + account.getDescription());
            return;
        }

        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Synchronizing account " + account.getDescription());

        account.setRingNotified(false);

        sendPendingMessages(account, listener);

        try
        {
            Account.FolderMode aDisplayMode = account.getFolderDisplayMode();
            Account.FolderMode aSyncMode = account.getFolderSyncMode();

            Store localStore = account.getLocalStore();
            for (final Folder folder : localStore.getPersonalNamespaces(false))
            {
                folder.open(Folder.OpenMode.READ_WRITE);
                folder.refresh(prefs);

                Folder.FolderClass fDisplayClass = folder.getDisplayClass();
                Folder.FolderClass fSyncClass = folder.getSyncClass();

                if (modeMismatch(aDisplayMode, fDisplayClass))
                {
                    // Never sync a folder that isn't displayed
                    /*
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Not syncing folder " + folder.getName() +
                              " which is in display mode " + fDisplayClass + " while account is in display mode " + aDisplayMode);
                    */

                    continue;
                }

                if (modeMismatch(aSyncMode, fSyncClass))
                {
                    // Do not sync folders in the wrong class
                    /*
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Not syncing folder " + folder.getName() +
                              " which is in sync mode " + fSyncClass + " while account is in sync mode " + aSyncMode);
                    */

                    continue;
                }
                synchronizeFolder(context, account, folder, ignoreLastCheckedTime, accountInterval, listener);
            }
        }
        catch (MessagingException e)
        {
            Log.e(K9.LOG_TAG, "Unable to synchronize account " + account.getName(), e);
            addErrorMessage(account, null, e);
        }
        finally
        {
            putBackground("clear notification flag for " + account.getDescription(), null, new Runnable()
            {
                public void run()
                {
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Clearing notification flag for " + account.getDescription());
                    account.setRingNotified(false);
                    try
                    {
                        AccountStats stats = account.getStats(context);
                        if (stats == null || stats.unreadMessageCount == 0)
                        {
                            notifyAccountCancel(context, account);
                        }
                    }
                    catch (MessagingException e)
                    {
                        Log.e(K9.LOG_TAG, "Unable to getUnreadMessageCount for account: " + account, e);
                    }
                }
            }
                         );
        }


    }


    private void synchronizeFolder(
        final Context context, final Account account,
        final Folder folder,
        final boolean ignoreLastCheckedTime,
        final long accountInterval,
        final MessagingListener listener


    )
    {


        if (K9.DEBUG)
            Log.v(K9.LOG_TAG, "Folder " + folder.getName() + " was last synced @ " +
                  new Date(folder.getLastChecked()));

        if (!ignoreLastCheckedTime && folder.getLastChecked() >
                (System.currentTimeMillis() - accountInterval))
        {
            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "Not syncing folder " + folder.getName()
                      + ", previously synced @ " + new Date(folder.getLastChecked())
                      + " which would be too recent for the account period");

            return;
        }
        putBackground("sync" + folder.getName(), null, new Runnable()
        {
            public void run()
            {
                LocalFolder tLocalFolder = null;
                try
                {
                    // In case multiple Commands get enqueued, don't run more than
                    // once
                    final LocalStore localStore = account.getLocalStore();
                    tLocalFolder = localStore.getFolder(folder.getName());
                    tLocalFolder.open(Folder.OpenMode.READ_WRITE);

                    if (!ignoreLastCheckedTime && tLocalFolder.getLastChecked() >
                            (System.currentTimeMillis() - accountInterval))
                    {
                        if (K9.DEBUG)
                            Log.v(K9.LOG_TAG, "Not running Command for folder " + folder.getName()
                                  + ", previously synced @ " + new Date(folder.getLastChecked())
                                  + " which would be too recent for the account period");
                        return;
                    }
                    notifyFetchingMail(account, folder);
                    try
                    {
                        synchronizeMailboxSynchronous(account, folder.getName(), listener, null);
                    }
                    finally
                    {
                        notifyFetchingMailCancel(account);
                    }
                }
                catch (Exception e)
                {

                    Log.e(K9.LOG_TAG, "Exception while processing folder " +
                          account.getDescription() + ":" + folder.getName(), e);
                    addErrorMessage(account, null, e);
                }
                finally
                {
                    closeFolder(tLocalFolder);
                }
            }
        }
                     );


    }



    public void compact(final Account account, final MessagingListener ml)
    {
        putBackground("compact:" + account.getDescription(), ml, new Runnable()
        {
            public void run()
            {
                try
                {
                    LocalStore localStore = account.getLocalStore();
                    long oldSize = localStore.getSize();
                    localStore.compact();
                    long newSize = localStore.getSize();
                    if (ml != null)
                    {
                        ml.accountSizeChanged(account, oldSize, newSize);
                    }
                    for (MessagingListener l : getListeners())
                    {
                        l.accountSizeChanged(account, oldSize, newSize);
                    }
                }
                catch (UnavailableStorageException e)
                {
                    Log.i(K9.LOG_TAG, "Failed to compact account because storage is not available - trying again later.");
                    throw new UnavailableAccountException(e);
                }
                catch (Exception e)
                {
                    Log.e(K9.LOG_TAG, "Failed to compact account " + account.getDescription(), e);
                }
            }
        });
    }

    public void clear(final Account account, final MessagingListener ml)
    {
        putBackground("clear:" + account.getDescription(), ml, new Runnable()
        {
            public void run()
            {
                try
                {
                    LocalStore localStore = account.getLocalStore();
                    long oldSize = localStore.getSize();
                    localStore.clear();
                    localStore.resetVisibleLimits(account.getDisplayCount());
                    long newSize = localStore.getSize();
                    AccountStats stats = new AccountStats();
                    stats.size = newSize;
                    stats.unreadMessageCount = 0;
                    stats.flaggedMessageCount = 0;
                    if (ml != null)
                    {
                        ml.accountSizeChanged(account, oldSize, newSize);
                        ml.accountStatusChanged(account, stats);
                    }
                    for (MessagingListener l : getListeners())
                    {
                        l.accountSizeChanged(account, oldSize, newSize);
                        l.accountStatusChanged(account, stats);
                    }
                }
                catch (UnavailableStorageException e)
                {
                    Log.i(K9.LOG_TAG, "Failed to clear account because storage is not available - trying again later.");
                    throw new UnavailableAccountException(e);
                }
                catch (Exception e)
                {
                    Log.e(K9.LOG_TAG, "Failed to clear account " + account.getDescription(), e);
                }
            }
        });
    }

    public void recreate(final Account account, final MessagingListener ml)
    {
        putBackground("recreate:" + account.getDescription(), ml, new Runnable()
        {
            public void run()
            {
                try
                {
                    LocalStore localStore = account.getLocalStore();
                    long oldSize = localStore.getSize();
                    localStore.recreate();
                    localStore.resetVisibleLimits(account.getDisplayCount());
                    long newSize = localStore.getSize();
                    AccountStats stats = new AccountStats();
                    stats.size = newSize;
                    stats.unreadMessageCount = 0;
                    stats.flaggedMessageCount = 0;
                    if (ml != null)
                    {
                        ml.accountSizeChanged(account, oldSize, newSize);
                        ml.accountStatusChanged(account, stats);
                    }
                    for (MessagingListener l : getListeners())
                    {
                        l.accountSizeChanged(account, oldSize, newSize);
                        l.accountStatusChanged(account, stats);
                    }
                }
                catch (UnavailableStorageException e)
                {
                    Log.i(K9.LOG_TAG, "Failed to recreate an account because storage is not available - trying again later.");
                    throw new UnavailableAccountException(e);
                }
                catch (Exception e)
                {
                    Log.e(K9.LOG_TAG, "Failed to recreate account " + account.getDescription(), e);
                }
            }
        });
    }


    private boolean shouldNotifyForMessage(Account account, Message message)
    {
        // Do not notify if the user does not have notifications
        // enabled or if the message has been read
        if (!account.isNotifyNewMail() || message.isSet(Flag.SEEN) || (account.getName() == null))
        {
            return false;
        }


        Folder folder = message.getFolder();
        if (folder != null)
        {
            // No notification for new messages in Trash, Drafts, or Sent folder.
            // But do notify if it's the INBOX (see issue 1817).
            String folderName = folder.getName();
            if (!K9.INBOX.equals(folderName) &&
                    (account.getTrashFolderName().equals(folderName)
                     || account.getDraftsFolderName().equals(folderName)
                     || account.getSentFolderName().equals(folderName)))
            {
                return false;
            }
        }

        return true;

    }



    /** Creates a notification of new email messages
      * ringtone, lights, and vibration to be played
    */
    private boolean notifyAccount(Context context, Account account, Message message, int previousUnreadMessageCount, AtomicInteger newMessageCount)
    {
        // If we don't even have an account name, don't show the notification
        // (This happens during initial account setup)
        //
        if (account.getName() == null)
        {
            return false;
        }

        // If we have a message, set the notification to "<From>: <Subject>"
        StringBuilder messageNotice = new StringBuilder();
        final KeyguardManager keyguardService = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        try
        {
            if (message != null && message.getFrom() != null)
            {
                Address[] fromAddrs = message.getFrom();
                String from = fromAddrs.length > 0 ? fromAddrs[0].toFriendly().toString() : null;
                String subject = message.getSubject();
                if (subject == null)
                {
                    subject = context.getString(R.string.general_no_subject);
                }

                if (from != null)
                {
                    // Show From: address by default
                    if (!account.isAnIdentity(fromAddrs))
                    {
                        messageNotice.append(from + ": " + subject);
                    }
                    // show To: if the message was sent from me
                    else
                    {
                        if (!account.isNotifySelfNewMail())
                        {
                            return false;
                        }

                        Address[] rcpts = message.getRecipients(Message.RecipientType.TO);
                        String to = rcpts.length > 0 ? rcpts[0].toFriendly().toString() : null;
                        if (to != null)
                        {
                            messageNotice.append(String.format(context.getString(R.string.message_to_fmt), to) +": "+subject);
                        }
                        else
                        {
                            messageNotice.append(context.getString(R.string.general_no_sender) + ": "+subject);

                        }

                    }
                }
            }
        }
        catch (MessagingException e)
        {
            Log.e(K9.LOG_TAG, "Unable to get message information for notification.", e);
        }

        // If privacy mode active and keyguard active
        // OR
        // If we could not set a per-message notification, revert to a default message
        if ((K9.keyguardPrivacy() && keyguardService.inKeyguardRestrictedInputMode()) || messageNotice.length() == 0)
        {
            messageNotice = new StringBuilder(context.getString(R.string.notification_new_title));
        }

        NotificationManager notifMgr =
            (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notif = new Notification(R.drawable.stat_notify_email_generic, messageNotice, System.currentTimeMillis());
        notif.number = previousUnreadMessageCount + newMessageCount.get();

        Intent i = FolderList.actionHandleNotification(context, account, message.getFolder().getName());
        PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);

        String accountNotice = context.getString(R.string.notification_new_one_account_fmt, notif.number, account.getDescription());
        notif.setLatestEventInfo(context, accountNotice, messageNotice, pi);

        // Only ring or vibrate if we have not done so already on this
        // account and fetch
        boolean ringAndVibrate = false;
        if (!account.isRingNotified())
        {
            account.setRingNotified(true);
            ringAndVibrate = true;
        }

        NotificationSetting n = account.getNotificationSetting();

        configureNotification(notif, ( n.shouldRing() ?  n.getRingtone() : null), (n.shouldVibrate() ? n.getVibration() : null), (n.isLed() ?  n.getLedColor()  : null), K9.NOTIFICATION_LED_BLINK_SLOW, ringAndVibrate);

        notifMgr.notify(account.getAccountNumber(), notif);
        return true;
    }

    /**
     * @param notification
     *            Object to configure. Never <code>null</code>.
     * @param rintone
     *          String name of ringtone. <code>null</code> if no ringtone should be played
     * @param vibrationPattern
     *         <code>long[]</code> vibration pattern. <code>null</code> if no vibration should be played
     * @param ledColor
     *         <code>Integer</code> Color to flash LED. <code>null</code> if no LED flash should happen
     * @param ledSpeed
     *         <code>int</code> should LEDs flash K9.NOTIFICATION_LED_BLINK_SLOW or K9.NOTIFICATION_LED_BLINK_FAST
     * @param ringAndVibrate
     *            <code>true</code> if ringtone/vibration are allowed,
     *            <code>false</code> otherwise.
     */
    private void configureNotification(final Notification notification,
                                       final String ringtone,
                                       final long[] vibrationPattern,
                                       final Integer ledColor,
                                       final int ledSpeed,

                                       final boolean ringAndVibrate)
    {

        // if it's quiet time, then we shouldn't be ringing, buzzing or flashing
        if (K9.isQuietTime())
        {
            return;
        }

        if (ringAndVibrate)
        {
            if (ringtone != null)
            {
                notification.sound = TextUtils.isEmpty(ringtone) ? null : Uri.parse(ringtone);
                notification.audioStreamType = AudioManager.STREAM_NOTIFICATION;
            }
            if (vibrationPattern != null )
            {
                notification.vibrate = vibrationPattern;
            }
        }

        if (ledColor != null )
        {
            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
            notification.ledARGB = ledColor;
            if (ledSpeed == K9.NOTIFICATION_LED_BLINK_SLOW)
            {
                notification.ledOnMS = K9.NOTIFICATION_LED_ON_TIME;
                notification.ledOffMS = K9.NOTIFICATION_LED_OFF_TIME;
            }
            else if (ledSpeed == K9.NOTIFICATION_LED_BLINK_FAST)
            {
                notification.ledOnMS = K9.NOTIFICATION_LED_FAST_ON_TIME;
                notification.ledOffMS = K9.NOTIFICATION_LED_FAST_OFF_TIME;
            }
        }
    }

    /** Cancel a notification of new email messages */
    public void notifyAccountCancel(Context context, Account account)
    {
        NotificationManager notifMgr =
            (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifMgr.cancel(account.getAccountNumber());
        notifMgr.cancel(-1000 - account.getAccountNumber());
    }


    public Message saveDraft(final Account account, final Message message)
    {
        Message localMessage = null;
        try
        {
            LocalStore localStore = account.getLocalStore();
            LocalFolder localFolder = localStore.getFolder(account.getDraftsFolderName());
            localFolder.open(OpenMode.READ_WRITE);
            localFolder.appendMessages(new Message[]
                                       {
                                           message
                                       });
            localMessage = localFolder.getMessage(message.getUid());
            localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);

            PendingCommand command = new PendingCommand();
            command.command = PENDING_COMMAND_APPEND;
            command.arguments = new String[]
            {
                localFolder.getName(),
                localMessage.getUid()
            };
            queuePendingCommand(account, command);
            processPendingCommands(account);

        }
        catch (MessagingException e)
        {
            Log.e(K9.LOG_TAG, "Unable to save message as draft.", e);
            addErrorMessage(account, null, e);
        }
        return localMessage;
    }

    public boolean modeMismatch(Account.FolderMode aMode, Folder.FolderClass fMode)
    {
        if (aMode == Account.FolderMode.NONE
                || (aMode == Account.FolderMode.FIRST_CLASS &&
                    fMode != Folder.FolderClass.FIRST_CLASS)
                || (aMode == Account.FolderMode.FIRST_AND_SECOND_CLASS &&
                    fMode != Folder.FolderClass.FIRST_CLASS &&
                    fMode != Folder.FolderClass.SECOND_CLASS)
                || (aMode == Account.FolderMode.NOT_SECOND_CLASS &&
                    fMode == Folder.FolderClass.SECOND_CLASS))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    static AtomicInteger sequencing = new AtomicInteger(0);
    class Command implements Comparable<Command>
    {
        public Runnable runnable;

        public MessagingListener listener;

        public String description;

        boolean isForeground;

        int sequence = sequencing.getAndIncrement();

        @Override
        public int compareTo(Command other)
        {
            if (other.isForeground && !isForeground)
            {
                return 1;
            }
            else if (!other.isForeground && isForeground)
            {
                return -1;
            }
            else
            {
                return (sequence - other.sequence);
            }
        }
    }

    public MessagingListener getCheckMailListener()
    {
        return checkMailListener;
    }

    public void setCheckMailListener(MessagingListener checkMailListener)
    {
        if (this.checkMailListener != null)
        {
            removeListener(this.checkMailListener);
        }
        this.checkMailListener = checkMailListener;
        if (this.checkMailListener != null)
        {
            addListener(this.checkMailListener);
        }
    }

    public SORT_TYPE getSortType()
    {
        return sortType;
    }

    public void setSortType(SORT_TYPE sortType)
    {
        this.sortType = sortType;
    }

    public boolean isSortAscending(SORT_TYPE sortType)
    {
        Boolean sortAsc = sortAscending.get(sortType);
        if (sortAsc == null)
        {
            return sortType.isDefaultAscending();
        }
        else return sortAsc;
    }

    public void setSortAscending(SORT_TYPE sortType, boolean nsortAscending)
    {
        sortAscending.put(sortType, nsortAscending);
    }

    public Collection<Pusher> getPushers()
    {
        return pushers.values();
    }

    public boolean setupPushing(final Account account)
    {
        try
        {
            Pusher previousPusher = pushers.remove(account);
            if (previousPusher != null)
            {
                previousPusher.stop();
            }
            Preferences prefs = Preferences.getPreferences(mApplication);

            Account.FolderMode aDisplayMode = account.getFolderDisplayMode();
            Account.FolderMode aPushMode = account.getFolderPushMode();

            List<String> names = new ArrayList<String>();

            Store localStore = account.getLocalStore();
            for (final Folder folder : localStore.getPersonalNamespaces(false))
            {
                if (folder.getName().equals(account.getErrorFolderName())
                        || folder.getName().equals(account.getOutboxFolderName()))
                {
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

                if (modeMismatch(aDisplayMode, fDisplayClass))
                {
                    // Never push a folder that isn't displayed
                    /*
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "Not pushing folder " + folder.getName() +
                              " which is in display class " + fDisplayClass + " while account is in display mode " + aDisplayMode);
                    */

                    continue;
                }

                if (modeMismatch(aPushMode, fPushClass))
                {
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

            if (names.size() > 0)
            {
                PushReceiver receiver = new MessagingControllerPushReceiver(mApplication, account, this);
                int maxPushFolders = account.getMaxPushFolders();

                if (names.size() > maxPushFolders)
                {
                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Count of folders to push for account " + account.getDescription() + " is " + names.size()
                              + ", greater than limit of " + maxPushFolders + ", truncating");

                    names = names.subList(0, maxPushFolders);
                }

                try
                {
                    Store store = account.getRemoteStore();
                    if (!store.isPushCapable())
                    {
                        if (K9.DEBUG)
                            Log.i(K9.LOG_TAG, "Account " + account.getDescription() + " is not push capable, skipping");

                        return false;
                    }
                    Pusher pusher = store.getPusher(receiver);
                    if (pusher != null)
                    {
                        Pusher oldPusher  = pushers.putIfAbsent(account, pusher);
                        if (oldPusher == null)
                        {
                            pusher.start(names);
                        }
                    }
                }
                catch (Exception e)
                {
                    Log.e(K9.LOG_TAG, "Could not get remote store", e);
                    return false;
                }

                return true;
            }
            else
            {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "No folders are configured for pushing in account " + account.getDescription());
                return false;
            }

        }
        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "Got exception while setting up pushing", e);
        }
        return false;
    }

    public void stopAllPushing()
    {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Stopping all pushers");

        Iterator<Pusher> iter = pushers.values().iterator();
        while (iter.hasNext())
        {
            Pusher pusher = iter.next();
            iter.remove();
            pusher.stop();
        }
    }

    public void messagesArrived(final Account account, final Folder remoteFolder, final List<Message> messages, final boolean flagSyncOnly)
    {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Got new pushed email messages for account " + account.getDescription()
                  + ", folder " + remoteFolder.getName());

        final CountDownLatch latch = new CountDownLatch(1);
        putBackground("Push messageArrived of account " + account.getDescription()
                      + ", folder " + remoteFolder.getName(), null, new Runnable()
        {
            public void run()
            {
                LocalFolder localFolder = null;
                try
                {
                    LocalStore localStore = account.getLocalStore();
                    localFolder= localStore.getFolder(remoteFolder.getName());
                    localFolder.open(OpenMode.READ_WRITE);

                    account.setRingNotified(false);
                    int newCount = downloadMessages(account, remoteFolder, localFolder, messages, flagSyncOnly);
                    int unreadMessageCount = setLocalUnreadCountToRemote(localFolder, remoteFolder,  messages.size());

                    setLocalFlaggedCountToRemote(localFolder, remoteFolder);

                    localFolder.setLastPush(System.currentTimeMillis());
                    localFolder.setStatus(null);

                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "messagesArrived newCount = " + newCount + ", unread count = " + unreadMessageCount);

                    if (unreadMessageCount == 0)
                    {
                        notifyAccountCancel(mApplication, account);
                    }

                    for (MessagingListener l : getListeners())
                    {
                        l.folderStatusChanged(account, remoteFolder.getName(), unreadMessageCount);
                    }

                }
                catch (Exception e)
                {
                    String rootMessage = getRootCauseMessage(e);
                    String errorMessage = "Push failed: " + rootMessage;
                    try
                    {
                        localFolder.setStatus(errorMessage);
                    }
                    catch (Exception se)
                    {
                        Log.e(K9.LOG_TAG, "Unable to set failed status on localFolder", se);
                    }
                    for (MessagingListener l : getListeners())
                    {
                        l.synchronizeMailboxFailed(account, remoteFolder.getName(), errorMessage);
                    }
                    addErrorMessage(account, null, e);
                }
                finally
                {
                    closeFolder(localFolder);
                    latch.countDown();
                }

            }
        });
        try
        {
            latch.await();
        }
        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "Interrupted while awaiting latch release", e);
        }
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "MessagingController.messagesArrivedLatch released");
    }
    enum MemorizingState { STARTED, FINISHED, FAILED };

    class Memory
    {
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

        Memory(Account nAccount, String nFolderName)
        {
            account = nAccount;
            folderName = nFolderName;
        }

        String getKey()
        {
            return getMemoryKey(account, folderName);
        }


    }
    static String getMemoryKey(Account taccount, String tfolderName)
    {
        return taccount.getDescription() + ":" + tfolderName;
    }
    class MemorizingListener extends MessagingListener
    {
        HashMap<String, Memory> memories = new HashMap<String, Memory>(31);

        Memory getMemory(Account account, String folderName)
        {
            Memory memory = memories.get(getMemoryKey(account, folderName));
            if (memory == null)
            {
                memory = new Memory(account, folderName);
                memories.put(memory.getKey(), memory);
            }
            return memory;
        }

        @Override
        public synchronized void synchronizeMailboxStarted(Account account, String folder)
        {
            Memory memory = getMemory(account, folder);
            memory.syncingState = MemorizingState.STARTED;
            memory.folderCompleted = 0;
            memory.folderTotal = 0;
        }

        @Override
        public synchronized void synchronizeMailboxFinished(Account account, String folder,
                int totalMessagesInMailbox, int numNewMessages)
        {
            Memory memory = getMemory(account, folder);
            memory.syncingState = MemorizingState.FINISHED;
            memory.syncingTotalMessagesInMailbox = totalMessagesInMailbox;
            memory.syncingNumNewMessages = numNewMessages;
        }

        @Override
        public synchronized void synchronizeMailboxFailed(Account account, String folder,
                String message)
        {

            Memory memory = getMemory(account, folder);
            memory.syncingState = MemorizingState.FAILED;
            memory.failureMessage = message;
        }
        synchronized void refreshOther(MessagingListener other)
        {
            if (other != null)
            {

                Memory syncStarted = null;
                Memory sendStarted = null;
                Memory processingStarted = null;

                for (Memory memory : memories.values())
                {

                    if (memory.syncingState != null)
                    {
                        switch (memory.syncingState)
                        {
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

                    if (memory.sendingState != null)
                    {
                        switch (memory.sendingState)
                        {
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
                    if (memory.pushingState != null)
                    {
                        switch (memory.pushingState)
                        {
                            case STARTED:
                                other.setPushActive(memory.account, memory.folderName, true);
                                break;
                            case FINISHED:
                                other.setPushActive(memory.account, memory.folderName, false);
                                break;
                        }
                    }
                    if (memory.processingState != null)
                    {
                        switch (memory.processingState)
                        {
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
                if (syncStarted != null)
                {
                    other.synchronizeMailboxStarted(syncStarted.account, syncStarted.folderName);
                    somethingStarted = syncStarted;
                }
                if (sendStarted != null)
                {
                    other.sendPendingMessagesStarted(sendStarted.account);
                    somethingStarted = sendStarted;
                }
                if (processingStarted != null)
                {
                    other.pendingCommandsProcessing(processingStarted.account);
                    if (processingStarted.processingCommandTitle != null)
                    {
                        other.pendingCommandStarted(processingStarted.account, processingStarted.processingCommandTitle);

                    }
                    else
                    {
                        other.pendingCommandCompleted(processingStarted.account, processingStarted.processingCommandTitle);
                    }
                    somethingStarted = processingStarted;
                }
                if (somethingStarted != null && somethingStarted.folderTotal > 0)
                {
                    other.synchronizeMailboxProgress(somethingStarted.account, somethingStarted.folderName, somethingStarted.folderCompleted, somethingStarted.folderTotal);
                }

            }
        }
        @Override
        public synchronized void setPushActive(Account account, String folderName, boolean active)
        {
            Memory memory = getMemory(account, folderName);
            memory.pushingState = (active ? MemorizingState.STARTED : MemorizingState.FINISHED);
        }

        @Override
        public synchronized void sendPendingMessagesStarted(Account account)
        {
            Memory memory = getMemory(account, null);
            memory.sendingState = MemorizingState.STARTED;
            memory.folderCompleted = 0;
            memory.folderTotal = 0;
        }

        @Override
        public synchronized void sendPendingMessagesCompleted(Account account)
        {
            Memory memory = getMemory(account, null);
            memory.sendingState = MemorizingState.FINISHED;
        }

        @Override
        public synchronized void sendPendingMessagesFailed(Account account)
        {
            Memory memory = getMemory(account, null);
            memory.sendingState = MemorizingState.FAILED;
        }


        @Override
        public synchronized void synchronizeMailboxProgress(Account account, String folderName, int completed, int total)
        {
            Memory memory = getMemory(account, folderName);
            memory.folderCompleted = completed;
            memory.folderTotal = total;
        }


        @Override
        public synchronized void pendingCommandsProcessing(Account account)
        {
            Memory memory = getMemory(account, null);
            memory.processingState = MemorizingState.STARTED;
            memory.folderCompleted = 0;
            memory.folderTotal = 0;
        }
        @Override
        public synchronized void pendingCommandsFinished(Account account)
        {
            Memory memory = getMemory(account, null);
            memory.processingState = MemorizingState.FINISHED;
        }
        @Override
        public synchronized void pendingCommandStarted(Account account, String commandTitle)
        {
            Memory memory = getMemory(account, null);
            memory.processingCommandTitle = commandTitle;
        }

        @Override
        public synchronized void pendingCommandCompleted(Account account, String commandTitle)
        {
            Memory memory = getMemory(account, null);
            memory.processingCommandTitle = null;
        }

    }

    private void actOnMessages(Message[] messages, MessageActor actor)
    {
        Map<Account, Map<Folder, List<Message>>> accountMap = new HashMap<Account, Map<Folder, List<Message>>>();

        for (Message message : messages)
        {
            Folder folder = message.getFolder();
            Account account = folder.getAccount();

            Map<Folder, List<Message>> folderMap = accountMap.get(account);
            if (folderMap == null)
            {
                folderMap = new HashMap<Folder, List<Message>>();
                accountMap.put(account, folderMap);
            }
            List<Message> messageList = folderMap.get(folder);
            if (messageList == null)
            {
                messageList = new LinkedList<Message>();
                folderMap.put(folder, messageList);
            }

            messageList.add(message);
        }
        for (Map.Entry<Account, Map<Folder, List<Message>>> entry : accountMap.entrySet())
        {
            Account account = entry.getKey();

            //account.refresh(Preferences.getPreferences(K9.app));
            Map<Folder, List<Message>> folderMap = entry.getValue();
            for (Map.Entry<Folder, List<Message>> folderEntry : folderMap.entrySet())
            {
                Folder folder = folderEntry.getKey();
                List<Message> messageList = folderEntry.getValue();
                actor.act(account, folder, messageList);
            }
        }
    }

    interface MessageActor
    {
        public void act(final Account account, final Folder folder, final List<Message> messages);
    }
}
