package com.fsck.k9.controller.tasks;


import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.controller.ControllerUtils;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingControllerCommands.PendingAppend;
import com.fsck.k9.controller.MessagingControllerCommands.PendingCommand;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.UnavailableAccountException;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.TransportProvider;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.UnavailableStorageException;
import com.fsck.k9.notification.NotificationController;
import timber.log.Timber;

import static com.fsck.k9.K9.MAX_SEND_ATTEMPTS;


public class SendPendingMessagesTask implements Runnable {
    private Context context;
    private NotificationController notificationController;
    private TransportProvider transportProvider;
    private ConcurrentHashMap<String, AtomicInteger> sendCount;
    private Account account;
    private Set<MessagingListener> listeners;
    private MessagingController controller;

    public SendPendingMessagesTask(MessagingController controller, Context context, NotificationController notificationController,
            TransportProvider transportProvider, ConcurrentHashMap<String, AtomicInteger> sendCount,
            Account account, Set<MessagingListener> listeners) {
        this.controller = controller;
        this.context = context;
        this.notificationController = notificationController;
        this.transportProvider = transportProvider;
        this.sendCount = sendCount;
        this.account = account;
        this.listeners = listeners;
    }

    @Override
    public void run() {
        if (!account.isAvailable(context)) {
            throw new UnavailableAccountException();
        }
        if (messagesPendingSend()) {

            showSendingNotificationIfNecessary();

            try {
                sendPendingMessagesSynchronous();
            } finally {
                clearSendingNotificationIfNecessary();
            }
        }
    }

    private void showSendingNotificationIfNecessary() {
        if (account.isShowOngoing()) {
            notificationController.showSendingNotification(account);
        }
    }

    private void clearSendingNotificationIfNecessary() {
        if (account.isShowOngoing()) {
            notificationController.clearSendingNotification(account);
        }
    }

    private boolean messagesPendingSend() {
        Folder localFolder = null;
        try {
            localFolder = account.getLocalStore().getFolder(
                    account.getOutboxFolderName());
            if (!localFolder.exists()) {
                return false;
            }

            localFolder.open(Folder.OPEN_MODE_RW);

            if (localFolder.getMessageCount() > 0) {
                return true;
            }
        } catch (Exception e) {
            Timber.e(e, "Exception while checking for unsent messages");
        } finally {
            ControllerUtils.closeFolder(localFolder);
        }
        return false;
    }

    /**
     * Attempt to send any messages that are sitting in the Outbox.
     */
    @VisibleForTesting
    void sendPendingMessagesSynchronous() {
        LocalFolder localFolder = null;
        Exception lastFailure = null;
        boolean wasPermanentFailure = false;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(
                    account.getOutboxFolderName());
            if (!localFolder.exists()) {
                Timber.v("Outbox does not exist");
                return;
            }
            for (MessagingListener l : listeners) {
                l.sendPendingMessagesStarted(account);
            }
            localFolder.open(Folder.OPEN_MODE_RW);

            List<LocalMessage> localMessages = localFolder.getMessages(null);
            int progress = 0;
            int todo = localMessages.size();
            for (MessagingListener l : listeners) {
                l.synchronizeMailboxProgress(account, account.getSentFolderName(), progress, todo);
            }
            /*
             * The profile we will use to pull all of the content
             * for a given local message into memory for sending.
             */
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.BODY);

            Timber.i("Scanning folder '%s' (%d) for messages to send",
                    account.getOutboxFolderName(), localFolder.getDatabaseId());

            Transport transport = transportProvider.getTransport(K9.app, account);

