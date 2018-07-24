package com.fsck.k9.notification;


import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationQuickMoveTrigger;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.controller.MessagingController;


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
        return notificationHelper.createNotificationBuilder()
                .setSmallIcon(getNewMailNotificationIcon())
                .setColor(account.getChipColor())
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_EMAIL);
    }

    protected boolean isQuickMoveEnabled() {
        NotificationQuickMoveTrigger moveAction = K9.getNotificationQuickMoveTrigger();
        return moveAction == NotificationQuickMoveTrigger.ALWAYS || moveAction == NotificationQuickMoveTrigger.FOR_SINGLE_MSG;
    }

    protected boolean isArchiveActionAvailable(Account account) {
        String archiveFolderName = account.getArchiveFolder();
        return archiveFolderName != null && isMovePossible(account, archiveFolderName);
    }

    protected boolean isMovePossible(Account account, String destinationFolderName) {
        if (K9.FOLDER_NONE.equals(destinationFolderName)) {
            return false;
        }

        MessagingController controller = createMessagingController();
        return controller.isMoveCapable(account);
    }

    protected void addQuickMoveAction(Account account, Builder builder, NotificationContent content, int notificationId) {
        if (!isQuickMoveEnabled()) {
            return;
        }
        K9.NotificationQuickMoveType quickMoveType = K9.getNotificationQuickMoveType();
        if(K9.NotificationQuickMoveType.DELETE == quickMoveType) {
            addDeleteAction(builder, content, notificationId);
        }
        if(K9.NotificationQuickMoveType.ARCHIVE == quickMoveType) {
            addArchiveAction(account, builder, content, notificationId);
        }
    }

    protected BigTextStyle createBigTextStyle(Builder builder) {
        return new BigTextStyle(builder);
    }

    private int getNewMailNotificationIcon() {
        return resourceProvider.getIconNewMail();
    }

    private void addDeleteAction(Builder builder, NotificationContent content, int notificationId) {
        int icon = resourceProvider.getIconDelete();
        String title = resourceProvider.actionDelete();

        MessageReference messageReference = content.messageReference;
        PendingIntent action = actionCreator.createDeleteMessagePendingIntent(messageReference, notificationId);

        builder.addAction(icon, title, action);
    }

    private void addArchiveAction(Account account, Builder builder, NotificationContent content, int notificationId) {
        if(!isArchiveActionAvailable(account))
        {
            return;
        }
        int icon = resourceProvider.getIconArchive();
        String title = resourceProvider.actionArchive();

        MessageReference messageReference = content.messageReference;
        PendingIntent action = actionCreator.createArchiveMessagePendingIntent(messageReference, notificationId);

        builder.addAction(icon, title, action);
    }

    MessagingController createMessagingController() {
        return MessagingController.getInstance(context);
    }
}
