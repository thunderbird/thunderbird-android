package com.fsck.k9.notification;


import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationQuickDelete;
import com.fsck.k9.R;


abstract class BaseNotifications {
    protected static final String NOTIFICATION_GROUP_KEY = "newMailNotifications";


    protected final Context context;
    protected final NotificationController controller;
    protected final NotificationActionCreator actionCreator;


    protected BaseNotifications(NotificationController controller, NotificationActionCreator actionCreator) {
        this.context = controller.getContext();
        this.controller = controller;
        this.actionCreator = actionCreator;
    }

    protected NotificationCompat.Builder createBigTextStyleNotification(Account account, NotificationHolder holder,
            int notificationId) {
        String accountName = controller.getAccountName(account);
        NotificationContent content = holder.content;

        NotificationCompat.Builder builder = createAndInitializeNotificationBuilder(account)
                .setTicker(content.summary)
                .setGroup(NOTIFICATION_GROUP_KEY)
                .setContentTitle(content.sender)
                .setContentText(content.subject)
                .setSubText(accountName);

        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle(builder);
        style.bigText(content.preview);

        builder.setStyle(style);

        PendingIntent contentIntent = actionCreator.createViewMessagePendingIntent(
                content.messageReference, notificationId);
        builder.setContentIntent(contentIntent);

        return builder;
    }

    protected NotificationCompat.Builder createAndInitializeNotificationBuilder(Account account) {
        return new NotificationCompat.Builder(context)
                .setSmallIcon(getNewMailNotificationIcon())
                .setColor(account.getChipColor())
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true);
    }

    protected boolean isDeleteActionEnabled() {
        NotificationQuickDelete deleteOption = K9.getNotificationQuickDeleteBehaviour();
        return deleteOption == NotificationQuickDelete.ALWAYS || deleteOption == NotificationQuickDelete.FOR_SINGLE_MSG;
    }

    private int getNewMailNotificationIcon() {
        return controller.platformSupportsVectorDrawables() ?
                R.drawable.ic_notify_new_mail_vector : R.drawable.ic_notify_new_mail;
    }
}
