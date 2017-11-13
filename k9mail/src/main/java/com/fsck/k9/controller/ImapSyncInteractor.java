package com.fsck.k9.controller;


import com.fsck.k9.Account;
import com.fsck.k9.Account.Expunge;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.notification.NotificationController;
import timber.log.Timber;


class ImapSyncInteractor {

    private final SyncHelper syncHelper;
    private final FlagSyncHelper flagSyncHelper;
    private final MessagingController controller;
    private final MessageDownloader messageDownloader;
    private final NotificationController notificationController;

    ImapSyncInteractor(SyncHelper syncHelper, FlagSyncHelper flagSyncHelper, MessagingController controller,
            MessageDownloader messageDownloader, NotificationController notificationController) {
        this.syncHelper = syncHelper;
        this.flagSyncHelper = flagSyncHelper;
        this.controller = controller;
        this.messageDownloader = messageDownloader;
        this.notificationController = notificationController;
    }

    void performSync(Account account, String folderName, MessagingListener listener, Folder providedRemoteFolder) {
        for (MessagingListener l : controller.getListeners(listener)) {
            l.synchronizeMailboxStarted(account, folderName);
        }

        Exception commandException = null;
        LocalFolder localFolder = null;
        ImapFolder imapFolder = null;

        try {
            Timber.d("SYNC: About to process pending commands for account %s", account.getDescription());

            try {
                controller.processPendingCommandsSynchronous(account);
            } catch (Exception e) {
                Timber.e(e, "Failure processing command, but allow message sync attempt");
                commandException = e;
            }

            localFolder = getOpenedLocalFolder(account, folderName);
            localFolder.updateLastUid();
            if (providedRemoteFolder != null) {
                if (providedRemoteFolder instanceof ImapFolder) {
                    Timber.v("SYNC: using providedRemoteFolder %s", folderName);
                    imapFolder = (ImapFolder) providedRemoteFolder;
                } else {
                    throw new IllegalArgumentException("A non-IMAP account was provided to ImapSyncInteractor");
                }
            } else {
                imapFolder = getImapFolder(account, folderName);

                if (!syncHelper.verifyOrCreateRemoteSpecialFolder(account, folderName, imapFolder, listener, controller)) {
                    return;
                }

                Timber.v("SYNC: About to open remote folder %s", folderName);

                if (Expunge.EXPUNGE_ON_POLL == account.getExpungePolicy()) {
                    Timber.d("SYNC: Expunging folder %s:%s", account.getDescription(), folderName);
                    imapFolder.expunge();
                }
                imapFolder.open(Folder.OPEN_MODE_RO);
            }

            notificationController.clearAuthenticationErrorNotification(account, true);

            int remoteMessageCount = imapFolder.getMessageCount();
            if (remoteMessageCount < 0) {
                throw new IllegalStateException("Message count " + remoteMessageCount + " for folder " + folderName);
            }

            Timber.v("SYNC: Remote message count for folder %s is %d", folderName, remoteMessageCount);

            int newMessages;
            NonQresyncExtensionHandler handler = new NonQresyncExtensionHandler(syncHelper, flagSyncHelper,
                    controller, messageDownloader);
            newMessages = handler.continueSync(account, localFolder, imapFolder, listener);

            int unreadMessageCount = localFolder.getUnreadMessageCount();
            for (MessagingListener l : controller.getListeners()) {
                l.folderStatusChanged(account, folderName, unreadMessageCount);
            }

            localFolder.setLastChecked(System.currentTimeMillis());
            localFolder.setStatus(null);

            Timber.d("Done synchronizing folder %s:%s @ %tc with %d new messages",
                    account.getDescription(),
                    folderName,
                    System.currentTimeMillis(),
                    newMessages);

            for (MessagingListener l : controller.getListeners(listener)) {
                l.synchronizeMailboxFinished(account, folderName, imapFolder.getMessageCount(), newMessages);
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
            Timber.e("Failed synchronizing folder %s:%s @ %tc", account.getDescription(), folderName,
                    System.currentTimeMillis());

        } finally {
            closeFolder(localFolder);
            if (providedRemoteFolder == null) {
                closeFolder(imapFolder);
            }
        }
    }

    private LocalFolder getOpenedLocalFolder(Account account, String folderName) throws MessagingException {
        Timber.v("SYNC: About to get local folder %s and open it", folderName);
        final LocalStore localStore = account.getLocalStore();
        LocalFolder localFolder = localStore.getFolder(folderName);
        localFolder.open(Folder.OPEN_MODE_RW);
        return localFolder;
    }

    private ImapFolder getImapFolder(Account account, String folderName) throws MessagingException {
        Store remoteStore = account.getRemoteStore();
        Timber.v("SYNC: About to get remote IMAP folder %s", folderName);
        Folder remoteFolder = remoteStore.getFolder(folderName);
        if (!(remoteFolder instanceof ImapFolder)) {
            throw new IllegalArgumentException("A non-IMAP account was provided to ImapSyncInteractor");
        }
        return  (ImapFolder) remoteFolder;
    }

    private void closeFolder(Folder folder) {
        if (folder != null) {
            folder.close();
        }
    }
}