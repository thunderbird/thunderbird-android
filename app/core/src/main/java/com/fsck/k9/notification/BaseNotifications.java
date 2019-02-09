package com.fsck.k9.notification;


import android.app.PendingIntent;
import android.content.Context;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.app.NotificationCompat.Builder;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationQuickDelete;


abstract class BaseNotifications {
    protected final Context context;
    protected final NotificationHelper notificationHelper;
    protected final NotificationActionCreator actionCreator;
    protected final NotificationResourceProvider resourceProvider;


    protected BaseNotifications(NotificationHelper notificationHelper, NotificationActionCreator actionCreator,
            NotificationResourceProvider resourceProvider) {
        this.context = notificationHelper.getContext();
        this.notificationHelper = notificationHelper;
        this.actionCreator = actionCreator;
        this.resourceProvider = resourceProvider;
    }

    protected NotificationCompat.Builder createBigTextStyleNotification(Account account, NotificationHolder holder,
            int notificationId) {
        String accountName = notificationHelper.getAccountName(account);
        NotificationContent content = holder.content;
        String groupKey = NotificationGroupKeys.getGroupKey(account);

        NotificationCompat.Builder builder = createAndInitializeNotificationBuilder(account)
                .setTicker(content.summary)
                .setGroup(groupKey)
                .setContentTitle(content.sender)
                .setContentText(content.subject)
                .setSubText(accountName);

        NotificationCompat.BigTextStyle style = createBigTextStyle(builder);
        style.bigText(content.preview);

        builder.setStyle(style);

        PendingIntent contentIntent = actionCreator.createViewMessagePendingIntent(
                content.messageReference, notificationId);
        builder.setContentIntent(contentIntent);

        return builder;
    }

    protected NotificationCompat.Builder createAndInitializeNotificationBuilder(Account account) {
        return notificationHelper.createNotificationBuilder(account,
                NotificationChannelManager.ChannelType.MESSAGES)
                .setSmallIcon(getNewMailNotificationIcon())
                .setColor(account.getChipColor())
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_EMAIL);
    }

    protected boolean isDeleteActionEnabled() {
        NotificationQuickDelete deleteOption = K9.getNotificationQuickDeleteBehaviour();
        return deleteOption == NotificationQuickDelete.ALWAYS || deleteOption == NotificationQuickDelete.FOR_SINGLE_MSG;
    }

    protected BigTextStyle createBigTextStyle(Builder builder) {
        return new BigTextStyle(builder);
    }

    private int getNewMailNotificationIcon() {
        return resourceProvider.getIconNewMail();
    }
}
