package com.fsck.k9.controller;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;
import com.fsck.k9.mailstore.LocalFolder;
import timber.log.Timber;


class NonQresyncExtensionHandler {

    private final Account account;
    private final LocalFolder localFolder;
    private final ImapFolder imapFolder;
    private final MessagingListener listener;
    private final MessagingController controller;
    private final ImapSyncInteractor imapSyncInteractor;

    NonQresyncExtensionHandler(Account account, LocalFolder localFolder, ImapFolder imapFolder,
            MessagingListener listener, MessagingController controller, ImapSyncInteractor imapSyncInteractor) {
        this.account = account;
        this.localFolder = localFolder;
        this.imapFolder = imapFolder;
        this.listener = listener;
        this.controller = controller;
        this.imapSyncInteractor = imapSyncInteractor;
    }

    int continueSync(MessageDownloader messageDownloader) throws MessagingException, IOException {
        Map<String, Long> localUidMap = localFolder.getAllMessagesAndEffectiveDates();
        String folderName = localFolder.getName();
        final List<ImapMessage> remoteMessages = new ArrayList<>();
        Map<String, ImapMessage> remoteUidMap = new HashMap<>();

        int remoteMessageCount = imapFolder.getMessageCount();
        int remoteStart = ImapSyncInteractor.getRemoteStart(localFolder, imapFolder);

        Timber.v("SYNC: About to fetch UIDs for messages %d through %d in folder %s",
                remoteStart, remoteMessageCount, folderName);

        findRemoteMessagesToDownload(localUidMap, remoteMessages, remoteUidMap, remoteStart);

        if (account.syncRemoteDeletions()) {
            imapSyncInteractor.syncRemoteDeletions(findDeletedMessageUids(localUidMap, remoteUidMap));
        }

        // noinspection UnusedAssignment, free memory early?
        localUidMap = null;

        if (localFolder.getHighestModSeq() != 0 && imapFolder.supportsModSeq()) {
            downloadChangedMessageFlags(remoteMessages, messageDownloader);
        }

        return messageDownloader.downloadMessages(account, imapFolder, localFolder, remoteMessages, false, true, true);
    }

    private void findRemoteMessagesToDownload(Map<String, Long> localUidMap, List<ImapMessage> remoteMessages,
            Map<String, ImapMessage> remoteUidMap, int remoteStart) throws MessagingException {
        String folderName = imapFolder.getName();
        final Date earliestDate = account.getEarliestPollDate();
        long earliestTimestamp = earliestDate != null ? earliestDate.getTime() : 0L;
        final AtomicInteger headerProgress = new AtomicInteger(0);
        int remoteMessageCount = imapFolder.getMessageCount();

        for (MessagingListener l : controller.getListeners(listener)) {
            l.synchronizeMailboxHeadersStarted(account, folderName);
        }

        List<ImapMessage> remoteMessageArray = new ArrayList<>();
        if (remoteMessageCount > 0) {
            remoteMessageArray = imapFolder.getMessages(remoteStart, remoteMessageCount, earliestDate, null);
        }

        int messageCount = remoteMessageArray.size();

        for (ImapMessage thisMess : remoteMessageArray) {
            headerProgress.incrementAndGet();
            for (MessagingListener l : controller.getListeners(listener)) {
                l.synchronizeMailboxHeadersProgress(account, folderName, headerProgress.get(), messageCount);
            }
            Long localMessageTimestamp = localUidMap.get(thisMess.getUid());
            if (localMessageTimestamp == null || localMessageTimestamp >= earliestTimestamp) {
                remoteMessages.add(thisMess);
                remoteUidMap.put(thisMess.getUid(), thisMess);
            }
        }

        Timber.v("SYNC: Fetched %d message UIDs for folder %s", remoteUidMap.size(), folderName);

        for (MessagingListener l : controller.getListeners(listener)) {
            l.synchronizeMailboxHeadersFinished(account, folderName, headerProgress.get(), remoteUidMap.size());
        }
    }

    private List<String> findDeletedMessageUids(Map<String, Long> localUidMap, Map<String, ImapMessage> remoteUidMap) {
        List<String> deletedMessageUids = new ArrayList<>();
        for (String localMessageUid : localUidMap.keySet()) {
            if (remoteUidMap.get(localMessageUid) == null) {
                deletedMessageUids.add(localMessageUid);
            }
        }
        return deletedMessageUids;
    }

    private void downloadChangedMessageFlags(List<ImapMessage> messages, final MessageDownloader messageDownloader)
            throws MessagingException {
        final String folderName = imapFolder.getName();
        final Map<Long, Message> knownMessageMap = new HashMap<>();
        Iterator<ImapMessage> iterator = messages.iterator();
        while (iterator.hasNext()) {
            String uid = iterator.next().getUid();
            Message localMessage = localFolder.getMessage(uid);
            if (localMessage != null && (localMessage.isSet(Flag.X_DOWNLOADED_FULL) ||
                    localMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)) && !localMessage.isSet(Flag.DELETED)) {
                knownMessageMap.put(Long.parseLong(uid), localMessage);
                iterator.remove();
            }
        }

        if (knownMessageMap.size() != 0) {
            Timber.v("SYNC: Fetching and syncing flags for %d local messages using CONDSTORE", knownMessageMap.size());
            long cachedHighestModSeq = localFolder.getHighestModSeq();
            final AtomicInteger flagSyncProgress = new AtomicInteger(0);
            imapFolder.fetchChangedMessageFlagsUsingCondstore(new ArrayList<>(knownMessageMap.keySet()),
                    cachedHighestModSeq, new MessageRetrievalListener<ImapMessage>() {
                        @Override
                        public void messageStarted(String uid, int number, int ofTotal) {

                        }

                        @Override
                        public void messageFinished(ImapMessage message, int number, int ofTotal) {
                            try {
                                messageDownloader.processDownloadedFlags(account, localFolder, message);
                            } catch (MessagingException e) {
                                Timber.e(e, "Error while synchronizing flags using CONDSTORE.");
                                controller.addErrorMessage(account, null, e);
                            }

                        }

                        @Override
                        public void messagesFinished(int total) {

                        }
                    });
        }
    }
}
