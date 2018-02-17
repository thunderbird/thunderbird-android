package com.fsck.k9.controller.imap;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.Account.Expunge;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.UidReverseComparator;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.BodyFactory;
import com.fsck.k9.mail.DefaultBodyFactory;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Folder.FolderType;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalFolder.MoreMessages;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.MessageRemovalListener;
import com.fsck.k9.notification.NotificationController;
import timber.log.Timber;

import static com.fsck.k9.helper.ExceptionHelper.getRootCauseMessage;


class ImapSync {
    private final NotificationController notificationController;
    private final MessagingController controller;
    private final Context context;


    // TODO: Replace all of these dependencies with one or more interfaces
    ImapSync(NotificationController notificationController, MessagingController controller, Context context) {
        this.notificationController = notificationController;
        this.controller = controller;
        this.context = context;
    }

    void sync(Account account, String folder, MessagingListener listener, Folder providedRemoteFolder) {
        synchronizeMailboxSynchronous(account, folder, listener, providedRemoteFolder);
    }

    void synchronizeMailboxSynchronous(final Account account, final String folder, final MessagingListener listener,
            Folder providedRemoteFolder) {
        Folder remoteFolder = null;
        LocalFolder tLocalFolder = null;

        Timber.i("Synchronizing folder %s:%s", account.getDescription(), folder);

        for (MessagingListener l : getListeners(listener)) {
            l.synchronizeMailboxStarted(account, folder);
        }
        /*
         * We don't ever sync the Outbox
         */
        if (folder.equals(account.getOutboxFolderName())) {
            for (MessagingListener l : getListeners(listener)) {
                l.synchronizeMailboxFinished(account, folder, 0, 0);
            }

            return;
        }

        Exception commandException = null;
        try {
            Timber.d("SYNC: About to process pending commands for account %s", account.getDescription());

            try {
                processPendingCommandsSynchronous(account);
            } catch (Exception e) {
                Timber.e(e, "Failure processing command, but allow message sync attempt");
                commandException = e;
            }

            /*
             * Get the message list from the local store and create an index of
             * the uids within the list.
             */
            Timber.v("SYNC: About to get local folder %s", folder);

            final LocalStore localStore = account.getLocalStore();
            tLocalFolder = localStore.getFolder(folder);
            final LocalFolder localFolder = tLocalFolder;
            localFolder.open(Folder.OPEN_MODE_RW);
            localFolder.updateLastUid();
            Map<String, Long> localUidMap = localFolder.getAllMessagesAndEffectiveDates();

            if (providedRemoteFolder != null) {
                Timber.v("SYNC: using providedRemoteFolder %s", folder);
                remoteFolder = providedRemoteFolder;
            } else {
                Store remoteStore = account.getRemoteStore();

                Timber.v("SYNC: About to get remote folder %s", folder);
                remoteFolder = remoteStore.getFolder(folder);

                if (!verifyOrCreateRemoteSpecialFolder(account, folder, remoteFolder, listener)) {
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
                Timber.v("SYNC: About to open remote folder %s", folder);

                if (Expunge.EXPUNGE_ON_POLL == account.getExpungePolicy()) {
                    Timber.d("SYNC: Expunging folder %s:%s", account.getDescription(), folder);
                    remoteFolder.expunge();
                }
                remoteFolder.open(Folder.OPEN_MODE_RO);

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

            Timber.v("SYNC: Remote message count for folder %s is %d", folder, remoteMessageCount);

            final Date earliestDate = account.getEarliestPollDate();
            long earliestTimestamp = earliestDate != null ? earliestDate.getTime() : 0L;


            int remoteStart = 1;
            if (remoteMessageCount > 0) {
                /* Message numbers start at 1.  */
                if (visibleLimit > 0) {
                    remoteStart = Math.max(0, remoteMessageCount - visibleLimit) + 1;
                } else {
                    remoteStart = 1;
                }

                Timber.v("SYNC: About to get messages %d through %d for folder %s",
                        remoteStart, remoteMessageCount, folder);

                final AtomicInteger headerProgress = new AtomicInteger(0);
                for (MessagingListener l : getListeners(listener)) {
                    l.synchronizeMailboxHeadersStarted(account, folder);
                }


                List<? extends Message> remoteMessageArray =
                        remoteFolder.getMessages(remoteStart, remoteMessageCount, earliestDate, null);

                int messageCount = remoteMessageArray.size();

                for (Message thisMess : remoteMessageArray) {
                    headerProgress.incrementAndGet();
                    for (MessagingListener l : getListeners(listener)) {
                        l.synchronizeMailboxHeadersProgress(account, folder, headerProgress.get(), messageCount);
                    }
                    Long localMessageTimestamp = localUidMap.get(thisMess.getUid());
                    if (localMessageTimestamp == null || localMessageTimestamp >= earliestTimestamp) {
                        remoteMessages.add(thisMess);
                        remoteUidMap.put(thisMess.getUid(), thisMess);
                    }
                }

                Timber.v("SYNC: Got %d messages for folder %s", remoteUidMap.size(), folder);

                for (MessagingListener l : getListeners(listener)) {
                    l.synchronizeMailboxHeadersFinished(account, folder, headerProgress.get(), remoteUidMap.size());
                }

            } else if (remoteMessageCount < 0) {
                throw new Exception("Message count " + remoteMessageCount + " for folder " + folder);
            }

            /*
             * Remove any messages that are in the local store but no longer on the remote store or are too old
             */
            MoreMessages moreMessages = localFolder.getMoreMessages();
            if (account.syncRemoteDeletions()) {
                List<String> destroyMessageUids = new ArrayList<>();
                for (String localMessageUid : localUidMap.keySet()) {
                    if (!localMessageUid.startsWith(K9.LOCAL_UID_PREFIX) && remoteUidMap.get(localMessageUid) == null) {
                        destroyMessageUids.add(localMessageUid);
                    }
                }

                List<LocalMessage> destroyMessages = localFolder.getMessagesByUids(destroyMessageUids);
                if (!destroyMessageUids.isEmpty()) {
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
            localUidMap = null;

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

            Timber.d("Done synchronizing folder %s:%s @ %tc with %d new messages",
                    account.getDescription(),
                    folder,
                    System.currentTimeMillis(),
                    newMessages);

            for (MessagingListener l : getListeners(listener)) {
                l.synchronizeMailboxFinished(account, folder, remoteMessageCount, newMessages);
            }


            if (commandException != null) {
                String rootMessage = getRootCauseMessage(commandException);
                Timber.e("Root cause failure in %s:%s was '%s'",
                        account.getDescription(), tLocalFolder.getName(), rootMessage);
                localFolder.setStatus(rootMessage);
                for (MessagingListener l : getListeners(listener)) {
                    l.synchronizeMailboxFailed(account, folder, rootMessage);
                }
            }

            Timber.i("Done synchronizing folder %s:%s", account.getDescription(), folder);

        } catch (AuthenticationFailedException e) {
            handleAuthenticationFailure(account, true);

            for (MessagingListener l : getListeners(listener)) {
                l.synchronizeMailboxFailed(account, folder, "Authentication failure");
            }
        } catch (Exception e) {
            Timber.e(e, "synchronizeMailbox");
            // If we don't set the last checked, it can try too often during
            // failure conditions
            String rootMessage = getRootCauseMessage(e);
            if (tLocalFolder != null) {
                try {
                    tLocalFolder.setStatus(rootMessage);
                    tLocalFolder.setLastChecked(System.currentTimeMillis());
                } catch (MessagingException me) {
                    Timber.e(e, "Could not set last checked on folder %s:%s",
                            account.getDescription(), tLocalFolder.getName());
                }
            }

            for (MessagingListener l : getListeners(listener)) {
                l.synchronizeMailboxFailed(account, folder, rootMessage);
            }
            notifyUserIfCertificateProblem(account, e, true);
            Timber.e("Failed synchronizing folder %s:%s @ %tc", account.getDescription(), folder,
                    System.currentTimeMillis());

        } finally {
            if (providedRemoteFolder == null) {
                closeFolder(remoteFolder);
            }

            closeFolder(tLocalFolder);
        }

    }

    /*
     * If the folder is a "special" folder we need to see if it exists
     * on the remote server. It if does not exist we'll try to create it. If we
     * can't create we'll abort.
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

                    Timber.i("Done synchronizing folder %s", folder);
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
     *         The account the remote store belongs to.
     * @param remoteFolder
     *         The remote folder to download messages from.
     * @param localFolder
     *         The {@link LocalFolder} instance corresponding to the remote folder.
     * @param inputMessages
     *         A list of messages objects that store the UIDs of which messages to download.
     * @param flagSyncOnly
     *         Only flags will be fetched from the remote store if this is {@code true}.
     * @param purgeToVisibleLimit
     *         If true, local messages will be purged down to the limit of visible messages.
     *
     * @return The number of downloaded messages that are not flagged as {@link Flag#SEEN}.
     *
     * @throws MessagingException
     */
    int downloadMessages(final Account account, final Folder remoteFolder,
            final LocalFolder localFolder, List<Message> inputMessages,
            boolean flagSyncOnly, boolean purgeToVisibleLimit) throws MessagingException {

        final Date earliestDate = account.getEarliestPollDate();
        Date downloadStarted = new Date(); // now

        if (earliestDate != null) {
            Timber.d("Only syncing messages after %s", earliestDate);
        }
        final String folder = remoteFolder.getName();

        int unreadBeforeStart = 0;
        try {
            AccountStats stats = account.getStats(context);
            unreadBeforeStart = stats.unreadMessageCount;

        } catch (MessagingException e) {
            Timber.e(e, "Unable to getUnreadMessageCount for account: %s", account);
        }

        List<Message> syncFlagMessages = new ArrayList<>();
        List<Message> unsyncedMessages = new ArrayList<>();
        final AtomicInteger newMessages = new AtomicInteger(0);

        List<Message> messages = new ArrayList<>(inputMessages);

        for (Message message : messages) {
            evaluateMessageForDownload(message, folder, localFolder, remoteFolder, account, unsyncedMessages,
                    syncFlagMessages, flagSyncOnly);
        }

        final AtomicInteger progress = new AtomicInteger(0);
        final int todo = unsyncedMessages.size() + syncFlagMessages.size();
        for (MessagingListener l : getListeners()) {
            l.synchronizeMailboxProgress(account, folder, progress.get(), todo);
        }

        Timber.d("SYNC: Have %d unsynced messages", unsyncedMessages.size());

        messages.clear();
        final List<Message> largeMessages = new ArrayList<>();
        final List<Message> smallMessages = new ArrayList<>();
        if (!unsyncedMessages.isEmpty()) {
            Collections.sort(unsyncedMessages, new UidReverseComparator());
            int visibleLimit = localFolder.getVisibleLimit();
            int listSize = unsyncedMessages.size();

            if ((visibleLimit > 0) && (listSize > visibleLimit)) {
                unsyncedMessages = unsyncedMessages.subList(0, visibleLimit);
            }

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.FLAGS);
            fp.add(FetchProfile.Item.ENVELOPE);

            Timber.d("SYNC: About to fetch %d unsynced messages for folder %s", unsyncedMessages.size(), folder);

            fetchUnsyncedMessages(account, remoteFolder, unsyncedMessages, smallMessages, largeMessages, progress, todo,
                    fp);

            String updatedPushState = localFolder.getPushState();
            for (Message message : unsyncedMessages) {
                String newPushState = remoteFolder.getNewPushState(updatedPushState, message);
                if (newPushState != null) {
                    updatedPushState = newPushState;
                }
            }
            localFolder.setPushState(updatedPushState);

            Timber.d("SYNC: Synced unsynced messages for folder %s", folder);
        }

        Timber.d("SYNC: Have %d large messages and %d small messages out of %d unsynced messages",
                largeMessages.size(), smallMessages.size(), unsyncedMessages.size());

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
        downloadSmallMessages(account, remoteFolder, localFolder, smallMessages, progress, unreadBeforeStart,
                newMessages, todo, fp);
        smallMessages.clear();
        /*
         * Now do the large messages that require more round trips.
         */
        fp = new FetchProfile();
        fp.add(FetchProfile.Item.STRUCTURE);
        downloadLargeMessages(account, remoteFolder, localFolder, largeMessages, progress, unreadBeforeStart,
                newMessages, todo, fp);
        largeMessages.clear();

        /*
         * Refresh the flags for any messages in the local store that we didn't just
         * download.
         */

        refreshLocalMessageFlags(account, remoteFolder, localFolder, syncFlagMessages, progress, todo);

        Timber.d("SYNC: Synced remote messages for folder %s, %d new messages", folder, newMessages.get());

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
            Timber.v("Message with uid %s is marked as deleted", message.getUid());

            syncFlagMessages.add(message);
            return;
        }

        Message localMessage = localFolder.getMessage(message.getUid());

        if (localMessage == null) {
            if (!flagSyncOnly) {
                if (!message.isSet(Flag.X_DOWNLOADED_FULL) && !message.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                    Timber.v("Message with uid %s has not yet been downloaded", message.getUid());

                    unsyncedMessages.add(message);
                } else {
                    Timber.v("Message with uid %s is partially or fully downloaded", message.getUid());

                    // Store the updated message locally
                    localFolder.appendMessages(Collections.singletonList(message));

                    localMessage = localFolder.getMessage(message.getUid());

                    localMessage.setFlag(Flag.X_DOWNLOADED_FULL, message.isSet(Flag.X_DOWNLOADED_FULL));
                    localMessage.setFlag(Flag.X_DOWNLOADED_PARTIAL, message.isSet(Flag.X_DOWNLOADED_PARTIAL));

                    for (MessagingListener l : getListeners()) {
                        if (!localMessage.isSet(Flag.SEEN)) {
                            l.synchronizeMailboxNewMessage(account, folder, localMessage);
                        }
                    }
                }
            }
        } else if (!localMessage.isSet(Flag.DELETED)) {
            Timber.v("Message with uid %s is present in the local store", message.getUid());

            if (!localMessage.isSet(Flag.X_DOWNLOADED_FULL) && !localMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                Timber.v("Message with uid %s is not downloaded, even partially; trying again", message.getUid());

                unsyncedMessages.add(message);
            } else {
                String newPushState = remoteFolder.getNewPushState(localFolder.getPushState(), message);
                if (newPushState != null) {
                    localFolder.setPushState(newPushState);
                }
                syncFlagMessages.add(message);
            }
        } else {
            Timber.v("Local copy of message with uid %s is marked as deleted", message.getUid());
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
                                if (K9.isDebug()) {
                                    if (message.isSet(Flag.DELETED)) {
                                        Timber.v("Newly downloaded message %s:%s:%s was marked deleted on server, " +
                                                "skipping", account, folder, message.getUid());
                                    } else {
                                        Timber.d("Newly downloaded message %s is older than %s, skipping",
                                                message.getUid(), earliestDate);
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
                            Timber.e(e, "Error while storing downloaded message.");
                        }
                    }

                    @Override
                    public void messageStarted(String uid, int number, int ofTotal) {
                    }

                    @Override
                    public void messagesFinished(int total) {
                        // FIXME this method is almost never invoked by various Stores! Don't rely on it unless fixed!!
                    }

                });
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

        Timber.d("SYNC: Fetching %d small messages for folder %s", smallMessages.size(), folder);

        remoteFolder.fetch(smallMessages,
                fp, new MessageRetrievalListener<T>() {
                    @Override
                    public void messageFinished(final T message, int number, int ofTotal) {
                        try {

                            if (!shouldImportMessage(message, earliestDate)) {
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

                            Timber.v("About to notify listeners that we got a new small message %s:%s:%s",
                                    account, folder, message.getUid());

                            // Update the listener with what we've found
                            for (MessagingListener l : getListeners()) {
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
                            Timber.e(me, "SYNC: fetch small messages");
                        }
                    }

                    @Override
                    public void messageStarted(String uid, int number, int ofTotal) {
                    }

                    @Override
                    public void messagesFinished(int total) {
                    }
                });

        Timber.d("SYNC: Done fetching small messages for folder %s", folder);
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

        Timber.d("SYNC: Fetching large messages for folder %s", folder);

        remoteFolder.fetch(largeMessages, fp, null);
        for (T message : largeMessages) {

            if (!shouldImportMessage(message, earliestDate)) {
                progress.incrementAndGet();
                continue;
            }

            if (message.getBody() == null) {
                downloadSaneBody(account, remoteFolder, localFolder, message);
            } else {
                downloadPartial(remoteFolder, localFolder, message);
            }

            Timber.v("About to notify listeners that we got a new large message %s:%s:%s",
                    account, folder, message.getUid());

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
        }

        Timber.d("SYNC: Done fetching large messages for folder %s", folder);
    }

    private void refreshLocalMessageFlags(final Account account, final Folder remoteFolder,
            final LocalFolder localFolder,
            List<Message> syncFlagMessages,
            final AtomicInteger progress,
            final int todo
    ) throws MessagingException {

        final String folder = remoteFolder.getName();
        Timber.d("SYNC: About to sync flags for %d remote messages for folder %s", syncFlagMessages.size(), folder);

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

    private void downloadSaneBody(Account account, Folder remoteFolder, LocalFolder localFolder, Message message)
            throws MessagingException {
        /*
         * The provider was unable to get the structure of the message, so
         * we'll download a reasonable portion of the messge and mark it as
         * incomplete so the entire thing can be downloaded later if the user
         * wishes to download it.
         */
        FetchProfile fp = new FetchProfile();
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
    }

    private void downloadPartial(Folder remoteFolder, LocalFolder localFolder, Message message)
            throws MessagingException {
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
        BodyFactory bodyFactory = new DefaultBodyFactory();
        for (Part part : viewables) {
            remoteFolder.fetchPart(message, part, null, bodyFactory);
        }
        // Store the updated message locally
        localFolder.appendMessages(Collections.singletonList(message));

        Message localMessage = localFolder.getMessage(message.getUid());

        // Set a flag indicating this message has been fully downloaded and can be
        // viewed.
        localMessage.setFlag(Flag.X_DOWNLOADED_PARTIAL, true);
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

    private boolean shouldImportMessage(Message message, Date earliestDate) {
        if (message.olderThan(earliestDate)) {
            Timber.d("Message %s is older than %s, hence not saving", message.getUid(), earliestDate);
            return false;
        }
        return true;
    }

    private static void closeFolder(Folder folder) {
        if (folder != null) {
            folder.close();
        }
    }


    /*
     * Methods calling back to MessagingController
     *
     * TODO: Move all of these to an interface so we don't have to depend on MessagingController directly
     */

    private void processPendingCommandsSynchronous(Account account) throws MessagingException {
        controller.processPendingCommandsSynchronous(account);
    }

    private Set<MessagingListener> getListeners() {
        return controller.getListeners();
    }

    private Set<MessagingListener> getListeners(MessagingListener listener) {
        return controller.getListeners(listener);
    }

    private void updateMoreMessages(Folder remoteFolder, LocalFolder localFolder, Date earliestDate, int remoteStart)
            throws IOException, MessagingException {
        controller.updateMoreMessages(remoteFolder, localFolder, earliestDate, remoteStart);
    }

    private void handleAuthenticationFailure(Account account, boolean incoming) {
        controller.handleAuthenticationFailure(account, incoming);
    }

    private void notifyUserIfCertificateProblem(Account account, Exception exception, boolean incoming) {
        controller.notifyUserIfCertificateProblem(account, exception, incoming);
    }

    private boolean shouldNotifyForMessage(Account account, LocalFolder localFolder, Message message) {
        return controller.shouldNotifyForMessage(account, localFolder, message);
    }

    private boolean isMessageSuppressed(LocalMessage message) {
        return controller.isMessageSuppressed(message);
    }
}
