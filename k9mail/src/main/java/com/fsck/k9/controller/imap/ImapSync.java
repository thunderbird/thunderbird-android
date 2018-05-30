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

import com.fsck.k9.Account;
import com.fsck.k9.Account.Expunge;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.SyncListener;
import com.fsck.k9.controller.UidReverseComparator;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.BodyFactory;
import com.fsck.k9.mail.DefaultBodyFactory;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.store.imap.ImapStore;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalFolder.MoreMessages;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.MessageRemovalListener;
import timber.log.Timber;

import static com.fsck.k9.helper.ExceptionHelper.getRootCauseMessage;


class ImapSync {
    private final Account account;
    private final LocalStore localStore;
    private final ImapStore imapStore;


    ImapSync(Account account, LocalStore localStore, ImapStore imapStore) {
        this.account = account;
        this.localStore = localStore;
        this.imapStore = imapStore;
    }

    void sync(String folder, SyncListener listener, Folder providedRemoteFolder) {
        synchronizeMailboxSynchronous(folder, listener, providedRemoteFolder);
    }

    void synchronizeMailboxSynchronous(final String folder, final SyncListener listener, Folder providedRemoteFolder) {
        Folder remoteFolder = null;
        LocalFolder tLocalFolder = null;

        Timber.i("Synchronizing folder %s:%s", account.getDescription(), folder);

        // We don't ever sync the Outbox
        if (folder.equals(account.getOutboxFolder())) {
            return;
        }

        try {
            Timber.d("SYNC: About to process pending commands for account %s", account.getDescription());

            Timber.v("SYNC: About to get local folder %s", folder);
            tLocalFolder = localStore.getFolder(folder);
            final LocalFolder localFolder = tLocalFolder;
            localFolder.open(Folder.OPEN_MODE_RW);
            String folderName = localFolder.getName();

            listener.syncStarted(folder, folderName);

            /*
             * Get the message list from the local store and create an index of
             * the uids within the list.
             */

            localFolder.updateLastUid();
            Integer lastUid = localFolder.getLastUid();

            Map<String, Long> localUidMap = localFolder.getAllMessagesAndEffectiveDates();

            if (providedRemoteFolder != null) {
                Timber.v("SYNC: using providedRemoteFolder %s", folder);
                remoteFolder = providedRemoteFolder;
            } else {
                Timber.v("SYNC: About to get remote folder %s", folder);
                remoteFolder = imapStore.getFolder(folder);

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

            listener.syncAuthenticationSuccess();

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
                listener.syncHeadersStarted(folder, folderName);


                List<? extends Message> remoteMessageArray =
                        remoteFolder.getMessages(remoteStart, remoteMessageCount, earliestDate, null);

                int messageCount = remoteMessageArray.size();

                for (Message thisMess : remoteMessageArray) {
                    headerProgress.incrementAndGet();
                    listener.syncHeadersProgress(folder, headerProgress.get(), messageCount);

                    Long localMessageTimestamp = localUidMap.get(thisMess.getUid());
                    if (localMessageTimestamp == null || localMessageTimestamp >= earliestTimestamp) {
                        remoteMessages.add(thisMess);
                        remoteUidMap.put(thisMess.getUid(), thisMess);
                    }
                }

                Timber.v("SYNC: Got %d messages for folder %s", remoteUidMap.size(), folder);

                listener.syncHeadersFinished(folder, headerProgress.get(), remoteUidMap.size());

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
                        listener.syncRemovedMessage(folder, destroyMessage);
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
            int newMessages = downloadMessages(remoteFolder, localFolder, remoteMessages, false,
                    true, lastUid, listener);

            int unreadMessageCount = localFolder.getUnreadMessageCount();
            listener.folderStatusChanged(folder, unreadMessageCount);

            /* Notify listeners that we're finally done. */

            localFolder.setLastChecked(System.currentTimeMillis());
            localFolder.setStatus(null);

            Timber.d("Done synchronizing folder %s:%s @ %tc with %d new messages",
                    account.getDescription(),
                    folder,
                    System.currentTimeMillis(),
                    newMessages);

            listener.syncFinished(folder, remoteMessageCount, newMessages);

            Timber.i("Done synchronizing folder %s:%s", account.getDescription(), folder);

        } catch (AuthenticationFailedException e) {
            listener.syncFailed(folder, "Authentication failure", e);
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
                            account.getDescription(), tLocalFolder.getServerId());
                }
            }

            listener.syncFailed(folder, rootMessage, e);