            for (LocalMessage message : localMessages) {
                if (message.isSet(Flag.DELETED)) {
                    message.destroy();
                    continue;
                }
                try {
                    AtomicInteger count = new AtomicInteger(0);
                    AtomicInteger oldCount = sendCount.putIfAbsent(message.getUid(), count);
                    if (oldCount != null) {
                        count = oldCount;
                    }

                    Timber.i("Send count for message %s is %d", message.getUid(), count.get());

                    if (count.incrementAndGet() > K9.MAX_SEND_ATTEMPTS) {
                        Timber.e("Send count for message %s can't be delivered after %d attempts. " +
                                "Giving up until the user restarts the device", message.getUid(), MAX_SEND_ATTEMPTS);
                        notificationController.showSendFailedNotification(account,
                                new MessagingException(message.getSubject()));
                        continue;
                    }

                    localFolder.fetch(Collections.singletonList(message), fp, null);
                    try {
                        if (message.getHeader(K9.IDENTITY_HEADER).length > 0) {
                            Timber.v("The user has set the Outbox and Drafts folder to the same thing. " +
                                    "This message appears to be a draft, so K-9 will not send it");
                            continue;
                        }

                        message.setFlag(Flag.X_SEND_IN_PROGRESS, true);

                        Timber.i("Sending message with UID %s", message.getUid());
                        transport.sendMessage(message);

                        message.setFlag(Flag.X_SEND_IN_PROGRESS, false);
                        message.setFlag(Flag.SEEN, true);
                        progress++;
                        for (MessagingListener l : listeners) {
                            l.synchronizeMailboxProgress(account, account.getSentFolderName(), progress, todo);
                        }
                        moveOrDeleteSentMessage(account, localStore, localFolder, message);
                    } catch (AuthenticationFailedException e) {
                        lastFailure = e;
                        wasPermanentFailure = false;

                        handleAuthenticationFailure(account, false);
                        handleSendFailure(account, localStore, localFolder, message, e, wasPermanentFailure);
                    } catch (CertificateValidationException e) {
                        lastFailure = e;
                        wasPermanentFailure = false;

                        notifyUserIfCertificateProblem(account, e, false);
                        handleSendFailure(account, localStore, localFolder, message, e, wasPermanentFailure);
                    } catch (MessagingException e) {
                        lastFailure = e;
                        wasPermanentFailure = e.isPermanentFailure();

                        handleSendFailure(account, localStore, localFolder, message, e, wasPermanentFailure);
                    } catch (Exception e) {
                        lastFailure = e;
                        wasPermanentFailure = true;

                        handleSendFailure(account, localStore, localFolder, message, e, wasPermanentFailure);
                    }
                } catch (Exception e) {
                    lastFailure = e;
                    wasPermanentFailure = false;
                    Timber.e(e, "Failed to fetch message for sending");
                    notifySynchronizeMailboxFailed(account, localFolder, e);
                }
            }

            for (MessagingListener l : listeners) {
                l.sendPendingMessagesCompleted(account);
            }

            if (lastFailure != null) {
                if (wasPermanentFailure) {
                    notificationController.showSendFailedNotification(account, lastFailure);
                } else {
                    notificationController.showSendFailedNotification(account, lastFailure);
                }
            }
        } catch (UnavailableStorageException e) {
            Timber.i("Failed to send pending messages because storage is not available - trying again later.");
            throw new UnavailableAccountException(e);
        } catch (Exception e) {
            Timber.v(e, "Failed to send pending messages");

            for (MessagingListener l : listeners) {
                l.sendPendingMessagesFailed(account);
            }
        } finally {
            if (lastFailure == null) {
                notificationController.clearSendFailedNotification(account);
            }
            ControllerUtils.closeFolder(localFolder);
        }
    }

    private void moveOrDeleteSentMessage(Account account, LocalStore localStore,
            LocalFolder localFolder, LocalMessage message) throws MessagingException {
        if (!account.hasSentFolder()) {
            Timber.i("Account does not have a sent mail folder; deleting sent message");
            message.setFlag(Flag.DELETED, true);
        } else {
            LocalFolder localSentFolder = localStore.getFolder(account.getSentFolderName());
            Timber.i("Moving sent message to folder '%s' (%d)", account.getSentFolderName(), localSentFolder.getDatabaseId());

            localFolder.moveMessages(Collections.singletonList(message), localSentFolder);

            Timber.i("Moved sent message to folder '%s' (%d)", account.getSentFolderName(), localSentFolder.getDatabaseId());

            PendingCommand command = PendingAppend.create(localSentFolder.getName(), message.getUid());
            controller.queuePendingCommand(account, command);
            controller.processPendingCommands(account);
        }
    }

    private void handleSendFailure(Account account, Store localStore, Folder localFolder, Message message,
            Exception exception, boolean permanentFailure) throws MessagingException {

        Timber.e(exception, "Failed to send message");

        if (permanentFailure) {
            moveMessageToDraftsFolder(account, localFolder, localStore, message);
        }

        message.setFlag(Flag.X_SEND_FAILED, true);

        notifySynchronizeMailboxFailed(account, localFolder, exception);
    }

    private void handleAuthenticationFailure(Account account, boolean incoming) {
        notificationController.showAuthenticationErrorNotification(account, incoming);
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

    private void notifySynchronizeMailboxFailed(Account account, Folder localFolder, Exception exception) {
        String folderName = localFolder.getName();
        String errorMessage = ControllerUtils.getRootCauseMessage(exception);
        for (MessagingListener listener : listeners) {
            listener.synchronizeMailboxFailed(account, folderName, errorMessage);
        }
    }

    private void moveMessageToDraftsFolder(Account account, Folder localFolder, Store localStore, Message message)
            throws MessagingException {
        LocalFolder draftsFolder = (LocalFolder) localStore.getFolder(account.getDraftsFolderName());
        localFolder.moveMessages(Collections.singletonList(message), draftsFolder);
    }
}
