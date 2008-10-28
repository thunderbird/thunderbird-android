
package com.android.email;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Application;
import android.content.Context;
import android.os.Process;
import android.util.Config;
import android.util.Log;

import com.android.email.mail.FetchProfile;
import com.android.email.mail.Flag;
import com.android.email.mail.Folder;
import com.android.email.mail.Message;
import com.android.email.mail.MessageRetrievalListener;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Part;
import com.android.email.mail.Store;
import com.android.email.mail.Transport;
import com.android.email.mail.Folder.FolderType;
import com.android.email.mail.Folder.OpenMode;
import com.android.email.mail.internet.MimeHeader;
import com.android.email.mail.internet.MimeUtility;
import com.android.email.mail.store.LocalStore;
import com.android.email.mail.store.LocalStore.LocalFolder;
import com.android.email.mail.store.LocalStore.LocalMessage;
import com.android.email.mail.store.LocalStore.PendingCommand;

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
    private static final int MAX_SMALL_MESSAGE_SIZE = (25 * 1024);

    private static final String PENDING_COMMAND_TRASH =
        "com.android.email.MessagingController.trash";
    private static final String PENDING_COMMAND_MARK_READ =
        "com.android.email.MessagingController.markRead";
    private static final String PENDING_COMMAND_APPEND =
        "com.android.email.MessagingController.append";

    private static MessagingController inst = null;
    private BlockingQueue<Command> mCommands = new LinkedBlockingQueue<Command>();
    private Thread mThread;
    private HashSet<MessagingListener> mListeners = new HashSet<MessagingListener>();
    private boolean mBusy;
    private Application mApplication;

    private MessagingController(Application application) {
        mApplication = application;
        mThread = new Thread(this);
        mThread.start();
    }

    /**
     * Gets or creates the singleton instance of MessagingController. Application is used to
     * provide a Context to classes that need it.
     * @param application
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

    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true) {
            try {
                Command command = mCommands.take();
                if (command.listener == null || mListeners.contains(command.listener)) {
                    mBusy = true;
                    command.runnable.run();
                    for (MessagingListener l : mListeners) {
                        l.controllerCommandCompleted(mCommands.size() > 0);
                    }
                }
            }
            catch (Exception e) {
                if (Config.LOGV) {
                    Log.v(Email.LOG_TAG, "Error running command", e);
                }
            }
            mBusy = false;
        }
    }

    private void put(String description, MessagingListener listener, Runnable runnable) {
        try {
            Command command = new Command();
            command.listener = listener;
            command.runnable = runnable;
            command.description = description;
            mCommands.put(command);
        }
        catch (InterruptedException ie) {
            throw new Error(ie);
        }
    }

    public void addListener(MessagingListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(MessagingListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Lists folders that are available locally and remotely. This method calls
     * listFoldersCallback for local folders before it returns, and then for
     * remote folders at some later point. If there are no local folders
     * includeRemote is forced by this method. This method should be called from
     * a Thread as it may take several seconds to list the local folders. TODO
     * this needs to cache the remote folder list
     *
     * @param account
     * @param includeRemote
     * @param listener
     * @throws MessagingException
     */
    public void listFolders(
            final Account account,
            boolean refreshRemote,
            MessagingListener listener) {
        for (MessagingListener l : mListeners) {
            l.listFoldersStarted(account);
        }
        try {
            Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
            Folder[] localFolders = localStore.getPersonalNamespaces();

            if (localFolders == null || localFolders.length == 0) {
                refreshRemote = true;
            } else {
                for (MessagingListener l : mListeners) {
                    l.listFolders(account, localFolders);
                }
            }
        }
        catch (Exception e) {
            for (MessagingListener l : mListeners) {
                l.listFoldersFailed(account, e.getMessage());
                return;
            }
        }
        if (refreshRemote) {
            put("listFolders", listener, new Runnable() {
                public void run() {
                    try {
                        Store store = Store.getInstance(account.getStoreUri(), mApplication);

                        Folder[] remoteFolders = store.getPersonalNamespaces();

                        Store localStore = Store.getInstance(
                                account.getLocalStoreUri(),
                                mApplication);
                        HashSet<String> remoteFolderNames = new HashSet<String>();
                        for (int i = 0, count = remoteFolders.length; i < count; i++) {
                            Folder localFolder = localStore.getFolder(remoteFolders[i].getName());
                            if (!localFolder.exists()) {
                                localFolder.create(FolderType.HOLDS_MESSAGES);
                            }
                            remoteFolderNames.add(remoteFolders[i].getName());
                        }

                        Folder[] localFolders = localStore.getPersonalNamespaces();

                        /*
                         * Clear out any folders that are no longer on the remote store.
                         */
                        for (Folder localFolder : localFolders) {
                            String localFolderName = localFolder.getName();
                            if (localFolderName.equalsIgnoreCase(Email.INBOX) ||
                                    localFolderName.equals(account.getTrashFolderName()) ||
                                    localFolderName.equals(account.getOutboxFolderName()) ||
                                    localFolderName.equals(account.getDraftsFolderName()) ||
                                    localFolderName.equals(account.getSentFolderName())) {
                                continue;
                            }
                            if (!remoteFolderNames.contains(localFolder.getName())) {
                                localFolder.delete(false);
                            }
                        }

                        localFolders = localStore.getPersonalNamespaces();

                        for (MessagingListener l : mListeners) {
                            l.listFolders(account, localFolders);
                        }
                        for (MessagingListener l : mListeners) {
                            l.listFoldersFinished(account);
                        }
                    }
                    catch (Exception e) {
                        for (MessagingListener l : mListeners) {
                            l.listFoldersFailed(account, "");
                        }
                    }
                }
            });
        } else {
            for (MessagingListener l : mListeners) {
                l.listFoldersFinished(account);
            }
        }
    }

    /**
     * List the local message store for the given folder. This work is done
     * synchronously.
     *
     * @param account
     * @param folder
     * @param listener
     * @throws MessagingException
     */
    public void listLocalMessages(final Account account, final String folder,
            MessagingListener listener) {
        for (MessagingListener l : mListeners) {
            l.listLocalMessagesStarted(account, folder);
        }

        try {
            Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
            Folder localFolder = localStore.getFolder(folder);
            localFolder.open(OpenMode.READ_WRITE);
            Message[] localMessages = localFolder.getMessages(null);
            ArrayList<Message> messages = new ArrayList<Message>();
            for (Message message : localMessages) {
                if (!message.isSet(Flag.DELETED)) {
                    messages.add(message);
                }
            }
            for (MessagingListener l : mListeners) {
                l.listLocalMessages(account, folder, messages.toArray(new Message[0]));
            }
            for (MessagingListener l : mListeners) {
                l.listLocalMessagesFinished(account, folder);
            }
        }
        catch (Exception e) {
            for (MessagingListener l : mListeners) {
                l.listLocalMessagesFailed(account, folder, e.getMessage());
            }
        }
    }

    public void loadMoreMessages(Account account, String folder, MessagingListener listener) {
        try {
            LocalStore localStore = (LocalStore) Store.getInstance(
                    account.getLocalStoreUri(),
                    mApplication);
            LocalFolder localFolder = (LocalFolder) localStore.getFolder(folder);
            localFolder.setVisibleLimit(localFolder.getVisibleLimit()
                    + Email.VISIBLE_LIMIT_INCREMENT);
            synchronizeMailbox(account, folder, listener);
        }
        catch (MessagingException me) {
            throw new RuntimeException("Unable to set visible limit on folder", me);
        }
    }

    public void resetVisibleLimits(Account[] accounts) {
        for (Account account : accounts) {
            try {
                LocalStore localStore =
                    (LocalStore) Store.getInstance(account.getLocalStoreUri(), mApplication);
                localStore.resetVisibleLimits();
            }
            catch (MessagingException e) {
                Log.e(Email.LOG_TAG, "Unable to reset visible limits", e);
            }
        }
    }

    /**
     * Start background synchronization of the specified folder.
     * @param account
     * @param folder
     * @param numNewestMessagesToKeep Specifies the number of messages that should be
     * considered as part of the window of available messages. This number effectively limits
     * the user's view into the mailbox to the newest (numNewestMessagesToKeep) messages.
     * @param listener
     */
    public void synchronizeMailbox(final Account account, final String folder,
            MessagingListener listener) {
        /*
         * We don't ever sync the Outbox.
         */
        if (folder.equals(account.getOutboxFolderName())) {
            return;
        }
        for (MessagingListener l : mListeners) {
            l.synchronizeMailboxStarted(account, folder);
        }
        put("synchronizeMailbox", listener, new Runnable() {
            public void run() {
                synchronizeMailboxSyncronous(account, folder);
            }
        });
    }

    /**
     * Start foreground synchronization of the specified folder. This is generally only called
     * by synchronizeMailbox.
     * @param account
     * @param folder
     * @param numNewestMessagesToKeep Specifies the number of messages that should be
     * considered as part of the window of available messages. This number effectively limits
     * the user's view into the mailbox to the newest (numNewestMessagesToKeep) messages.
     * @param listener
     *
     * TODO Break this method up into smaller chunks.
     */
    public void synchronizeMailboxSyncronous(final Account account, final String folder) {
        for (MessagingListener l : mListeners) {
            l.synchronizeMailboxStarted(account, folder);
        }
        try {
            processPendingCommandsSynchronous(account);

            /*
             * Get the message list from the local store and create an index of
             * the uids within the list.
             */
            final LocalStore localStore =
                (LocalStore) Store.getInstance(account.getLocalStoreUri(), mApplication);
            final LocalFolder localFolder = (LocalFolder) localStore.getFolder(folder);
            localFolder.open(OpenMode.READ_WRITE);
            Message[] localMessages = localFolder.getMessages(null);
            HashMap<String, Message> localUidMap = new HashMap<String, Message>();
            for (Message message : localMessages) {
                localUidMap.put(message.getUid(), message);
            }

            Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
            Folder remoteFolder = remoteStore.getFolder(folder);

            /*
             * If the folder is a "special" folder we need to see if it exists
             * on the remote server. It if does not exist we'll try to create it. If we
             * can't create we'll abort. This will happen on every single Pop3 folder as
             * designed and on Imap folders during error conditions. This allows us
             * to treat Pop3 and Imap the same in this code.
             */
            if (folder.equals(account.getTrashFolderName()) ||
                    folder.equals(account.getSentFolderName()) ||
                    folder.equals(account.getDraftsFolderName())) {
                if (!remoteFolder.exists()) {
                    if (!remoteFolder.create(FolderType.HOLDS_MESSAGES)) {
                        for (MessagingListener l : mListeners) {
                            l.synchronizeMailboxFinished(account, folder, 0, 0);
                        }
                        return;
                    }
                }
            }

            /*
             * Synchronization process:
                Open the folder
                Upload any local messages that are marked as PENDING_UPLOAD (Drafts, Sent, Trash)
                Get the message count
                Get the list of the newest Email.DEFAULT_VISIBLE_LIMIT messages
                    getMessages(messageCount - Email.DEFAULT_VISIBLE_LIMIT, messageCount)
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
            remoteFolder.open(OpenMode.READ_WRITE);

            /*
             * Trash any remote messages that are marked as trashed locally.
             */

            /*
             * Get the remote message count.
             */
            int remoteMessageCount = remoteFolder.getMessageCount();

            int visibleLimit = localFolder.getVisibleLimit();

            Message[] remoteMessages = new Message[0];
            final ArrayList<Message> unsyncedMessages = new ArrayList<Message>();
            HashMap<String, Message> remoteUidMap = new HashMap<String, Message>();

            if (remoteMessageCount > 0) {
                /*
                 * Message numbers start at 1.
                 */
                int remoteStart = Math.max(0, remoteMessageCount - visibleLimit) + 1;
                int remoteEnd = remoteMessageCount;
                remoteMessages = remoteFolder.getMessages(remoteStart, remoteEnd, null);
                for (Message message : remoteMessages) {
                    remoteUidMap.put(message.getUid(), message);
                }

                /*
                 * Get a list of the messages that are in the remote list but not on the
                 * local store, or messages that are in the local store but failed to download
                 * on the last sync. These are the new messages that we will download.
                 */
                for (Message message : remoteMessages) {
                    Message localMessage = localUidMap.get(message.getUid());
                    if (localMessage == null ||
                            (!localMessage.isSet(Flag.X_DOWNLOADED_FULL) &&
                            !localMessage.isSet(Flag.X_DOWNLOADED_PARTIAL))) {
                        unsyncedMessages.add(message);
                    }
                }
            }

            /*
             * A list of messages that were downloaded and which did not have the Seen flag set.
             * This will serve to indicate the true "new" message count that will be reported to
             * the user via notification.
             */
            final ArrayList<Message> newMessages = new ArrayList<Message>();

            /*
             * Fetch the flags and envelope only of the new messages. This is intended to get us
s             * critical data as fast as possible, and then we'll fill in the details.
             */
            if (unsyncedMessages.size() > 0) {

                /*
                 * Reverse the order of the messages. Depending on the server this may get us
                 * fetch results for newest to oldest. If not, no harm done.
                 */
                Collections.reverse(unsyncedMessages);

                FetchProfile fp = new FetchProfile();
                fp.add(FetchProfile.Item.FLAGS);
                fp.add(FetchProfile.Item.ENVELOPE);
                remoteFolder.fetch(unsyncedMessages.toArray(new Message[0]), fp,
                        new MessageRetrievalListener() {
                            public void messageFinished(Message message, int number, int ofTotal) {
                                try {
                                    // Store the new message locally
                                    localFolder.appendMessages(new Message[] {
                                        message
                                    });

                                    // And include it in the view
                                    if (message.getSubject() != null &&
                                            message.getFrom() != null) {
                                        /*
                                         * We check to make sure that we got something worth
                                         * showing (subject and from) because some protocols
                                         * (POP) may not be able to give us headers for
                                         * ENVELOPE, only size.
                                         */
                                        for (MessagingListener l : mListeners) {
                                            l.synchronizeMailboxNewMessage(account, folder,
                                                    localFolder.getMessage(message.getUid()));
                                        }
                                    }

                                    if (!message.isSet(Flag.SEEN)) {
                                        newMessages.add(message);
                                    }
                                }
                                catch (Exception e) {
                                    Log.e(Email.LOG_TAG,
                                            "Error while storing downloaded message.",
                                            e);
                                }
                            }

                            public void messageStarted(String uid, int number, int ofTotal) {
                            }
                        });
            }

            /*
             * Refresh the flags for any messages in the local store that we didn't just
             * download.
             */
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.FLAGS);
            remoteFolder.fetch(remoteMessages, fp, null);
            for (Message remoteMessage : remoteMessages) {
                Message localMessage = localFolder.getMessage(remoteMessage.getUid());
                if (localMessage == null) {
                    continue;
                }
                if (remoteMessage.isSet(Flag.SEEN) != localMessage.isSet(Flag.SEEN)) {
                    localMessage.setFlag(Flag.SEEN, remoteMessage.isSet(Flag.SEEN));
                    for (MessagingListener l : mListeners) {
                        l.synchronizeMailboxNewMessage(account, folder, localMessage);
                    }
                }
            }

            /*
             * Get and store the unread message count.
             */
            int remoteUnreadMessageCount = remoteFolder.getUnreadMessageCount();
            if (remoteUnreadMessageCount == -1) {
                localFolder.setUnreadMessageCount(localFolder.getUnreadMessageCount()
                        + newMessages.size());
            }
            else {
                localFolder.setUnreadMessageCount(remoteUnreadMessageCount);
            }

            /*
             * Remove any messages that are in the local store but no longer on the remote store.
             */
            for (Message localMessage : localMessages) {
                if (remoteUidMap.get(localMessage.getUid()) == null) {
                    localMessage.setFlag(Flag.X_DESTROYED, true);
                    for (MessagingListener l : mListeners) {
                        l.synchronizeMailboxRemovedMessage(account, folder, localMessage);
                    }
                }
            }

            /*
             * Now we download the actual content of messages.
             */
            ArrayList<Message> largeMessages = new ArrayList<Message>();
            ArrayList<Message> smallMessages = new ArrayList<Message>();
            for (Message message : unsyncedMessages) {
                /*
                 * Sort the messages into two buckets, small and large. Small messages will be
                 * downloaded fully and large messages will be downloaded in parts. By sorting
                 * into two buckets we can pipeline the commands for each set of messages
                 * into a single command to the server saving lots of round trips.
                 */
                if (message.getSize() > (MAX_SMALL_MESSAGE_SIZE)) {
                    largeMessages.add(message);
                } else {
                    smallMessages.add(message);
                }
            }
            /*
             * Grab the content of the small messages first. This is going to
             * be very fast and at very worst will be a single up of a few bytes and a single
             * download of 625k.
             */
            fp = new FetchProfile();
            fp.add(FetchProfile.Item.BODY);
            remoteFolder.fetch(smallMessages.toArray(new Message[smallMessages.size()]),
                    fp, new MessageRetrievalListener() {
                public void messageFinished(Message message, int number, int ofTotal) {
                    try {
                        // Store the updated message locally
                        localFolder.appendMessages(new Message[] {
                            message
                        });

                        Message localMessage = localFolder.getMessage(message.getUid());

                        // Set a flag indicating this message has now be fully downloaded
                        localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);

                        // Update the listener with what we've found
                        for (MessagingListener l : mListeners) {
                            l.synchronizeMailboxNewMessage(
                                    account,
                                    folder,
                                    localMessage);
                        }
                    }
                    catch (MessagingException me) {

                    }
                }

                public void messageStarted(String uid, int number, int ofTotal) {
                }
            });

            /*
             * Now do the large messages that require more round trips.
             */
            fp.clear();
            fp.add(FetchProfile.Item.STRUCTURE);
            remoteFolder.fetch(largeMessages.toArray(new Message[largeMessages.size()]),
                    fp, null);
            for (Message message : largeMessages) {
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
                    localFolder.appendMessages(new Message[] {
                        message
                    });

                    Message localMessage = localFolder.getMessage(message.getUid());

                    // Set a flag indicating that the message has been partially downloaded and
                    // is ready for view.
                    localMessage.setFlag(Flag.X_DOWNLOADED_PARTIAL, true);
                } else {
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
                    for (Part part : viewables) {
                        fp.clear();
                        fp.add(part);
                        // TODO what happens if the network connection dies? We've got partial
                        // messages with incorrect status stored.
                        remoteFolder.fetch(new Message[] { message }, fp, null);
                    }
                    // Store the updated message locally
                    localFolder.appendMessages(new Message[] {
                        message
                    });

                    Message localMessage = localFolder.getMessage(message.getUid());

                    // Set a flag indicating this message has been fully downloaded and can be
                    // viewed.
                    localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);
                }

                // Update the listener with what we've found
                for (MessagingListener l : mListeners) {
                    l.synchronizeMailboxNewMessage(
                            account,
                            folder,
                            localFolder.getMessage(message.getUid()));
                }
            }


            /*
             * Notify listeners that we're finally done.
             */
            for (MessagingListener l : mListeners) {
                l.synchronizeMailboxFinished(
                        account,
                        folder,
                        remoteFolder.getMessageCount(), newMessages.size());
            }

            remoteFolder.close(false);
            localFolder.close(false);
        }
        catch (Exception e) {
            if (Config.LOGV) {
                Log.v(Email.LOG_TAG, "synchronizeMailbox", e);
            }
            for (MessagingListener l : mListeners) {
                l.synchronizeMailboxFailed(
                        account,
                        folder,
                        e.getMessage());
            }
        }
    }

    private void queuePendingCommand(Account account, PendingCommand command) {
        try {
            LocalStore localStore = (LocalStore) Store.getInstance(
                    account.getLocalStoreUri(),
                    mApplication);
            localStore.addPendingCommand(command);
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to enqueue pending command", e);
        }
    }

    private void processPendingCommands(final Account account) {
        put("processPendingCommands", null, new Runnable() {
            public void run() {
                try {
                    processPendingCommandsSynchronous(account);
                }
                catch (MessagingException me) {
                    if (Config.LOGV) {
                        Log.v(Email.LOG_TAG, "processPendingCommands", me);
                    }
                    /*
                     * Ignore any exceptions from the commands. Commands will be processed
                     * on the next round.
                     */
                }
            }
        });
    }

    private void processPendingCommandsSynchronous(Account account) throws MessagingException {
        LocalStore localStore = (LocalStore) Store.getInstance(
                account.getLocalStoreUri(),
                mApplication);
        ArrayList<PendingCommand> commands = localStore.getPendingCommands();
        for (PendingCommand command : commands) {
            /*
             * We specifically do not catch any exceptions here. If a command fails it is
             * most likely due to a server or IO error and it must be retried before any
             * other command processes. This maintains the order of the commands.
             */
            if (PENDING_COMMAND_APPEND.equals(command.command)) {
                processPendingAppend(command, account);
            }
            else if (PENDING_COMMAND_MARK_READ.equals(command.command)) {
                processPendingMarkRead(command, account);
            }
            else if (PENDING_COMMAND_TRASH.equals(command.command)) {
                processPendingTrash(command, account);
            }
            localStore.removePendingCommand(command);
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
        String folder = command.arguments[0];
        String uid = command.arguments[1];

        LocalStore localStore = (LocalStore) Store.getInstance(
                account.getLocalStoreUri(),
                mApplication);
        LocalFolder localFolder = (LocalFolder) localStore.getFolder(folder);
        LocalMessage localMessage = (LocalMessage) localFolder.getMessage(uid);

        if (localMessage == null) {
            return;
        }

        Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
        Folder remoteFolder = remoteStore.getFolder(folder);
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
        if (!localMessage.getUid().startsWith("Local")
                && !localMessage.getUid().contains("-")) {
            remoteMessage = remoteFolder.getMessage(localMessage.getUid());
        }

        if (remoteMessage == null) {
            /*
             * If the message does not exist remotely we just upload it and then
             * update our local copy with the new uid.
             */
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.BODY);
            localFolder.fetch(new Message[] { localMessage }, fp, null);
            String oldUid = localMessage.getUid();
            remoteFolder.appendMessages(new Message[] { localMessage });
            localFolder.changeUid(localMessage);
            for (MessagingListener l : mListeners) {
                l.messageUidChanged(account, folder, oldUid, localMessage.getUid());
            }
        }
        else {
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
            if (remoteDate.compareTo(localDate) > 0) {
                /*
                 * If the remote message is newer than ours we'll just
                 * delete ours and move on. A sync will get the server message
                 * if we need to be able to see it.
                 */
                localMessage.setFlag(Flag.DELETED, true);
            }
            else {
                /*
                 * Otherwise we'll upload our message and then delete the remote message.
                 */
                fp.clear();
                fp = new FetchProfile();
                fp.add(FetchProfile.Item.BODY);
                localFolder.fetch(new Message[] { localMessage }, fp, null);
                String oldUid = localMessage.getUid();
                remoteFolder.appendMessages(new Message[] { localMessage });
                localFolder.changeUid(localMessage);
                for (MessagingListener l : mListeners) {
                    l.messageUidChanged(account, folder, oldUid, localMessage.getUid());
                }
                remoteMessage.setFlag(Flag.DELETED, true);
            }
        }
    }

    /**
     * Process a pending trash message command.
     *
     * @param command arguments = (String folder, String uid)
     * @param account
     * @throws MessagingException
     */
    private void processPendingTrash(PendingCommand command, Account account)
            throws MessagingException {
        String folder = command.arguments[0];
        String uid = command.arguments[1];

        Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
        Folder remoteFolder = remoteStore.getFolder(folder);
        if (!remoteFolder.exists()) {
            return;
        }
        remoteFolder.open(OpenMode.READ_WRITE);
        if (remoteFolder.getMode() != OpenMode.READ_WRITE) {
            return;
        }

        Message remoteMessage = null;
        if (!uid.startsWith("Local")
                && !uid.contains("-")) {
            remoteMessage = remoteFolder.getMessage(uid);
        }
        if (remoteMessage == null) {
            return;
        }

        Folder remoteTrashFolder = remoteStore.getFolder(account.getTrashFolderName());
        /*
         * Attempt to copy the remote message to the remote trash folder.
         */
        if (!remoteTrashFolder.exists()) {
            /*
             * If the remote trash folder doesn't exist we try to create it.
             */
            remoteTrashFolder.create(FolderType.HOLDS_MESSAGES);
        }

        if (remoteTrashFolder.exists()) {
            remoteFolder.copyMessages(new Message[] { remoteMessage }, remoteTrashFolder);
        }

        remoteMessage.setFlag(Flag.DELETED, true);
        remoteFolder.expunge();
    }

    /**
     * Processes a pending mark read or unread command.
     *
     * @param command arguments = (String folder, String uid, boolean read)
     * @param account
     */
    private void processPendingMarkRead(PendingCommand command, Account account)
            throws MessagingException {
        String folder = command.arguments[0];
        String uid = command.arguments[1];
        boolean read = Boolean.parseBoolean(command.arguments[2]);

        Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
        Folder remoteFolder = remoteStore.getFolder(folder);
        if (!remoteFolder.exists()) {
            return;
        }
        remoteFolder.open(OpenMode.READ_WRITE);
        if (remoteFolder.getMode() != OpenMode.READ_WRITE) {
            return;
        }
        Message remoteMessage = null;
        if (!uid.startsWith("Local")
                && !uid.contains("-")) {
            remoteMessage = remoteFolder.getMessage(uid);
        }
        if (remoteMessage == null) {
            return;
        }
        remoteMessage.setFlag(Flag.SEEN, read);
    }

    /**
     * Mark the message with the given account, folder and uid either Seen or not Seen.
     * @param account
     * @param folder
     * @param uid
     * @param seen
     */
    public void markMessageRead(
            final Account account,
            final String folder,
            final String uid,
            final boolean seen) {
        try {
            Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
            Folder localFolder = localStore.getFolder(folder);
            localFolder.open(OpenMode.READ_WRITE);

            Message message = localFolder.getMessage(uid);
            message.setFlag(Flag.SEEN, seen);
            PendingCommand command = new PendingCommand();
            command.command = PENDING_COMMAND_MARK_READ;
            command.arguments = new String[] { folder, uid, Boolean.toString(seen) };
            queuePendingCommand(account, command);
            processPendingCommands(account);
        }
        catch (MessagingException me) {
            throw new RuntimeException(me);
        }
    }

    private void loadMessageForViewRemote(final Account account, final String folder,
            final String uid, MessagingListener listener) {
        put("loadMessageForViewRemote", listener, new Runnable() {
            public void run() {
                try {
                    Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
                    LocalFolder localFolder = (LocalFolder) localStore.getFolder(folder);
                    localFolder.open(OpenMode.READ_WRITE);

                    Message message = localFolder.getMessage(uid);

                    if (message.isSet(Flag.X_DOWNLOADED_FULL)) {
                        /*
                         * If the message has been synchronized since we were called we'll
                         * just hand it back cause it's ready to go.
                         */
                        FetchProfile fp = new FetchProfile();
                        fp.add(FetchProfile.Item.ENVELOPE);
                        fp.add(FetchProfile.Item.BODY);
                        localFolder.fetch(new Message[] { message }, fp, null);

                        for (MessagingListener l : mListeners) {
                            l.loadMessageForViewBodyAvailable(account, folder, uid, message);
                        }
                        for (MessagingListener l : mListeners) {
                            l.loadMessageForViewFinished(account, folder, uid, message);
                        }
                        localFolder.close(false);
                        return;
                    }

                    /*
                     * At this point the message is not available, so we need to download it
                     * fully if possible.
                     */

                    Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
                    Folder remoteFolder = remoteStore.getFolder(folder);
                    remoteFolder.open(OpenMode.READ_WRITE);

                    // Get the remote message and fully download it
                    Message remoteMessage = remoteFolder.getMessage(uid);
                    FetchProfile fp = new FetchProfile();
                    fp.add(FetchProfile.Item.BODY);
                    remoteFolder.fetch(new Message[] { remoteMessage }, fp, null);

                    // Store the message locally and load the stored message into memory
                    localFolder.appendMessages(new Message[] { remoteMessage });
                    message = localFolder.getMessage(uid);
                    localFolder.fetch(new Message[] { message }, fp, null);

                    // This is a view message request, so mark it read
                    if (!message.isSet(Flag.SEEN)) {
                        markMessageRead(account, folder, uid, true);
                    }

                    // Mark that this message is now fully synched
                    message.setFlag(Flag.X_DOWNLOADED_FULL, true);

                    for (MessagingListener l : mListeners) {
                        l.loadMessageForViewBodyAvailable(account, folder, uid, message);
                    }
                    for (MessagingListener l : mListeners) {
                        l.loadMessageForViewFinished(account, folder, uid, message);
                    }
                    remoteFolder.close(false);
                    localFolder.close(false);
                }
                catch (Exception e) {
                    for (MessagingListener l : mListeners) {
                        l.loadMessageForViewFailed(account, folder, uid, e.getMessage());
                    }
                }
            }
        });
    }

    public void loadMessageForView(final Account account, final String folder, final String uid,
            MessagingListener listener) {
        for (MessagingListener l : mListeners) {
            l.loadMessageForViewStarted(account, folder, uid);
        }
        try {
            Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
            LocalFolder localFolder = (LocalFolder) localStore.getFolder(folder);
            localFolder.open(OpenMode.READ_WRITE);

            Message message = localFolder.getMessage(uid);

            for (MessagingListener l : mListeners) {
                l.loadMessageForViewHeadersAvailable(account, folder, uid, message);
            }

            if (!message.isSet(Flag.X_DOWNLOADED_FULL)) {
                loadMessageForViewRemote(account, folder, uid, listener);
                localFolder.close(false);
                return;
            }

            if (!message.isSet(Flag.SEEN)) {
                markMessageRead(account, folder, uid, true);
            }

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.BODY);
            localFolder.fetch(new Message[] {
                message
            }, fp, null);

            for (MessagingListener l : mListeners) {
                l.loadMessageForViewBodyAvailable(account, folder, uid, message);
            }

            for (MessagingListener l : mListeners) {
                l.loadMessageForViewFinished(account, folder, uid, message);
            }
            localFolder.close(false);
        }
        catch (Exception e) {
            for (MessagingListener l : mListeners) {
                l.loadMessageForViewFailed(account, folder, uid, e.getMessage());
            }
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
            MessagingListener listener) {
        /*
         * Check if the attachment has already been downloaded. If it has there's no reason to
         * download it, so we just tell the listener that it's ready to go.
         */
        try {
            if (part.getBody() != null) {
                for (MessagingListener l : mListeners) {
                    l.loadAttachmentStarted(account, message, part, tag, false);
                }

                for (MessagingListener l : mListeners) {
                    l.loadAttachmentFinished(account, message, part, tag);
                }
                return;
            }
        }
        catch (MessagingException me) {
            /*
             * If the header isn't there the attachment isn't downloaded yet, so just continue
             * on.
             */
        }

        for (MessagingListener l : mListeners) {
            l.loadAttachmentStarted(account, message, part, tag, true);
        }

        put("loadAttachment", listener, new Runnable() {
            public void run() {
                try {
                    LocalStore localStore =
                        (LocalStore) Store.getInstance(account.getLocalStoreUri(), mApplication);
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
                    for (Part attachment : attachments) {
                        attachment.setBody(null);
                    }
                    Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
                    LocalFolder localFolder =
                        (LocalFolder) localStore.getFolder(message.getFolder().getName());
                    Folder remoteFolder = remoteStore.getFolder(message.getFolder().getName());
                    remoteFolder.open(OpenMode.READ_WRITE);

                    FetchProfile fp = new FetchProfile();
                    fp.add(part);
                    remoteFolder.fetch(new Message[] { message }, fp, null);
                    localFolder.updateMessage((LocalMessage)message);
                    localFolder.close(false);
                    for (MessagingListener l : mListeners) {
                        l.loadAttachmentFinished(account, message, part, tag);
                    }
                }
                catch (MessagingException me) {
                    if (Config.LOGV) {
                        Log.v(Email.LOG_TAG, "", me);
                    }
                    for (MessagingListener l : mListeners) {
                        l.loadAttachmentFailed(account, message, part, tag, me.getMessage());
                    }
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
            Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
            LocalFolder localFolder =
                (LocalFolder) localStore.getFolder(account.getOutboxFolderName());
            localFolder.open(OpenMode.READ_WRITE);
            localFolder.appendMessages(new Message[] {
                message
            });
            Message localMessage = localFolder.getMessage(message.getUid());
            localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);
            localFolder.close(false);
            sendPendingMessages(account, null);
        }
        catch (Exception e) {
            for (MessagingListener l : mListeners) {
                // TODO general failed
            }
        }
    }

    /**
     * Attempt to send any messages that are sitting in the Outbox.
     * @param account
     * @param listener
     */
    public void sendPendingMessages(final Account account,
            MessagingListener listener) {
        put("sendPendingMessages", listener, new Runnable() {
            public void run() {
                sendPendingMessagesSynchronous(account);
            }
        });
    }

    /**
     * Attempt to send any messages that are sitting in the Outbox.
     * @param account
     * @param listener
     */
    public void sendPendingMessagesSynchronous(final Account account) {
        try {
            Store localStore = Store.getInstance(
                    account.getLocalStoreUri(),
                    mApplication);
            Folder localFolder = localStore.getFolder(
                    account.getOutboxFolderName());
            if (!localFolder.exists()) {
                return;
            }
            localFolder.open(OpenMode.READ_WRITE);

            Message[] localMessages = localFolder.getMessages(null);

            /*
             * The profile we will use to pull all of the content
             * for a given local message into memory for sending.
             */
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.BODY);

            LocalFolder localSentFolder =
                (LocalFolder) localStore.getFolder(
                        account.getSentFolderName());

            Transport transport = Transport.getInstance(account.getTransportUri());
            for (Message message : localMessages) {
                try {
                    localFolder.fetch(new Message[] { message }, fp, null);
                    try {
                        message.setFlag(Flag.X_SEND_IN_PROGRESS, true);
                        transport.sendMessage(message);
                        message.setFlag(Flag.X_SEND_IN_PROGRESS, false);
                        localFolder.copyMessages(
                                new Message[] { message },
                                localSentFolder);

                        PendingCommand command = new PendingCommand();
                        command.command = PENDING_COMMAND_APPEND;
                        command.arguments =
                            new String[] {
                                localSentFolder.getName(),
                                message.getUid() };
                        queuePendingCommand(account, command);
                        processPendingCommands(account);
                        message.setFlag(Flag.X_DESTROYED, true);
                    }
                    catch (Exception e) {
                        message.setFlag(Flag.X_SEND_FAILED, true);
                    }
                }
                catch (Exception e) {
                    /*
                     * We ignore this exception because a future refresh will retry this
                     * message.
                     */
                }
            }
            localFolder.expunge();
            if (localFolder.getMessageCount() == 0) {
                localFolder.delete(false);
            }
            for (MessagingListener l : mListeners) {
                l.sendPendingMessagesCompleted(account);
            }
        }
        catch (Exception e) {
            for (MessagingListener l : mListeners) {
                // TODO general failed
            }
        }
    }

    /**
     * We do the local portion of this synchronously because other activities may have to make
     * updates based on what happens here
     * @param account
     * @param folder
     * @param message
     * @param listener
     */
    public void deleteMessage(final Account account, final String folder, final Message message,
            MessagingListener listener) {
        if (folder.equals(account.getTrashFolderName())) {
            return;
        }
        try {
            Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
            Folder localFolder = localStore.getFolder(folder);
            Folder localTrashFolder = localStore.getFolder(account.getTrashFolderName());

            localFolder.copyMessages(new Message[] { message }, localTrashFolder);
            message.setFlag(Flag.DELETED, true);

            if (account.getDeletePolicy() == Account.DELETE_POLICY_ON_DELETE) {
                PendingCommand command = new PendingCommand();
                command.command = PENDING_COMMAND_TRASH;
                command.arguments = new String[] { folder, message.getUid() };
                queuePendingCommand(account, command);
                processPendingCommands(account);
            }
        }
        catch (MessagingException me) {
            throw new RuntimeException("Error deleting message from local store.", me);
        }
    }

    public void emptyTrash(final Account account, MessagingListener listener) {
        put("emptyTrash", listener, new Runnable() {
            public void run() {
                // TODO IMAP
                try {
                    Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
                    Folder localFolder = localStore.getFolder(account.getTrashFolderName());
                    localFolder.open(OpenMode.READ_WRITE);
                    Message[] messages = localFolder.getMessages(null);
                    localFolder.setFlags(messages, new Flag[] {
                        Flag.DELETED
                    }, true);
                    localFolder.close(true);
                    for (MessagingListener l : mListeners) {
                        l.emptyTrashCompleted(account);
                    }
                }
                catch (Exception e) {
                    // TODO
                    if (Config.LOGV) {
                        Log.v(Email.LOG_TAG, "emptyTrash");
                    }
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
            final MessagingListener listener) {
        for (MessagingListener l : mListeners) {
            l.checkMailStarted(context, account);
        }
        put("checkMail", listener, new Runnable() {
            public void run() {
                Account[] accounts;
                if (account != null) {
                    accounts = new Account[] {
                        account
                    };
                } else {
                    accounts = Preferences.getPreferences(context).getAccounts();
                }
                for (Account account : accounts) {
                    sendPendingMessagesSynchronous(account);
                    synchronizeMailboxSyncronous(account, Email.INBOX);
                }
                for (MessagingListener l : mListeners) {
                    l.checkMailFinished(context, account);
                }
            }
        });
    }

    public void saveDraft(final Account account, final Message message) {
        try {
            Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
            LocalFolder localFolder =
                (LocalFolder) localStore.getFolder(account.getDraftsFolderName());
            localFolder.open(OpenMode.READ_WRITE);
            localFolder.appendMessages(new Message[] {
                message
            });
            Message localMessage = localFolder.getMessage(message.getUid());
            localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);

            PendingCommand command = new PendingCommand();
            command.command = PENDING_COMMAND_APPEND;
            command.arguments = new String[] {
                    localFolder.getName(),
                    localMessage.getUid() };
            queuePendingCommand(account, command);
            processPendingCommands(account);
        }
        catch (MessagingException e) {
            Log.e(Email.LOG_TAG, "Unable to save message as draft.", e);
        }
    }

    class Command {
        public Runnable runnable;

        public MessagingListener listener;

        public String description;
    }
}
