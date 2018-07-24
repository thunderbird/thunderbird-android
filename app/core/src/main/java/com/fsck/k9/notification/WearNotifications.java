package com.fsck.k9.notification;


import java.util.ArrayList;

import android.app.Notification;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationCompat.WearableExtender;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.K9.NotificationQuickMoveType;


class WearNotifications extends BaseNotifications {

    public WearNotifications(NotificationHelper notificationHelper, NotificationActionCreator actionCreator,
            NotificationResourceProvider resourceProvider) {
        super(notificationHelper, actionCreator, resourceProvider);
    }

    public Notification buildStackedNotification(Account account, NotificationHolder holder) {
        int notificationId = holder.notificationId;
        NotificationContent content = holder.content;
        NotificationCompat.Builder builder = createBigTextStyleNotification(account, holder, notificationId);

        PendingIntent deletePendingIntent = actionCreator.createDismissMessagePendingIntent(
                context, content.messageReference, holder.notificationId);
        builder.setDeleteIntent(deletePendingIntent);

        addActions(builder, account, holder);

        return builder.build();
    }


    public void addSummaryActions(Builder builder, NotificationData notificationData) {
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();

        addMarkAllAsReadAction(wearableExtender, notificationData);

        if (isDeleteActionAvailableForWear()) {
            addDeleteAllAction(wearableExtender, notificationData);
        }

        Account account = notificationData.getAccount();
        if (isArchiveActionAvailable(account)) {
            addArchiveAllAction(wearableExtender, notificationData);
        }

        builder.extend(wearableExtender);
    }

    private void addMarkAllAsReadAction(WearableExtender wearableExtender, NotificationData notificationData) {
        int icon = resourceProvider.getWearIconMarkAsRead();
        String title = resourceProvider.actionMarkAllAsRead();

        Account account = notificationData.getAccount();
        ArrayList<MessageReference> messageReferences = notificationData.getAllMessageReferences();
        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);
        PendingIntent action = actionCreator.getMarkAllAsReadPendingIntent(account, messageReferences, notificationId);

        NotificationCompat.Action markAsReadAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(markAsReadAction);
    }

    private void addDeleteAllAction(WearableExtender wearableExtender, NotificationData notificationData) {
        int icon = resourceProvider.getWearIconDelete();
        String title = resourceProvider.actionDeleteAll();

        Account account = notificationData.getAccount();
        ArrayList<MessageReference> messageReferences = notificationData.getAllMessageReferences();
        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);
        PendingIntent action = actionCreator.getDeleteAllPendingIntent(account, messageReferences, notificationId);

        NotificationCompat.Action deleteAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(deleteAction);
    }

    private void addArchiveAllAction(WearableExtender wearableExtender, NotificationData notificationData) {
        int icon = resourceProvider.getWearIconArchive();
        String title = resourceProvider.actionArchiveAll();

        Account account = notificationData.getAccount();
        ArrayList<MessageReference> messageReferences = notificationData.getAllMessageReferences();
        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);
        PendingIntent action = actionCreator.createArchiveAllPendingIntent(account, messageReferences, notificationId);

        NotificationCompat.Action archiveAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(archiveAction);
    }

    private void addActions(Builder builder, Account account, NotificationHolder holder) {
        addDeviceActions(builder, account, holder);
        addWearActions(builder, account, holder);
    }

    private void addDeviceActions(Builder builder, Account account, NotificationHolder holder) {
        addDeviceReplyAction(builder, holder);
        addDeviceMarkAsReadAction(builder, holder);
        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);
        addQuickMoveAction(account, builder, holder.content, notificationId);
    }

    private void addDeviceReplyAction(Builder builder, NotificationHolder holder) {
        int icon = resourceProvider.getIconReply();
        String title = resourceProvider.actionReply();

        NotificationContent content = holder.content;
        MessageReference messageReference = content.messageReference;
        PendingIntent replyToMessagePendingIntent =
                actionCreator.createReplyPendingIntent(messageReference, holder.notificationId);

        builder.addAction(icon, title, replyToMessagePendingIntent);
    }

    private void addDeviceMarkAsReadAction(Builder builder, NotificationHolder holder) {
        int icon = resourceProvider.getIconMarkAsRead();
        String title = resourceProvider.actionMarkAsRead();

        NotificationContent content = holder.content;
        int notificationId = holder.notificationId;
        MessageReference messageReference = content.messageReference;
        PendingIntent action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference, notificationId);

        builder.addAction(icon, title, action);
    }

    private void addWearActions(Builder builder, Account account, NotificationHolder holder) {
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();

        addReplyAction(wearableExtender, holder);
        addMarkAsReadAction(wearableExtender, holder);

        if (isDeleteActionAvailableForWear()) {
            addDeleteAction(wearableExtender, holder);
        }

        if (isArchiveActionAvailable(account)) {
            addArchiveAction(wearableExtender, holder);
        }

        if (isSpamActionAvailableForWear(account)) {
            addMarkAsSpamAction(wearableExtender, holder);
        }

        builder.extend(wearableExtender);
    }

    private void addReplyAction(WearableExtender wearableExtender, NotificationHolder holder) {
        int icon = resourceProvider.getWearIconReplyAll();
        String title = resourceProvider.actionReply();

        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createReplyPendingIntent(messageReference, notificationId);

        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(replyAction);
    }

    private void addMarkAsReadAction(WearableExtender wearableExtender, NotificationHolder holder) {
        int icon = resourceProvider.getWearIconMarkAsRead();
        String title = resourceProvider.actionMarkAsRead();

        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference, notificationId);

        NotificationCompat.Action markAsReadAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(markAsReadAction);
    }

    private void addDeleteAction(WearableExtender wearableExtender, NotificationHolder holder) {
        int icon = resourceProvider.getWearIconDelete();
        String title = resourceProvider.actionDelete();

        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createDeleteMessagePendingIntent(messageReference, notificationId);

        NotificationCompat.Action deleteAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(deleteAction);
    }

    private void addArchiveAction(WearableExtender wearableExtender, NotificationHolder holder) {
        int icon = resourceProvider.getWearIconArchive();
        String title = resourceProvider.actionArchive();

        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createArchiveMessagePendingIntent(messageReference, notificationId);

        NotificationCompat.Action archiveAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(archiveAction);
    }

    private void addMarkAsSpamAction(WearableExtender wearableExtender, NotificationHolder holder) {
        int icon = resourceProvider.getWearIconMarkAsSpam();
        String title = resourceProvider.actionMarkAsSpam();

        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createMarkMessageAsSpamPendingIntent(messageReference, notificationId);

        NotificationCompat.Action spamAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(spamAction);
    }

    private boolean isDeleteActionAvailableForWear() {
        return isQuickMoveEnabled() && NotificationQuickMoveType.DELETE == K9.getNotificationQuickMoveType()
                && !K9.confirmDeleteFromNotification();
    }

    private boolean isSpamActionAvailableForWear(Account account) {
        String spamFolderName = account.getSpamFolder();
        return spamFolderName != null && !K9.confirmSpam() && isMovePossible(account, spamFolderName);
    }
}
