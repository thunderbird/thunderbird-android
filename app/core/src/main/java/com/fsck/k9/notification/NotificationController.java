package com.fsck.k9.notification;


import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;

import com.fsck.k9.Account;
import com.fsck.k9.DI;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalMessage;


public class NotificationController {
    private final CertificateErrorNotifications certificateErrorNotifications;
    private final AuthenticationErrorNotifications authenticationErrorNotifications;
    private final SyncNotifications syncNotifications;
    private final SendFailedNotifications sendFailedNotifications;
    private final NewMailNotifications newMailNotifications;


    public static NotificationController newInstance(Context context) {
        Context appContext = context.getApplicationContext();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(appContext);
        return new NotificationController(appContext, notificationManager);
    }

    public static boolean platformSupportsExtendedNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean platformSupportsLockScreenNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }


    NotificationController(Context context, NotificationManagerCompat notificationManager) {
        NotificationHelper notificationHelper = new NotificationHelper(context, notificationManager);
        NotificationActionCreator actionBuilder = DI.get(NotificationActionCreator.class);
        certificateErrorNotifications = new CertificateErrorNotifications(notificationHelper, actionBuilder);
        authenticationErrorNotifications = new AuthenticationErrorNotifications(notificationHelper, actionBuilder);
        syncNotifications = new SyncNotifications(notificationHelper, actionBuilder);
        sendFailedNotifications = new SendFailedNotifications(notificationHelper, actionBuilder);
        newMailNotifications = NewMailNotifications.newInstance(notificationHelper, actionBuilder);
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

    public void showFetchingMailNotification(Account account, Folder folder) {
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
