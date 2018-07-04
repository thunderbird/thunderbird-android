package com.fsck.k9.notification;


import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.fsck.k9.Account;
import com.fsck.k9.core.R;

import static com.fsck.k9.notification.NotificationHelper.NOTIFICATION_LED_BLINK_FAST;
import static com.fsck.k9.notification.NotificationHelper.NOTIFICATION_LED_FAILURE_COLOR;


class CertificateErrorNotifications {
    private final NotificationHelper notificationHelper;
    private final NotificationActionCreator actionCreator;


    public CertificateErrorNotifications(NotificationHelper notificationHelper,
            NotificationActionCreator actionCreator) {
        this.notificationHelper = notificationHelper;
        this.actionCreator = actionCreator;
    }

    public void showCertificateErrorNotification(Account account, boolean incoming) {
        int notificationId = NotificationIds.getCertificateErrorNotificationId(account, incoming);
        Context context = notificationHelper.getContext();

        PendingIntent editServerSettingsPendingIntent = createContentIntent(account, incoming);
        String title = context.getString(R.string.notification_certificate_error_title, account.getDescription());
        String text = context.getString(R.string.notification_certificate_error_text);

        NotificationCompat.Builder builder = notificationHelper.createNotificationBuilder()
                .setSmallIcon(getCertificateErrorNotificationIcon())
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(editServerSettingsPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ERROR);

        notificationHelper.configureNotification(builder, null, null,
                NOTIFICATION_LED_FAILURE_COLOR,
                NOTIFICATION_LED_BLINK_FAST, true);

        getNotificationManager().notify(notificationId, builder.build());
    }

    public void clearCertificateErrorNotifications(Account account, boolean incoming) {
        int notificationId = NotificationIds.getCertificateErrorNotificationId(account, incoming);
        getNotificationManager().cancel(notificationId);
    }

    PendingIntent createContentIntent(Account account, boolean incoming) {
        return incoming ?
                actionCreator.getEditIncomingServerSettingsIntent(account) :
                actionCreator.getEditOutgoingServerSettingsIntent(account);
    }

    private int getCertificateErrorNotificationIcon() {
        //TODO: Use a different icon for certificate error notifications
        return R.drawable.notification_icon_new_mail;
    }

    private NotificationManagerCompat getNotificationManager() {
        return notificationHelper.getNotificationManager();
    }
}
