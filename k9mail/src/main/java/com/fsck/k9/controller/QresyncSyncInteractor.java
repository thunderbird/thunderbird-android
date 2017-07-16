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
import com.fsck.k9.mail.store.imap.QresyncResponse;
import com.fsck.k9.mailstore.LocalFolder;
import timber.log.Timber;


class QresyncSyncInteractor {

    private final Account account;
    private final  LocalFolder localFolder;
    private final  ImapFolder imapFolder;
    private final  MessagingListener listener;
    private final  MessagingController controller;
    private final  ImapSyncInteractor imapSyncInteractor;

    QresyncSyncInteractor(Account account, LocalFolder localFolder, ImapFolder imapFolder, MessagingListener listener,
            MessagingController controller, ImapSyncInteractor imapSyncInteractor) {
        this.account = account;
        this.localFolder = localFolder;
        this.imapFolder = imapFolder;
        this.listener = listener;
        this.controller = controller;
        this.imapSyncInteractor = imapSyncInteractor;
    }

    int performSync(QresyncResponse qresyncResponse, List<String> expungedUids, MessageDownloader messageDownloader)
            throws MessagingException, IOException {
        final List<ImapMessage> remoteMessages = new ArrayList<>();

        if (account.syncRemoteDeletions()) {
            List<String> deletedUids = new ArrayList<>(expungedUids);
            deletedUids.addAll(qresyncResponse.getExpungedUids());
            imapSyncInteractor.syncRemoteDeletions(deletedUids);
        }

        findNewRemoteMessagesToDownload(remoteMessages, qresyncResponse, messageDownloader);
        if (imapFolder.getMessageCount() > localFolder.getVisibleLimit() && imapFolder.getMessageCount() >
                (remoteMessages.size() + localFolder.getMessageCount())) {
            findOldRemoteMessagesToDownload(remoteMessages);
        }

        return messageDownloader.downloadMessages(account, imapFolder, localFolder, remoteMessages, false, true, false);
    }

    private void findNewRemoteMessagesToDownload(List<ImapMessage> remoteMessages, QresyncResponse qresyncResponse,
            MessageDownloader messageDownloader) throws MessagingException {

        String folderName = imapFolder.getName();
        final AtomicInteger headerProgress = new AtomicInteger(0);

        for (MessagingListener l : controller.getListeners(listener)) {
            l.synchronizeMailboxHeadersStarted(account, folderName);
        }

        List<ImapMessage> modifiedMessages = qresyncResponse.getModifiedMessages();
        Timber.v("SYNC: Received %d FETCH responses in QRESYNC response", modifiedMessages.size());

        for (ImapMessage imapMessage : modifiedMessages) {
            Message localMessage = localFolder.getMessage(imapMessage.getUid());
            if (localMessage == null || (!localMessage.isSet(Flag.X_DOWNLOADED_FULL) &&
                    !localMessage.isSet(Flag.X_DOWNLOADED_PARTIAL))) {
                remoteMessages.add(imapMessage);
            } else {
                messageDownloader.processDownloadedFlags(account, localFolder, imapMessage, headerProgress,
                        modifiedMessages.size());
            }
        }

        Timber.v("SYNC: Received %d new message UIDs in QRESYNC response", remoteMessages.size());
    }

    private void findOldRemoteMessagesToDownload(List<ImapMessage> remoteMessages) throws MessagingException {
        /*At ths point, UIDs and flags for new messages and existing changed messages have been fetched, so all N
          messages in the local store should correspond to the most recent N messages in the remote store. We still need
          to download historical messages (messages older than the oldest message in the inbox) if necessary till we
          reach the mailbox capacity.*/

        String folderName = imapFolder.getName();
        Date earliestDate = account.getEarliestPollDate();
        final AtomicInteger headerProgress = new AtomicInteger(0);

        int oldMessagesStart = imapSyncInteractor.getRemoteStart(localFolder, imapFolder);
        int oldMessagesEnd = oldMessagesStart - 1 + localFolder.getVisibleLimit() - remoteMessages.size() -
                localFolder.getMessageCount();

        if (oldMessagesEnd > oldMessagesStart) {
            List<ImapMessage> oldMessages = imapFolder.getMessages(oldMessagesStart, oldMessagesEnd, earliestDate, null);
            Timber.v("SYNC: Fetched %d old message UIDs for folder %s", oldMessages.size(), folderName);
            for (ImapMessage oldMessage : oldMessages) {
                headerProgress.incrementAndGet();
                for (MessagingListener l : controller.getListeners(listener)) {
                    l.synchronizeMailboxHeadersProgress(account, folderName, headerProgress.get(), oldMessages.size());
                }
                remoteMessages.add(oldMessage);
            }
        }

        for (MessagingListener l : controller.getListeners(listener)) {
            l.synchronizeMailboxHeadersFinished(account, folderName, headerProgress.get(), remoteMessages.size());
        }
    }
}
