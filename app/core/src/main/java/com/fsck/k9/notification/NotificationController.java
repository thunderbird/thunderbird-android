package com.fsck.k9.notification;


import com.fsck.k9.Account;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;


public class NotificationController {
    private final CertificateErrorNotifications certificateErrorNotifications;
    private final AuthenticationErrorNotifications authenticationErrorNotifications;
    private final SyncNotifications syncNotifications;
    private final SendFailedNotifications sendFailedNotifications;
    private final NewMailNotifications newMailNotifications;


    NotificationController(
            CertificateErrorNotifications certificateErrorNotifications,
            AuthenticationErrorNotifications authenticationErrorNotifications,
            SyncNotifications syncNotifications,
            SendFailedNotifications sendFailedNotifications,
            NewMailNotifications newMailNotifications
    ) {
        this.certificateErrorNotifications = certificateErrorNotifications;
        this.authenticationErrorNotifications = authenticationErrorNotifications;
        this.syncNotifications = syncNotifications;
        this.sendFailedNotifications = sendFailedNotifications;
        this.newMailNotifications = newMailNotifications;
    }

    public void showCertificateErrorNotification(Account account, boolean incoming) {
        certificateErrorNotifications.showCertificateErrorNotification(account, incoming);
    }

    public void clearCertificateErrorNotifications(Account account, boolean incoming) {
        certificateErrorNotifications.clearCertificateErrorNotifications(account, incoming);
    }

    public void showAuthenticationErrorNotification(Account account, boolean incoming) {
        authenticationErrorNotifications.showAuthenticationErrorNotification(account, incoming);
    }

    public void clearAuthenticationErrorNotification(Account account, boolean incoming) {
        authenticationErrorNotifications.clearAuthenticationErrorNotification(account, incoming);
    }

    public void showSendingNotification(Account account) {
        syncNotifications.showSendingNotification(account);
    }

    public void clearSendingNotification(Account account) {
        syncNotifications.clearSendingNotification(account);
    }

    public void showSendFailedNotification(Account account, Exception exception) {
        sendFailedNotifications.showSendFailedNotification(account, exception);
    }

    public void clearSendFailedNotification(Account account) {
        sendFailedNotifications.clearSendFailedNotification(account);
    }

    public void showFetchingMailNotification(Account account, LocalFolder folder) {
        syncNotifications.showFetchingMailNotification(account, folder);
    }

    public void clearFetchingMailNotification(Account account) {
        syncNotifications.clearFetchingMailNotification(account);
    }

    public void addNewMailNotification(Account account, LocalMessage message, int previousUnreadMessageCount) {
        newMailNotifications.addNewMailNotification(account, message, previousUnreadMessageCount);
    }

    public void removeNewMailNotification(Account account, MessageReference messageReference) {
        newMailNotifications.removeNewMailNotification(account, messageReference);
    }

    public void clearNewMailNotifications(Account account) {
        newMailNotifications.clearNewMailNotifications(account);
    }
}
