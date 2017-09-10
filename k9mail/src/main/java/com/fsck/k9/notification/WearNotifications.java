package com.fsck.k9.notification;


import java.util.ArrayList;

import android.app.Notification;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationCompat.WearableExtender;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;


class WearNotifications extends BaseNotifications {

    public WearNotifications(NotificationController controller, NotificationActionCreator actionCreator) {
        super(controller, actionCreator);
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
        if (isArchiveActionAvailableForWear(account)) {
            addArchiveAllAction(wearableExtender, notificationData);
        }

        builder.extend(wearableExtender);
    }

    private void addMarkAllAsReadAction(WearableExtender wearableExtender, NotificationData notificationData) {
        int icon = R.drawable.ic_action_mark_as_read_dark;
        String title = context.getString(R.string.notification_action_mark_all_as_read);

        Account account = notificationData.getAccount();
        ArrayList<MessageReference> messageReferences = notificationData.getAllMessageReferences();
        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);
        PendingIntent action = actionCreator.getMarkAllAsReadPendingIntent(account, messageReferences, notificationId);

        NotificationCompat.Action markAsReadAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(markAsReadAction);
    }

    private void addDeleteAllAction(WearableExtender wearableExtender, NotificationData notificationData) {
        int icon = R.drawable.ic_action_delete_dark;
        String title = context.getString(R.string.notification_action_delete_all);

        Account account = notificationData.getAccount();
        ArrayList<MessageReference> messageReferences = notificationData.getAllMessageReferences();
        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);
        PendingIntent action = actionCreator.getDeleteAllPendingIntent(account, messageReferences, notificationId);

        NotificationCompat.Action deleteAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(deleteAction);
    }

    private void addArchiveAllAction(WearableExtender wearableExtender, NotificationData notificationData) {
        int icon = R.drawable.ic_action_archive_dark;
        String title = context.getString(R.string.notification_action_archive_all);

        Account account = notificationData.getAccount();
        ArrayList<MessageReference> messageReferences = notificationData.getAllMessageReferences();
        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);
        PendingIntent action = actionCreator.createArchiveAllPendingIntent(account, messageReferences, notificationId);

        NotificationCompat.Action archiveAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(archiveAction);
    }

    private void addActions(Builder builder, Account account, NotificationHolder holder) {
        addDeviceActions(builder, holder);
        addWearActions(builder, account, holder);
    }

    private void addDeviceActions(Builder builder, NotificationHolder holder) {
        addDeviceReplyAction(builder, holder);
        addDeviceMarkAsReadAction(builder, holder);
        addDeviceDeleteAction(builder, holder);
    }

    private void addDeviceReplyAction(Builder builder, NotificationHolder holder) {
        int icon = R.drawable.notification_action_mark_as_read;
        String title = context.getString(R.string.notification_action_reply);

        NotificationContent content = holder.content;
        MessageReference messageReference = content.messageReference;
        PendingIntent replyToMessagePendingIntent =
                actionCreator.createReplyPendingIntent(messageReference, holder.notificationId);

        builder.addAction(icon, title, replyToMessagePendingIntent);
    }

    private void addDeviceMarkAsReadAction(Builder builder, NotificationHolder holder) {
        int icon = R.drawable.notification_action_mark_as_read;
        String title = context.getString(R.string.notification_action_mark_as_read);

        NotificationContent content = holder.content;
        int notificationId = holder.notificationId;
        MessageReference messageReference = content.messageReference;
        PendingIntent action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference, notificationId);

        builder.addAction(icon, title, action);
    }

    private void addDeviceDeleteAction(Builder builder, NotificationHolder holder) {
        if (!isDeleteActionEnabled()) {
            return;
        }

        int icon = R.drawable.notification_action_delete;
        String title = context.getString(R.string.notification_action_delete);

        NotificationContent content = holder.content;
        int notificationId = holder.notificationId;
        MessageReference messageReference = content.messageReference;
        PendingIntent action = actionCreator.createDeleteMessagePendingIntent(messageReference, notificationId);

        builder.addAction(icon, title, action);
    }

    private void addWearActions(Builder builder, Account account, NotificationHolder holder) {
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();

        addReplyAction(wearableExtender, holder);
        addMarkAsReadAction(wearableExtender, holder);

        if (isDeleteActionAvailableForWear()) {
            addDeleteAction(wearableExtender, holder);
        }

        if (isArchiveActionAvailableForWear(account)) {
            addArchiveAction(wearableExtender, holder);
        }

        if (isSpamActionAvailableForWear(account)) {
            addMarkAsSpamAction(wearableExtender, holder);
        }

        builder.extend(wearableExtender);
    }

    private void addReplyAction(WearableExtender wearableExtender, NotificationHolder holder) {
        int icon = R.drawable.ic_action_single_message_options_dark;
        String title = context.getString(R.string.notification_action_reply);

        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createReplyPendingIntent(messageReference, notificationId);

        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(replyAction);
    }

    private void addMarkAsReadAction(WearableExtender wearableExtender, NotificationHolder holder) {
        int icon = R.drawable.ic_action_mark_as_read_dark;
        String title = context.getString(R.string.notification_action_mark_as_read);

        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference, notificationId);

        NotificationCompat.Action markAsReadAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(markAsReadAction);
    }

    private void addDeleteAction(WearableExtender wearableExtender, NotificationHolder holder) {
        int icon = R.drawable.ic_action_delete_dark;
        String title = context.getString(R.string.notification_action_delete);

        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createDeleteMessagePendingIntent(messageReference, notificationId);

        NotificationCompat.Action deleteAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(deleteAction);
    }

    private void addArchiveAction(WearableExtender wearableExtender, NotificationHolder holder) {
        int icon = R.drawable.ic_action_archive_dark;
        String title = context.getString(R.string.notification_action_archive);

        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createArchiveMessagePendingIntent(messageReference, notificationId);

        NotificationCompat.Action archiveAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(archiveAction);
    }

    private void addMarkAsSpamAction(WearableExtender wearableExtender, NotificationHolder holder) {
        int icon = R.drawable.ic_action_spam_dark;
        String title = context.getString(R.string.notification_action_spam);

        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createMarkMessageAsSpamPendingIntent(messageReference, notificationId);

        NotificationCompat.Action spamAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        wearableExtender.addAction(spamAction);
    }

    private boolean isDeleteActionAvailableForWear() {
        return isDeleteActionEnabled() && !K9.confirmDeleteFromNotification();
    }

    private boolean isArchiveActionAvailableForWear(Account account) {
        String archiveFolderName = account.getArchiveFolderId();
        return archiveFolderName != null && isMovePossible(account, archiveFolderName);
    }

    private boolean isSpamActionAvailableForWear(Account account) {
        String spamFolderName = account.getSpamFolderId();
        return spamFolderName != null && !K9.confirmSpam() && isMovePossible(account, spamFolderName);
    }

    private boolean isMovePossible(Account account, String destinationFolderName) {
        if (K9.FOLDER_NONE.equalsIgnoreCase(destinationFolderName)) {
            return false;
        }

        MessagingController controller = createMessagingController();
        return controller.isMoveCapable(account);
    }

    MessagingController createMessagingController() {
        return MessagingController.getInstance(context);
    }
}
