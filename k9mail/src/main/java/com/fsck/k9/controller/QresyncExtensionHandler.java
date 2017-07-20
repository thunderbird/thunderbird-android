package com.fsck.k9.controller;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;
import com.fsck.k9.mail.store.imap.QresyncParamResponse;
import com.fsck.k9.mailstore.LocalFolder;
import timber.log.Timber;


class QresyncExtensionHandler {

    private final Account account;
    private final LocalFolder localFolder;
    private final ImapFolder imapFolder;
    private final MessagingListener listener;
    private final MessagingController controller;
    private final ImapSyncInteractor imapSyncInteractor;

    QresyncExtensionHandler(Account account, LocalFolder localFolder, ImapFolder imapFolder, MessagingListener listener,
            MessagingController controller, ImapSyncInteractor imapSyncInteractor) {
        this.account = account;
        this.localFolder = localFolder;
        this.imapFolder = imapFolder;
        this.listener = listener;
        this.controller = controller;
        this.imapSyncInteractor = imapSyncInteractor;
    }

    int continueSync(QresyncParamResponse qresyncParamResponse, List<String> expungedUids,
            MessageDownloader messageDownloader) throws MessagingException, IOException {
        final String folderName = localFolder.getName();
        final List<ImapMessage> remoteMessagesToDownload = new ArrayList<>();

        if (account.syncRemoteDeletions()) {
            List<String> deletedUids = new ArrayList<>(expungedUids);
            deletedUids.addAll(qresyncParamResponse.getExpungedUids());
            imapSyncInteractor.syncRemoteDeletions(deletedUids);
        }

        for (MessagingListener l : controller.getListeners(listener)) {
            l.synchronizeMailboxHeadersStarted(account, folderName);
        }

        processFetchResponses(remoteMessagesToDownload, qresyncParamResponse, messageDownloader);

        int newLocalMessageCount = remoteMessagesToDownload.size() + localFolder.getMessageCount();
        if (imapFolder.getMessageCount() >= localFolder.getVisibleLimit() && imapFolder.getMessageCount() >=
                newLocalMessageCount) {
            findOldRemoteMessagesToDownload(remoteMessagesToDownload);
        }

        int messageDownloadCount = remoteMessagesToDownload.size();
        for (MessagingListener l : controller.getListeners(listener)) {
            l.synchronizeMailboxHeadersFinished(account, folderName, messageDownloadCount, messageDownloadCount);
        }

        return messageDownloader.downloadMessages(account, imapFolder, localFolder, remoteMessagesToDownload, false,
                true, false);
    }

    private void processFetchResponses(List<ImapMessage> remoteMessagesToDownload, QresyncParamResponse
            qresyncParamResponse, MessageDownloader messageDownloader) throws MessagingException {
        String folderName = imapFolder.getName();
        final AtomicInteger headerProgress = new AtomicInteger(0);

        List<ImapMessage> modifiedMessages = qresyncParamResponse.getModifiedMessages();
        Timber.v("SYNC: Received %d FETCH responses in QRESYNC response", modifiedMessages.size());

        Long cachedSmallestUid = localFolder.getSmallestMessageUid();
        long smallestLocalUid = cachedSmallestUid == null ? 1 : cachedSmallestUid;

        for (ImapMessage imapMessage : modifiedMessages) {
            long remoteMessageUid = Long.parseLong(imapMessage.getUid());
            if (remoteMessageUid < smallestLocalUid) {
                continue;
            }
            Message localMessage = localFolder.getMessage(imapMessage.getUid());

            if (localMessage == null || (!localMessage.isSet(Flag.X_DOWNLOADED_FULL) &&
                    !localMessage.isSet(Flag.X_DOWNLOADED_PARTIAL))) {
                remoteMessagesToDownload.add(imapMessage);
            } else {
                messageDownloader.processDownloadedFlags(account, localFolder, imapMessage);
            }

            headerProgress.incrementAndGet();
            for (MessagingListener l : controller.getListeners()) {
                l.synchronizeMailboxProgress(account, folderName, headerProgress.get(), modifiedMessages.size());
            }
        }

        Timber.v("SYNC: Received %d new message UIDs in QRESYNC response", remoteMessagesToDownload.size());
    }

    private void findOldRemoteMessagesToDownload(List<ImapMessage> remoteMessagesToDownload) throws MessagingException {
        /*At ths point, UIDs and flags for new messages and existing changed messages have been fetched, so all N
          messages in the local store should correspond to the most recent N messages in the remote store. We still need
          to download historical messages (messages older than the oldest message in the inbox) if necessary till we
          reach the mailbox capacity.*/

        String folderName = imapFolder.getName();
        Date earliestDate = account.getEarliestPollDate();
        final AtomicInteger headerProgress = new AtomicInteger(0);

        int newLocalMessageCount = remoteMessagesToDownload.size() + localFolder.getMessageCount();
        int oldMessagesStart = ImapSyncInteractor.getRemoteStart(localFolder, imapFolder);
        int oldMessagesEnd = oldMessagesStart - 1 + localFolder.getVisibleLimit() - newLocalMessageCount;

        if (oldMessagesEnd > oldMessagesStart) {
            List<ImapMessage> oldMessages = imapFolder.getMessages(oldMessagesStart, oldMessagesEnd, earliestDate, null);
            Timber.v("SYNC: Fetched %d old message UIDs for folder %s", oldMessages.size(), folderName);

            for (ImapMessage oldMessage : oldMessages) {
                headerProgress.incrementAndGet();
                for (MessagingListener l : controller.getListeners(listener)) {
                    l.synchronizeMailboxHeadersProgress(account, folderName, headerProgress.get(), oldMessages.size());
                }

                remoteMessagesToDownload.add(oldMessage);
            }
        }
    }
}
