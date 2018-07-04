package com.fsck.k9.notification;


import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.fsck.k9.Account;
import com.fsck.k9.core.R;
import com.fsck.k9.helper.ExceptionHelper;

import static com.fsck.k9.notification.NotificationHelper.NOTIFICATION_LED_BLINK_FAST;
import static com.fsck.k9.notification.NotificationHelper.NOTIFICATION_LED_FAILURE_COLOR;


class SendFailedNotifications {
    private final NotificationHelper notificationHelper;
    private final NotificationActionCreator actionBuilder;


    public SendFailedNotifications(NotificationHelper notificationHelper, NotificationActionCreator actionBuilder) {
        this.notificationHelper = notificationHelper;
        this.actionBuilder = actionBuilder;
    }

    public void showSendFailedNotification(Account account, Exception exception) {
        Context context = notificationHelper.getContext();
        String title = context.getString(R.string.send_failure_subject);
        String text = ExceptionHelper.getRootCauseMessage(exception);

        int notificationId = NotificationIds.getSendFailedNotificationId(account);
        PendingIntent folderListPendingIntent = actionBuilder.createViewFolderListPendingIntent(
                account, notificationId);

        NotificationCompat.Builder builder = notificationHelper.createNotificationBuilder()
                .setSmallIcon(getSendFailedNotificationIcon())
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(folderListPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ERROR);

        notificationHelper.configureNotification(builder, null, null, NOTIFICATION_LED_FAILURE_COLOR,
                NOTIFICATION_LED_BLINK_FAST, true);

        getNotificationManager().notify(notificationId, builder.build());
    }

    public void clearSendFailedNotification(Account account) {
        int notificationId = NotificationIds.getSendFailedNotificationId(account);
        getNotificationManager().cancel(notificationId);
    }

    private int getSendFailedNotificationIcon() {
        //TODO: Use a different icon for send failure notifications
        return R.drawable.notification_icon_new_mail;
    }

    private NotificationManagerCompat getNotificationManager() {
        return notificationHelper.getNotificationManager();
    }
}
