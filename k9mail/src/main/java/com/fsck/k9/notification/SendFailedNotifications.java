package com.fsck.k9.notification;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.helper.ExceptionHelper;

import static com.fsck.k9.notification.NotificationController.NOTIFICATION_LED_BLINK_FAST;
import static com.fsck.k9.notification.NotificationController.NOTIFICATION_LED_FAILURE_COLOR;


class SendFailedNotifications {
    private final NotificationController controller;
    private final NotificationActionCreator actionBuilder;


    public SendFailedNotifications(NotificationController controller, NotificationActionCreator actionBuilder) {
        this.controller = controller;
        this.actionBuilder = actionBuilder;
    }

    public void showSendFailedNotification(Account account, Exception exception) {
        Context context = controller.getContext();
        String title = context.getString(R.string.send_failure_subject);
        String text = ExceptionHelper.getRootCauseMessage(exception);

        PendingIntent folderListPendingIntent = actionBuilder.createViewFolderListPendingIntent(account);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(getSendFailedNotificationIcon())
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(folderListPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        controller.configureNotification(builder, null, null, NOTIFICATION_LED_FAILURE_COLOR,
                NOTIFICATION_LED_BLINK_FAST, true);

        int notificationId = NotificationIds.getSendFailedNotificationId(account);
        getNotificationManager().notify(notificationId, builder.build());
    }

    public void clearSendFailedNotification(Account account) {
        int notificationId = NotificationIds.getSendFailedNotificationId(account);
        getNotificationManager().cancel(notificationId);
    }

    private int getSendFailedNotificationIcon() {
        //TODO: Use a different icon for send failure notifications
        return controller.platformSupportsVectorDrawables() ?
                R.drawable.ic_notify_new_mail_vector : R.drawable.ic_notify_new_mail;
    }

    private NotificationManager getNotificationManager() {
        return controller.getNotificationManager();
    }
}
