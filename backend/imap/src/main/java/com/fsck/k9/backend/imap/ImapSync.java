package com.fsck.k9.backend.imap;


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

import com.fsck.k9.backend.api.BackendFolder;
import com.fsck.k9.backend.api.BackendFolder.MoreMessages;
import com.fsck.k9.backend.api.BackendStorage;
import com.fsck.k9.backend.api.SyncConfig;
import com.fsck.k9.backend.api.SyncConfig.ExpungePolicy;
import com.fsck.k9.backend.api.SyncListener;
import com.fsck.k9.helper.ExceptionHelper;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.BodyFactory;
import com.fsck.k9.mail.DefaultBodyFactory;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;
import com.fsck.k9.mail.store.imap.ImapStore;
import timber.log.Timber;


class ImapSync {
    private static final String EXTRA_UID_VALIDITY = "imapUidValidity";

    private final String accountName;
    private final BackendStorage backendStorage;
    private final ImapStore imapStore;


    ImapSync(String accountName, BackendStorage backendStorage, ImapStore imapStore) {
        this.accountName = accountName;
        this.backendStorage = backendStorage;
        this.imapStore = imapStore;
    }

    void sync(String folder, SyncConfig syncConfig, SyncListener listener) {
        synchronizeMailboxSynchronous(folder, syncConfig, listener);
    }

