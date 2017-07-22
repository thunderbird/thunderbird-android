package com.fsck.k9.controller;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.fsck.k9.Account;
import com.fsck.k9.Account.Expunge;
import com.fsck.k9.K9;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalFolder.MoreMessages;
import com.fsck.k9.mailstore.LocalMessage;
import timber.log.Timber;

/* This class contains code that used to be present directly in the MessagingController. It used to represent a common
synchronization mechanism for all types of accounts. Currently, it is used for synchronization of POP3 and WebDAV
accounts only
 */
class LegacySyncInteractor {

    static void performSync(Account account, String folderName, MessagingListener listener,
            MessagingController controller, MessageDownloader messageDownloader) {

        Exception commandException = null;
        LocalFolder localFolder = null;
        Folder remoteFolder = null;

        try {
            Timber.d("SYNC: About to process pending commands for account %s", account.getDescription());

            try {
                controller.processPendingCommandsSynchronous(account);
            } catch (Exception e) {
                controller.addErrorMessage(account, null, e);
                Timber.e(e, "Failure processing command, but allow message sync attempt");
                commandException = e;
            }

            /*
             * Get the message list from the local store and create an index of
             * the uids within the list.
             */
            Timber.v("SYNC: About to get local folder %s", folderName);
            localFolder = SyncUtils.getOpenedLocalFolder(account, folderName);
            localFolder.updateLastUid();

            Store remoteStore = account.getRemoteStore();
            Timber.v("SYNC: About to get remote folder %s", folderName);
            remoteFolder = remoteStore.getFolder(folderName);

            if (!SyncUtils.verifyOrCreateRemoteSpecialFolder(account, folderName, remoteFolder, listener, controller)) {
                return;
            }

            /*
             * Open the remote folder. This pre-loads certain metadata like message count.
             */
            Timber.v("SYNC: About to open remote folder %s", folderName);
            remoteFolder.open(Folder.OPEN_MODE_RW);
            if (Expunge.EXPUNGE_ON_POLL == account.getExpungePolicy()) {
                Timber.d("SYNC: Expunging folder %s:%s", account.getDescription(), folderName);
                remoteFolder.expunge();
            }

            Map<String, Long> localUidMap = localFolder.getAllMessagesAndEffectiveDates();

            /*
             * Get the remote message count.
             */
            int remoteMessageCount = remoteFolder.getMessageCount();
            Timber.v("SYNC: Remote message count for folder %s is %d", folderName, remoteMessageCount);

            int visibleLimit = localFolder.getVisibleLimit();
            if (visibleLimit < 0) {
                visibleLimit = K9.DEFAULT_VISIBLE_LIMIT;
            }

            final List<Message> remoteMessages = new ArrayList<>();
            Map<String, Message> remoteUidMap = new HashMap<>();

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

                findRemoteMessagesToDownload(account, remoteFolder, localUidMap, remoteMessages, remoteUidMap,
                        earliestDate, remoteStart, listener, controller);

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
            int newMessages =  messageDownloader.downloadMessages(account, remoteFolder, localFolder, remoteMessages,
                    true, true);
            int unreadMessageCount = localFolder.getUnreadMessageCount();
            for (MessagingListener l : controller.getListeners()) {
                l.folderStatusChanged(account, folderName, unreadMessageCount);
            }
            /* Notify listeners that we're finally done. */

            localFolder.setLastChecked(System.currentTimeMillis());
            localFolder.setStatus(null);

            Timber.d("Done synchronizing folder %s:%s @ %tc with %d new messages",
                    account.getDescription(),
                    folderName,
                    System.currentTimeMillis(),
                    newMessages);

            for (MessagingListener l : controller.getListeners(listener)) {
                l.synchronizeMailboxFinished(account, folderName, remoteMessageCount, newMessages);
            }


            if (commandException != null) {
                String rootMessage = MessagingController.getRootCauseMessage(commandException);
                Timber.e("Root cause failure in %s:%s was '%s'",
                        account.getDescription(), folderName, rootMessage);
                localFolder.setStatus(rootMessage);
                for (MessagingListener l : controller.getListeners(listener)) {
                    l.synchronizeMailboxFailed(account, folderName, rootMessage);
                }
            }

            Timber.i("Done synchronizing folder %s:%s", account.getDescription(), folderName);

        } catch (AuthenticationFailedException e) {
            controller.handleAuthenticationFailure(account, true);
            for (MessagingListener l : controller.getListeners(listener)) {
                l.synchronizeMailboxFailed(account, folderName, "Authentication failure");
            }
        } catch (Exception e) {
            Timber.e(e, "synchronizeMailbox");
            // If we don't set the last checked, it can try too often during
            // failure conditions
            String rootMessage = MessagingController.getRootCauseMessage(e);
            if (localFolder != null) {
                try {
                    localFolder.setStatus(rootMessage);
                    localFolder.setLastChecked(System.currentTimeMillis());
                } catch (MessagingException me) {
                    Timber.e(e, "Could not set last checked on folder %s:%s",
                            account.getDescription(), localFolder.getName());
                }
            }

            for (MessagingListener l : controller.getListeners(listener)) {
                l.synchronizeMailboxFailed(account, folderName, rootMessage);
            }
            controller.notifyUserIfCertificateProblem(account, e, true);
            controller.addErrorMessage(account, null, e);
            Timber.e("Failed synchronizing folder %s:%s @ %tc", account.getDescription(), folderName,
                    System.currentTimeMillis());

        } finally {
            MessagingController.closeFolder(remoteFolder);
            MessagingController.closeFolder(localFolder);
        }
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
