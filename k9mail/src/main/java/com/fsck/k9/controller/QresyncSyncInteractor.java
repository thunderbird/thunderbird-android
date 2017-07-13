package com.fsck.k9.controller;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;
import com.fsck.k9.mail.store.imap.QresyncResponse;
import com.fsck.k9.mailstore.LocalFolder;
import timber.log.Timber;

import static com.fsck.k9.controller.ImapSyncInteractor.getRemoteStart;


class QresyncSyncInteractor {

    static int performSync(Account account, LocalFolder localFolder, ImapFolder imapFolder, MessagingListener listener,
            MessagingController controller, QresyncResponse qresyncResponse, MessageDownloader messageDownloader)
            throws MessagingException, IOException {
        String folderName = localFolder.getName();
        final List<ImapMessage> remoteMessages = new ArrayList<>();

        findRemoteMessagesToDownload(account, localFolder, imapFolder, remoteMessages, listener, controller,
                qresyncResponse);

        /*
         * Remove any messages that are in the local store but no longer on the remote store or are too old
         */
        if (account.syncRemoteDeletions()) {
            ImapSyncInteractor.syncRemoteDeletions(account, localFolder, imapFolder, qresyncResponse.getExpungedUids(),
                    listener, controller);
        }

        return messageDownloader.downloadMessages(account, imapFolder, localFolder, remoteMessages, false, true, false);
    }

    private static void findRemoteMessagesToDownload(Account account, LocalFolder localFolder,
            ImapFolder imapFolder, List<ImapMessage> remoteMessages, MessagingListener listener,
            MessagingController controller, QresyncResponse qresyncResponse) throws MessagingException {

        String folderName = imapFolder.getName();
        final AtomicInteger headerProgress = new AtomicInteger(0);
        int remoteMessageCount = imapFolder.getMessageCount();

        for (MessagingListener l : controller.getListeners(listener)) {
            l.synchronizeMailboxHeadersStarted(account, folderName);
        }

        for (ImapMessage imapMessage : qresyncResponse.getModifiedMessages()) {
            Message localMessage = localFolder.getMessage(imapMessage.getUid());
            if (localMessage == null) {
                remoteMessages.add(imapMessage);
            } else {
                localMessage.setFlags(imapMessage.getFlags(), true);
                localFolder.appendMessages(Collections.singletonList(localMessage));
            }
        }

        Date earliestDate = account.getEarliestPollDate();
        int oldMessagesStart = getRemoteStart(localFolder, imapFolder);
        int oldMessagesEnd = imapFolder.getMessageCount() - localFolder.getMessageCount();

        if (oldMessagesEnd > oldMessagesStart) {
            remoteMessages.addAll(imapFolder.getMessages(oldMessagesStart, oldMessagesEnd, earliestDate, null));
        }

        int messageCount = remoteMessages.size();

        for (ImapMessage thisMess : remoteMessages) {
            headerProgress.incrementAndGet();
            for (MessagingListener l : controller.getListeners(listener)) {
                l.synchronizeMailboxHeadersProgress(account, folderName, headerProgress.get(), messageCount);
            }
        }

        Timber.v("SYNC: Got %d messages for folder %s", remoteMessages.size(), folderName);

        for (MessagingListener l : controller.getListeners(listener)) {
            l.synchronizeMailboxHeadersFinished(account, folderName, headerProgress.get(), remoteMessages.size());
        }
    }
}