    void synchronizeMailboxSynchronous(final String folder, SyncConfig syncConfig, final SyncListener listener) {
        Timber.i("Synchronizing folder %s:%s", accountName, folder);

        ImapFolder remoteFolder = null;
        BackendFolder backendFolder = null;
        try {
            Timber.v("SYNC: About to get local folder %s", folder);
            backendFolder = backendStorage.getFolder(folder);

            listener.syncStarted(folder);

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

            if (syncConfig.getExpungePolicy() == ExpungePolicy.ON_POLL) {
                Timber.d("SYNC: Expunging folder %s:%s", accountName, folder);
                remoteFolder.expunge();
            }
            remoteFolder.open(ImapFolder.OPEN_MODE_RO);

            listener.syncAuthenticationSuccess();

            Long uidValidity = remoteFolder.getUidValidity();
            Long oldUidValidity = backendFolder.getFolderExtraNumber(EXTRA_UID_VALIDITY);
            if (oldUidValidity == null && uidValidity != null) {
                Timber.d("SYNC: Saving UIDVALIDITY for %s", folder);
                backendFolder.setFolderExtraNumber(EXTRA_UID_VALIDITY, uidValidity);
            } else if (oldUidValidity != null && !oldUidValidity.equals(uidValidity)) {
                Timber.d("SYNC: UIDVALIDITY for %s changed; clearing local message cache", folder);
                backendFolder.clearAllMessages();
                backendFolder.setFolderExtraNumber(EXTRA_UID_VALIDITY, uidValidity);
            }

            /*
             * Get the message list from the local store and create an index of
             * the uids within the list.
             */

            Long lastUid = backendFolder.getLastUid();

            Map<String, Long> localUidMap = backendFolder.getAllMessagesAndEffectiveDates();

            /*
             * Get the remote message count.
             */
            int remoteMessageCount = remoteFolder.getMessageCount();

            int visibleLimit = backendFolder.getVisibleLimit();

            if (visibleLimit < 0) {
                visibleLimit = syncConfig.getDefaultVisibleLimit();
            }

            final List<ImapMessage> remoteMessages = new ArrayList<>();
            Map<String, ImapMessage> remoteUidMap = new HashMap<>();

            Timber.v("SYNC: Remote message count for folder %s is %d", folder, remoteMessageCount);

            final Date earliestDate = syncConfig.getEarliestPollDate();
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
                listener.syncHeadersStarted(folder);


                List<ImapMessage> remoteMessageArray =
                        remoteFolder.getMessages(remoteStart, remoteMessageCount, earliestDate, null);

                int messageCount = remoteMessageArray.size();

                for (ImapMessage thisMess : remoteMessageArray) {
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
            MoreMessages moreMessages = backendFolder.getMoreMessages();
            if (syncConfig.getSyncRemoteDeletions()) {
                List<String> destroyMessageUids = new ArrayList<>();
                for (String localMessageUid : localUidMap.keySet()) {
                    if (remoteUidMap.get(localMessageUid) == null) {
                        destroyMessageUids.add(localMessageUid);
                    }
                }

                if (!destroyMessageUids.isEmpty()) {
                    moreMessages = MoreMessages.UNKNOWN;

                    backendFolder.destroyMessages(destroyMessageUids);
                    for (String uid : destroyMessageUids) {
                        listener.syncRemovedMessage(folder, uid);
                    }
                }
            }
            // noinspection UnusedAssignment, free memory early? (better break up the method!)
            localUidMap = null;

            if (moreMessages == MoreMessages.UNKNOWN) {
                updateMoreMessages(remoteFolder, backendFolder, earliestDate, remoteStart);
            }

            /*
             * Now we download the actual content of messages.
             */
            int newMessages = downloadMessages(syncConfig, remoteFolder, backendFolder, remoteMessages, false,
                    lastUid, listener);

            listener.folderStatusChanged(folder);

            /* Notify listeners that we're finally done. */

            backendFolder.setLastChecked(System.currentTimeMillis());
            backendFolder.setStatus(null);

            Timber.d("Done synchronizing folder %s:%s @ %tc with %d new messages",
                    accountName,
                    folder,
                    System.currentTimeMillis(),
                    newMessages);

            listener.syncFinished(folder);

            Timber.i("Done synchronizing folder %s:%s", accountName, folder);

        } catch (AuthenticationFailedException e) {
            listener.syncFailed(folder, "Authentication failure", e);
        } catch (Exception e) {
            Timber.e(e, "synchronizeMailbox");
            // If we don't set the last checked, it can try too often during
            // failure conditions
            String rootMessage = ExceptionHelper.getRootCauseMessage(e);
            if (backendFolder != null) {
                try {
                    backendFolder.setStatus(rootMessage);
                    backendFolder.setLastChecked(System.currentTimeMillis());
                } catch (Exception e1) {
                    Timber.e(e1, "Could not set last checked on folder %s:%s", accountName, folder);
                }
            }

            listener.syncFailed(folder, rootMessage, e);

            Timber.e("Failed synchronizing folder %s:%s @ %tc", accountName, folder,
                    System.currentTimeMillis());

        } finally {
            closeFolder(remoteFolder);
        }

    }

    void downloadMessage(SyncConfig syncConfig, String folderServerId, String messageServerId)
            throws MessagingException {
        BackendFolder backendFolder = backendStorage.getFolder(folderServerId);
        ImapFolder remoteFolder = imapStore.getFolder(folderServerId);
        try {
            remoteFolder.open(ImapFolder.OPEN_MODE_RO);
            ImapMessage remoteMessage = remoteFolder.getMessage(messageServerId);

            downloadMessages(
                    syncConfig,
                    remoteFolder,
                    backendFolder,
                    Collections.singletonList(remoteMessage),
                    false,
                    null,
                    new SimpleSyncListener());
        } finally {
            remoteFolder.close();
        }
    }

    /**
     * Fetches the messages described by inputMessages from the remote store and writes them to
     * local storage.
     *
     * @param remoteFolder
     *         The remote folder to download messages from.
     * @param backendFolder
     *         The {@link BackendFolder} instance corresponding to the remote folder.
     * @param inputMessages
     *         A list of messages objects that store the UIDs of which messages to download.
     * @param flagSyncOnly
     *         Only flags will be fetched from the remote store if this is {@code true}.
     * @return The number of downloaded messages that are not flagged as {@link Flag#SEEN}.
     *
     * @throws MessagingException
     */
    private int downloadMessages(SyncConfig syncConfig, ImapFolder remoteFolder, BackendFolder backendFolder,
            List<ImapMessage> inputMessages, boolean flagSyncOnly, Long lastUid,
            final SyncListener listener) throws MessagingException {

        final String folder = remoteFolder.getServerId();

        List<ImapMessage> syncFlagMessages = new ArrayList<>();
        List<ImapMessage> unsyncedMessages = new ArrayList<>();
        final AtomicInteger newMessages = new AtomicInteger(0);

        List<ImapMessage> messages = new ArrayList<>(inputMessages);

        for (ImapMessage message : messages) {
            evaluateMessageForDownload(message, backendFolder, remoteFolder, unsyncedMessages, syncFlagMessages,
                    flagSyncOnly);
        }

        final AtomicInteger progress = new AtomicInteger(0);
        final int todo = unsyncedMessages.size() + syncFlagMessages.size();
        listener.syncProgress(folder, progress.get(), todo);

        Timber.d("SYNC: Have %d unsynced messages", unsyncedMessages.size());

        messages.clear();
        final List<ImapMessage> largeMessages = new ArrayList<>();
        final List<ImapMessage> smallMessages = new ArrayList<>();
        if (!unsyncedMessages.isEmpty()) {
            Collections.sort(unsyncedMessages, new UidReverseComparator());
            int visibleLimit = backendFolder.getVisibleLimit();
            int listSize = unsyncedMessages.size();

            if ((visibleLimit > 0) && (listSize > visibleLimit)) {
                unsyncedMessages = unsyncedMessages.subList(0, visibleLimit);
            }

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.FLAGS);
            fp.add(FetchProfile.Item.ENVELOPE);

            Timber.d("SYNC: About to fetch %d unsynced messages for folder %s", unsyncedMessages.size(), folder);

            fetchUnsyncedMessages(syncConfig, remoteFolder, unsyncedMessages, smallMessages, largeMessages, progress,
                    todo, fp, listener);

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
        int maxDownloadSize = syncConfig.getMaximumAutoDownloadMessageSize();
        FetchProfile fp = new FetchProfile();
        //TODO: Only fetch small and large messages if we have some
        fp.add(FetchProfile.Item.BODY);
        //        fp.add(FetchProfile.Item.FLAGS);
        //        fp.add(FetchProfile.Item.ENVELOPE);
        downloadSmallMessages(remoteFolder, backendFolder, smallMessages, progress, newMessages, todo, fp,
                lastUid, listener);
        smallMessages.clear();
        /*
         * Now do the large messages that require more round trips.
         */
        fp = new FetchProfile();
        fp.add(FetchProfile.Item.STRUCTURE);
        downloadLargeMessages(remoteFolder, backendFolder, largeMessages, progress, newMessages, todo, fp,
                lastUid, listener, maxDownloadSize);
        largeMessages.clear();

        /*
         * Refresh the flags for any messages in the local store that we didn't just
         * download.
         */

        refreshLocalMessageFlags(syncConfig, remoteFolder, backendFolder, syncFlagMessages, progress, todo, listener);

        Timber.d("SYNC: Synced remote messages for folder %s, %d new messages", folder, newMessages.get());

        return newMessages.get();
    }

    private void evaluateMessageForDownload(ImapMessage message, BackendFolder backendFolder, ImapFolder remoteFolder,
            List<ImapMessage> unsyncedMessages, List<ImapMessage> syncFlagMessages, boolean flagSyncOnly) {

        String messageServerId = message.getUid();
        if (message.isSet(Flag.DELETED)) {
            Timber.v("Message with uid %s is marked as deleted", messageServerId);

            syncFlagMessages.add(message);
            return;
        }

        boolean messagePresentLocally = backendFolder.isMessagePresent(messageServerId);
        if (!messagePresentLocally) {
            if (!flagSyncOnly) {
                Timber.v("Message with uid %s has not yet been downloaded", messageServerId);
                unsyncedMessages.add(message);
            }
            return;
        }

        Set<Flag> messageFlags = backendFolder.getMessageFlags(messageServerId);
        if (!messageFlags.contains(Flag.DELETED)) {
            Timber.v("Message with uid %s is present in the local store", messageServerId);

            if (!messageFlags.contains(Flag.X_DOWNLOADED_FULL) && !messageFlags.contains(Flag.X_DOWNLOADED_PARTIAL)) {
                Timber.v("Message with uid %s is not downloaded, even partially; trying again",
                        messageServerId);

                unsyncedMessages.add(message);
            } else {
                syncFlagMessages.add(message);
            }
        } else {
            Timber.v("Local copy of message with uid %s is marked as deleted", messageServerId);
        }
    }

    private boolean isOldMessage(String messageServerId, Long lastUid) {
        if (lastUid == null) {
            return false;
        }

        try {
            Long messageUid = Long.parseLong(messageServerId);
            return messageUid <= lastUid;
        } catch (NumberFormatException e) {
            Timber.w(e, "Couldn't parse UID: %s", messageServerId);
        }

        return false;
    }

    private void fetchUnsyncedMessages(
            final SyncConfig syncConfig,
            final ImapFolder remoteFolder,
            List<ImapMessage> unsyncedMessages,
            final List<ImapMessage> smallMessages,
            final List<ImapMessage> largeMessages,
            final AtomicInteger progress,
            final int todo,
            FetchProfile fp,
            final SyncListener listener) throws MessagingException {
        final String folder = remoteFolder.getServerId();

        remoteFolder.fetch(unsyncedMessages, fp,
                new MessageRetrievalListener<ImapMessage>() {
                    @Override
                    public void messageFinished(ImapMessage message, int number, int ofTotal) {
                        try {
                            if (message.isSet(Flag.DELETED)) {
                                Timber.v("Newly downloaded message %s:%s:%s was marked deleted on server, skipping",
                                        accountName, folder, message.getUid());

                                progress.incrementAndGet();

                                //TODO: This might be the source of poll count errors in the UI. Is todo always the same as ofTotal
                                listener.syncProgress(folder, progress.get(), todo);

                                return;
                            }

                            if (syncConfig.getMaximumAutoDownloadMessageSize() > 0 &&
                                    message.getSize() > syncConfig.getMaximumAutoDownloadMessageSize()) {
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

                },
                syncConfig.getMaximumAutoDownloadMessageSize());
    }

    private void downloadSmallMessages(
            final ImapFolder remoteFolder,
            final BackendFolder backendFolder,
            List<ImapMessage> smallMessages,
            final AtomicInteger progress,
            final AtomicInteger newMessages,
            final int todo,
            FetchProfile fp,
            final Long lastUid,
            final SyncListener listener) throws MessagingException {
        final String folder = remoteFolder.getServerId();

        Timber.d("SYNC: Fetching %d small messages for folder %s", smallMessages.size(), folder);

        remoteFolder.fetch(smallMessages,
                fp, new MessageRetrievalListener<ImapMessage>() {
                    @Override
                    public void messageFinished(final ImapMessage message, int number, int ofTotal) {
                        try {
                            // Store the updated message locally
                            backendFolder.saveCompleteMessage(message);
                            progress.incrementAndGet();


                            // Increment the number of "new messages" if the newly downloaded message is
                            // not marked as read.
                            if (!message.isSet(Flag.SEEN)) {
                                newMessages.incrementAndGet();
                            }

                            String messageServerId = message.getUid();
                            Timber.v("About to notify listeners that we got a new small message %s:%s:%s",
                                    accountName, folder, messageServerId);

                            // Update the listener with what we've found
                            listener.syncProgress(folder, progress.get(), todo);

                            boolean isOldMessage = isOldMessage(messageServerId, lastUid);
                            listener.syncNewMessage(folder, messageServerId, isOldMessage);
                        } catch (Exception e) {
                            Timber.e(e, "SYNC: fetch small messages");
                        }
                    }

                    @Override
                    public void messageStarted(String uid, int number, int ofTotal) {
                    }

                    @Override
                    public void messagesFinished(int total) {
                    }
                },
                -1);

        Timber.d("SYNC: Done fetching small messages for folder %s", folder);
    }

    private void downloadLargeMessages(
            final ImapFolder remoteFolder,
            final BackendFolder backendFolder,
            List<ImapMessage> largeMessages,
            final AtomicInteger progress,
            final AtomicInteger newMessages,
            final int todo,
            FetchProfile fp,
            Long lastUid,
            SyncListener listener,
            int maxDownloadSize) throws MessagingException {
        final String folder = remoteFolder.getServerId();

        Timber.d("SYNC: Fetching large messages for folder %s", folder);

        remoteFolder.fetch(largeMessages, fp, null, maxDownloadSize);
        for (ImapMessage message : largeMessages) {
            if (message.getBody() == null) {
                downloadSaneBody(remoteFolder, backendFolder, message, maxDownloadSize);
            } else {
                downloadPartial(remoteFolder, backendFolder, message, maxDownloadSize);
            }

            String messageServerId = message.getUid();
            Timber.v("About to notify listeners that we got a new large message %s:%s:%s",
                    accountName, folder, messageServerId);

            // Update the listener with what we've found
            progress.incrementAndGet();

            // TODO do we need to re-fetch this here?
            Set<Flag> flags = backendFolder.getMessageFlags(messageServerId);
            // Increment the number of "new messages" if the newly downloaded message is
            // not marked as read.
            if (!flags.contains(Flag.SEEN)) {
                newMessages.incrementAndGet();
            }

            listener.syncProgress(folder, progress.get(), todo);

            boolean isOldMessage = isOldMessage(messageServerId, lastUid);
            listener.syncNewMessage(folder, messageServerId, isOldMessage);
        }

        Timber.d("SYNC: Done fetching large messages for folder %s", folder);
    }

    private void refreshLocalMessageFlags(
            SyncConfig syncConfig,
            final ImapFolder remoteFolder,
            final BackendFolder backendFolder,
            List<ImapMessage> syncFlagMessages,
            final AtomicInteger progress,
            final int todo,
            SyncListener listener
    ) throws MessagingException {

        final String folder = remoteFolder.getServerId();
        Timber.d("SYNC: About to sync flags for %d remote messages for folder %s", syncFlagMessages.size(), folder);

        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.FLAGS);

        List<ImapMessage> undeletedMessages = new LinkedList<>();
        for (ImapMessage message : syncFlagMessages) {
            if (!message.isSet(Flag.DELETED)) {
                undeletedMessages.add(message);
            }
        }

        int maxDownloadSize = syncConfig.getMaximumAutoDownloadMessageSize();
        remoteFolder.fetch(undeletedMessages, fp, null, maxDownloadSize);
        for (ImapMessage remoteMessage : syncFlagMessages) {
            boolean messageChanged = syncFlags(syncConfig, backendFolder, remoteMessage);
            if (messageChanged) {
                listener.syncFlagChanged(folder, remoteMessage.getUid());
            }
            progress.incrementAndGet();
            listener.syncProgress(folder, progress.get(), todo);
        }
    }

    private void downloadSaneBody(ImapFolder remoteFolder, BackendFolder backendFolder, ImapMessage message,
            int maxDownloadSize) throws MessagingException {
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

        remoteFolder.fetch(Collections.singletonList(message), fp, null, maxDownloadSize);

        // Store the updated message locally
        backendFolder.savePartialMessage(message);
    }

    private void downloadPartial(ImapFolder remoteFolder, BackendFolder backendFolder, ImapMessage message,
            int maxDownloadSize) throws MessagingException {
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
            remoteFolder.fetchPart(message, part, null, bodyFactory, maxDownloadSize);
        }
        // Store the updated message locally
        backendFolder.savePartialMessage(message);
    }

    private boolean syncFlags(SyncConfig syncConfig, BackendFolder backendFolder, ImapMessage remoteMessage) {
        String messageServerId = remoteMessage.getUid();

        if (!backendFolder.isMessagePresent(messageServerId)) {
            return false;
        }

        Set<Flag> localMessageFlags = backendFolder.getMessageFlags(messageServerId);
        if (localMessageFlags.contains(Flag.DELETED)) {
            return false;
        }

        boolean messageChanged = false;
        if (remoteMessage.isSet(Flag.DELETED)) {
            if (syncConfig.getSyncRemoteDeletions()) {
                backendFolder.setMessageFlag(messageServerId, Flag.DELETED, true);
                messageChanged = true;
            }
        } else {
            for (Flag flag : syncConfig.getSyncFlags()) {
                if (remoteMessage.isSet(flag) != localMessageFlags.contains(flag)) {
                    backendFolder.setMessageFlag(messageServerId, flag, remoteMessage.isSet(flag));
                    messageChanged = true;
                }
            }
        }
        return messageChanged;
    }

    private static void closeFolder(ImapFolder folder) {
        if (folder != null) {
            folder.close();
        }
    }

    private void updateMoreMessages(ImapFolder remoteFolder, BackendFolder backendFolder, Date earliestDate, int remoteStart)
            throws MessagingException, IOException {

        if (remoteStart == 1) {
            backendFolder.setMoreMessages(MoreMessages.FALSE);
        } else {
            boolean moreMessagesAvailable = remoteFolder.areMoreMessagesAvailable(remoteStart, earliestDate);

            MoreMessages newMoreMessages = (moreMessagesAvailable) ? MoreMessages.TRUE : MoreMessages.FALSE;
            backendFolder.setMoreMessages(newMoreMessages);
        }
    }
}