            Timber.e("Failed synchronizing folder %s:%s @ %tc", account.getDescription(), folder,
                    System.currentTimeMillis());

        } finally {
            if (providedRemoteFolder == null) {
                closeFolder(remoteFolder);
            }

            closeFolder(tLocalFolder);
        }

    }

    /**
     * Fetches the messages described by inputMessages from the remote store and writes them to
     * local storage.
     *
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
    int downloadMessages(Folder remoteFolder, LocalFolder localFolder, List<Message> inputMessages,
            boolean flagSyncOnly, boolean purgeToVisibleLimit, Integer lastUid, final SyncListener listener)
            throws MessagingException {

        final Date earliestDate = account.getEarliestPollDate();

        if (earliestDate != null) {
            Timber.d("Only syncing messages after %s", earliestDate);
        }
        final String folder = remoteFolder.getServerId();

        List<Message> syncFlagMessages = new ArrayList<>();
        List<Message> unsyncedMessages = new ArrayList<>();
        final AtomicInteger newMessages = new AtomicInteger(0);

        List<Message> messages = new ArrayList<>(inputMessages);

        for (Message message : messages) {
            evaluateMessageForDownload(message, folder, localFolder, remoteFolder, unsyncedMessages,
                    syncFlagMessages, flagSyncOnly, lastUid, listener);
        }

        final AtomicInteger progress = new AtomicInteger(0);
        final int todo = unsyncedMessages.size() + syncFlagMessages.size();
        listener.syncProgress(folder, progress.get(), todo);

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

            fetchUnsyncedMessages(remoteFolder, unsyncedMessages, smallMessages, largeMessages, progress, todo,
                    fp, listener);

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
        downloadSmallMessages(remoteFolder, localFolder, smallMessages, progress,
                newMessages, todo, fp, lastUid, listener);
        smallMessages.clear();
        /*
         * Now do the large messages that require more round trips.
         */
        fp = new FetchProfile();
        fp.add(FetchProfile.Item.STRUCTURE);
        downloadLargeMessages(remoteFolder, localFolder, largeMessages, progress,
                newMessages, todo, fp, lastUid, listener);
        largeMessages.clear();

        /*
         * Refresh the flags for any messages in the local store that we didn't just
         * download.
         */

        refreshLocalMessageFlags(remoteFolder, localFolder, syncFlagMessages, progress, todo, listener);

        Timber.d("SYNC: Synced remote messages for folder %s, %d new messages", folder, newMessages.get());

        if (purgeToVisibleLimit) {
            localFolder.purgeToVisibleLimit(new MessageRemovalListener() {
                @Override
                public void messageRemoved(Message message) {
                    listener.syncRemovedMessage(folder, message);
                }

            });
        }

        return newMessages.get();
    }

    private void evaluateMessageForDownload(final Message message, final String folder,
            final LocalFolder localFolder,
            final Folder remoteFolder,
            final List<Message> unsyncedMessages,
            final List<Message> syncFlagMessages,
            boolean flagSyncOnly,
            Integer lastUid,
            SyncListener listener) throws MessagingException {
        if (message.isSet(Flag.DELETED)) {
            Timber.v("Message with uid %s is marked as deleted", message.getUid());

            syncFlagMessages.add(message);
            return;
        }

        LocalMessage localMessage = localFolder.getMessage(message.getUid());

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

                    boolean isOldMessage = isOldMessage(localMessage, lastUid);
                    listener.syncNewMessage(folder, localMessage, isOldMessage);
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

    private boolean isOldMessage(LocalMessage message, Integer lastUid) {
        if (lastUid == null) {
            return false;
        }

        try {
            Integer messageUid = Integer.parseInt(message.getUid());
            return messageUid <= lastUid;
        } catch (NumberFormatException e) {
            Timber.w(e, "Couldn't parse UID: %s", message.getUid());
        }

        return false;
    }

    private <T extends Message> void fetchUnsyncedMessages(final Folder<T> remoteFolder,
            List<T> unsyncedMessages,
            final List<Message> smallMessages,
            final List<Message> largeMessages,
            final AtomicInteger progress,
            final int todo,
            FetchProfile fp,
            final SyncListener listener) throws MessagingException {
        final String folder = remoteFolder.getServerId();

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

                                //TODO: This might be the source of poll count errors in the UI. Is todo always the same as ofTotal
                                listener.syncProgress(folder, progress.get(), todo);

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

    private <T extends Message> void downloadSmallMessages(final Folder<T> remoteFolder,
            final LocalFolder localFolder,
            List<T> smallMessages,
            final AtomicInteger progress,
            final AtomicInteger newMessages,
            final int todo,
            FetchProfile fp,
            final Integer lastUid,
            final SyncListener listener) throws MessagingException {
        final String folder = remoteFolder.getServerId();

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
                            listener.syncProgress(folder, progress.get(), todo);

                            boolean isOldMessage = isOldMessage(localMessage, lastUid);
                            listener.syncNewMessage(folder, localMessage, isOldMessage);
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

    private <T extends Message> void downloadLargeMessages(final Folder<T> remoteFolder,
            final LocalFolder localFolder,
            List<T> largeMessages,
            final AtomicInteger progress,
            final AtomicInteger newMessages,
            final int todo,
            FetchProfile fp,
            Integer lastUid,
            SyncListener listener) throws MessagingException {
        final String folder = remoteFolder.getServerId();
        final Date earliestDate = account.getEarliestPollDate();

        Timber.d("SYNC: Fetching large messages for folder %s", folder);

        remoteFolder.fetch(largeMessages, fp, null);
        for (T message : largeMessages) {

            if (!shouldImportMessage(message, earliestDate)) {
                progress.incrementAndGet();
                continue;
            }

            if (message.getBody() == null) {
                downloadSaneBody(remoteFolder, localFolder, message);
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

            listener.syncProgress(folder, progress.get(), todo);

            boolean isOldMessage = isOldMessage(localMessage, lastUid);
            listener.syncNewMessage(folder, localMessage, isOldMessage);
        }

        Timber.d("SYNC: Done fetching large messages for folder %s", folder);
    }

    private void refreshLocalMessageFlags(final Folder remoteFolder,
            final LocalFolder localFolder,
            List<Message> syncFlagMessages,
            final AtomicInteger progress,
            final int todo,
            SyncListener listener
    ) throws MessagingException {

        final String folder = remoteFolder.getServerId();
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
                listener.syncFlagChanged(folder, localMessage);
            }
            progress.incrementAndGet();
            listener.syncProgress(folder, progress.get(), todo);
        }
    }

    private void downloadSaneBody(Folder remoteFolder, LocalFolder localFolder, Message message)
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
}
