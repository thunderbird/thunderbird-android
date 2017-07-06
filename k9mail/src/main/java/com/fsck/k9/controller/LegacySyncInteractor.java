package com.fsck.k9.controller;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalFolder.MoreMessages;
import com.fsck.k9.mailstore.LocalMessage;
import timber.log.Timber;


class LegacySyncInteractor {

    static int performSync(Account account, MessagingListener listener, LocalFolder localFolder, Folder remoteFolder,
            MessagingController controller, MessageDownloader messageDownloader) throws  Exception {

        Map<String, Long> localUidMap = localFolder.getAllMessagesAndEffectiveDates();
        String folderName = localFolder.getName();
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

        Timber.v("SYNC: Remote message count for folder %s is %d", folderName, remoteMessageCount);

        final Date earliestDate = account.getEarliestPollDate();

        int remoteStart = 1;
        if (remoteMessageCount > 0) {
            /* Message numbers start at 1.  */
            if (visibleLimit > 0) {
                remoteStart = Math.max(0, remoteMessageCount - visibleLimit) + 1;
            } else {
                remoteStart = 1;
            }

            Timber.v("SYNC: About to get messages %d through %d for folder %s",
                    remoteStart, remoteMessageCount, folderName);

            findRemoteMessagesToDownload(account, remoteFolder, localUidMap, remoteMessages, remoteUidMap, earliestDate,
                    remoteStart, listener, controller);

        } else if (remoteMessageCount < 0) {
            throw new Exception("Message count " + remoteMessageCount + " for folder " + folderName);
        }

        /*
         * Remove any messages that are in the local store but no longer on the remote store or are too old
         */
        MoreMessages moreMessages = null;
        if (account.syncRemoteDeletions()) {
            moreMessages = syncRemoteDeletions(account, localFolder, localUidMap, remoteUidMap, listener, controller);
        }

        // noinspection UnusedAssignment, free memory early? (better break up the method!)
        localUidMap = null;

        if (moreMessages != null && moreMessages == MoreMessages.UNKNOWN) {
            updateMoreMessages(remoteFolder, localFolder, earliestDate, remoteStart);
        }

        /*
         * Now we download the actual content of messages.
         */
        return messageDownloader.downloadMessages(account, remoteFolder, localFolder, remoteMessages, false, true, true);
    }

    private static void findRemoteMessagesToDownload(Account account, Folder remoteFolder, Map<String, Long> localUidMap,
            List<Message> remoteMessages, Map<String, Message> remoteUidMap, Date earliestDate, int remoteStart,
            MessagingListener listener, MessagingController controller) throws MessagingException {

        String folderName = remoteFolder.getName();
        int remoteMessageCount = remoteFolder.getMessageCount();
        long earliestTimestamp = earliestDate != null ? earliestDate.getTime() : 0L;
        final AtomicInteger headerProgress = new AtomicInteger(0);

        for (MessagingListener l : controller.getListeners(listener)) {
            l.synchronizeMailboxHeadersStarted(account, folderName);
        }

        List<? extends Message> remoteMessageArray =
                remoteFolder.getMessages(remoteStart, remoteMessageCount, earliestDate, null);

        int messageCount = remoteMessageArray.size();

        for (Message thisMess : remoteMessageArray) {
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

        Timber.v("SYNC: Got %d messages for folder %s", remoteUidMap.size(), folderName);

        for (MessagingListener l : controller.getListeners(listener)) {
            l.synchronizeMailboxHeadersFinished(account, folderName, headerProgress.get(), remoteUidMap.size());
        }
    }

    private static MoreMessages syncRemoteDeletions(Account account, LocalFolder localFolder,
            Map<String, Long> localUidMap, Map<String, Message> remoteUidMap, MessagingListener listener,
            MessagingController controller) throws MessagingException {

        String folderName = localFolder.getName();
        MoreMessages moreMessages = localFolder.getMoreMessages();
        List<String> destroyMessageUids = new ArrayList<>();
        for (String localMessageUid : localUidMap.keySet()) {
            if (remoteUidMap.get(localMessageUid) == null) {
                destroyMessageUids.add(localMessageUid);
            }
        }

        List<LocalMessage> destroyMessages = localFolder.getMessagesByUids(destroyMessageUids);
        if (!destroyMessageUids.isEmpty()) {
            moreMessages = MoreMessages.UNKNOWN;

            localFolder.destroyMessages(destroyMessages);

            for (Message destroyMessage : destroyMessages) {
                for (MessagingListener l : controller.getListeners(listener)) {
                    l.synchronizeMailboxRemovedMessage(account, folderName, destroyMessage);
                }
            }
        }
        return moreMessages;
    }

    private static void updateMoreMessages(Folder remoteFolder, LocalFolder localFolder, Date earliestDate,
            int remoteStart) throws MessagingException, IOException {

        if (remoteStart == 1) {
            localFolder.setMoreMessages(MoreMessages.FALSE);
        } else {
            boolean moreMessagesAvailable = remoteFolder.areMoreMessagesAvailable(remoteStart, earliestDate);

            MoreMessages newMoreMessages = (moreMessagesAvailable) ? MoreMessages.TRUE : MoreMessages.FALSE;
            localFolder.setMoreMessages(newMoreMessages);
        }
    }
}
