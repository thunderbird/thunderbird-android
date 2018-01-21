package com.fsck.k9.controller.tasks;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.controller.ControllerUtils;
import com.fsck.k9.controller.IMessageController;
import com.fsck.k9.controller.MessageDownloader;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.notification.NotificationController;
import timber.log.Timber;


public class LoadMessageRemoteTask implements Runnable {
    private final IMessageController controller;
    private final Context context;
    private final NotificationController notificationController;
    private final Set<MessagingListener> listeners;
    private final MessageDownloader messageDownloader;
    private final Account account;
    private final String folder;
    private final String uid;
    private final MessagingListener listener;
    private final boolean loadPartialFromSearch;

    public LoadMessageRemoteTask(IMessageController controller, Context context,
            NotificationController notificationController,
            Set<MessagingListener> listeners, MessageDownloader messageDownloader, Account account,
            String folder, String uid, MessagingListener listener, boolean loadPartialFromSearch) {
        this.controller = controller;
        this.context = context;
        this.notificationController = notificationController;
        this.listeners = listeners;
        this.messageDownloader = messageDownloader;
        this.account = account;
        this.folder = folder;
        this.uid = uid;
        this.listener = listener;
        this.loadPartialFromSearch = loadPartialFromSearch;
    }


    @Override
    public void run() {
        loadMessageRemoteSynchronous();
    }

    @VisibleForTesting
    protected boolean loadMessageRemoteSynchronous() {
        Set<MessagingListener> listeners = new HashSet<>(this.listeners);
        listeners.add(listener);
        Folder remoteFolder = null;
        LocalFolder localFolder = null;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folder);
            localFolder.open(Folder.OPEN_MODE_RW);

            LocalMessage message = localFolder.getMessage(uid);

            if (uid.startsWith(K9.LOCAL_UID_PREFIX)) {
                Timber.w("Message has local UID so cannot download fully.");
                // ASH move toast
                android.widget.Toast.makeText(context,
                        "Message has local UID so cannot download fully",
                        android.widget.Toast.LENGTH_LONG).show();
                // TODO: Using X_DOWNLOADED_FULL is wrong because it's only a partial message. But
                // one we can't download completely. Maybe add a new flag; X_PARTIAL_MESSAGE ?
                message.setFlag(Flag.X_DOWNLOADED_FULL, true);
                message.setFlag(Flag.X_DOWNLOADED_PARTIAL, false);
            } else {
                Store remoteStore = account.getRemoteStore();
                remoteFolder = remoteStore.getFolder(folder);
                remoteFolder.open(Folder.OPEN_MODE_RW);

                // Get the remote message and fully download it
                Message remoteMessage = remoteFolder.getMessage(uid);

                if (loadPartialFromSearch) {
                    messageDownloader.downloadMessages(controller, notificationController, context, account, remoteFolder, localFolder,
                            Collections.singletonList(remoteMessage), false, false, this.listeners);
                } else {
                    FetchProfile fp = new FetchProfile();
                    fp.add(FetchProfile.Item.BODY);
                    fp.add(FetchProfile.Item.FLAGS);
                    remoteFolder.fetch(Collections.singletonList(remoteMessage), fp, null);
                    localFolder.appendMessages(Collections.singletonList(remoteMessage));
                }

                message = localFolder.getMessage(uid);

                if (!loadPartialFromSearch) {
                    message.setFlag(Flag.X_DOWNLOADED_FULL, true);
                }
            }

            // now that we have the full message, refresh the headers
            for (MessagingListener l : listeners) {
                l.loadMessageRemoteFinished(account, folder, uid);
            }

            return true;
        } catch (Exception e) {
            for (MessagingListener l : listeners) {
                l.loadMessageRemoteFailed(account, folder, uid, e);
            }
            notifyUserIfCertificateProblem(account, e, true);
            Timber.e(e, "Error while loading remote message");
            return false;
        } finally {
            ControllerUtils.closeFolder(remoteFolder);
            ControllerUtils.closeFolder(localFolder);
        }
    }

    private void notifyUserIfCertificateProblem(Account account, Exception exception, boolean incoming) {
        if (!(exception instanceof CertificateValidationException)) {
            return;
        }

        CertificateValidationException cve = (CertificateValidationException) exception;
        if (!cve.needsUserAttention()) {
            return;
        }

        notificationController.showCertificateErrorNotification(account, incoming);
    }

}
