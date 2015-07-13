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


    public void addSummaryActions(Builder builder, NotificationsHolder notificationsHolder) {
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();

        addMarkAllAsReadAction(builder, wearableExtender, notificationsHolder);

        if (isDeleteActionAvailableForWear()) {
            addDeleteAllAction(builder, wearableExtender, notificationsHolder);
        }

        Account account = notificationsHolder.getAccount();
        if (isArchiveActionAvailableForWear(account)) {
            addArchiveAllAction(builder, wearableExtender, notificationsHolder);
        }
    }

    private void addMarkAllAsReadAction(Builder builder, WearableExtender wearableExtender,
            NotificationsHolder notificationsHolder) {
        int icon = R.drawable.ic_action_mark_as_read_dark;
        String title = context.getString(R.string.notification_action_mark_all_as_read);

        Account account = notificationsHolder.getAccount();
        ArrayList<MessageReference> messageReferences = notificationsHolder.getAllMessageReferences();
        int notificationId = NotificationIds.getNewMailNotificationId(account);
        PendingIntent action = actionCreator.createMarkAllAsReadPendingIntent(
                account, messageReferences, notificationId);

        NotificationCompat.Action markAsReadAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        builder.extend(wearableExtender.addAction(markAsReadAction));
    }

    private void addDeleteAllAction(Builder builder, WearableExtender wearableExtender,
            NotificationsHolder notificationsHolder) {
        int icon = R.drawable.ic_action_delete_dark;
        String title = context.getString(R.string.notification_action_delete_all);

        Account account = notificationsHolder.getAccount();
        ArrayList<MessageReference> messageReferences = notificationsHolder.getAllMessageReferences();
        int notificationId = NotificationIds.getNewMailNotificationId(account);
        PendingIntent action = actionCreator.createDeleteAllPendingIntent(account, messageReferences, notificationId);

        NotificationCompat.Action deleteAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        builder.extend(wearableExtender.addAction(deleteAction));
    }

    private void addArchiveAllAction(Builder builder, WearableExtender wearableExtender,
            NotificationsHolder notificationsHolder) {
        int icon = R.drawable.ic_action_archive_dark;
        String title = context.getString(R.string.notification_action_archive_all);

        Account account = notificationsHolder.getAccount();
        ArrayList<MessageReference> messageReferences = notificationsHolder.getAllMessageReferences();
        int notificationId = NotificationIds.getNewMailNotificationId(account);
        PendingIntent action = actionCreator.createArchiveAllPendingIntent(account, messageReferences, notificationId);

        NotificationCompat.Action archiveAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        builder.extend(wearableExtender.addAction(archiveAction));
    }

    private void addActions(Builder builder, Account account, NotificationHolder holder) {
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();

        addReplyAction(builder, wearableExtender, holder);
        addMarkAsReadAction(builder, wearableExtender, holder);

        if (isDeleteActionAvailableForWear()) {
            addDeleteAction(builder, wearableExtender, holder);
        }

        if (isArchiveActionAvailableForWear(account)) {
            addArchiveAction(builder, wearableExtender, holder);
        }

        if (isSpamActionAvailableForWear(account)) {
            addMarkAsSpamAction(builder, wearableExtender, holder);
        }
    }

    private void addReplyAction(Builder builder, WearableExtender wearableExtender, NotificationHolder holder) {
        int icon = R.drawable.ic_action_single_message_options_dark;
        String title = context.getString(R.string.notification_action_reply);

        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createReplyPendingIntent(messageReference, notificationId);

        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        builder.extend(wearableExtender.addAction(replyAction));
    }

    private void addMarkAsReadAction(Builder builder, WearableExtender wearableExtender,
            NotificationHolder holder) {
        int icon = R.drawable.ic_action_mark_as_read_dark;
        String title = context.getString(R.string.notification_action_mark_as_read);


        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference, notificationId);

        NotificationCompat.Action markAsReadAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        builder.extend(wearableExtender.addAction(markAsReadAction));
    }

    private void addDeleteAction(Builder builder, WearableExtender wearableExtender, NotificationHolder holder) {
        int icon = R.drawable.ic_action_delete_dark;
        String title = context.getString(R.string.notification_action_delete);

        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createDeleteMessagePendingIntent(messageReference, notificationId);

        NotificationCompat.Action deleteAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        builder.extend(wearableExtender.addAction(deleteAction));
    }

    private void addArchiveAction(Builder builder, WearableExtender wearableExtender, NotificationHolder holder) {
        int icon = R.drawable.ic_action_archive_dark;
        String title = context.getString(R.string.notification_action_archive);

        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createArchiveMessagePendingIntent(messageReference, notificationId);

        NotificationCompat.Action archiveAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        builder.extend(wearableExtender.addAction(archiveAction));
    }

    private void addMarkAsSpamAction(Builder builder, WearableExtender wearableExtender,
            NotificationHolder holder) {
        int icon = R.drawable.ic_action_spam_dark;
        String title = context.getString(R.string.notification_action_spam);

        MessageReference messageReference = holder.content.messageReference;
        int notificationId = holder.notificationId;
        PendingIntent action = actionCreator.createMarkMessageAsSpamPendingIntent(messageReference, notificationId);

        NotificationCompat.Action spamAction = new NotificationCompat.Action.Builder(icon, title, action).build();
        builder.extend(wearableExtender.addAction(spamAction));
    }

    private boolean isDeleteActionAvailableForWear() {
        return isDeleteActionEnabled() && !K9.confirmDeleteFromNotification();
    }

    private boolean isArchiveActionAvailableForWear(Account account) {
        String archiveFolderName = account.getArchiveFolderName();
        return archiveFolderName != null && isMovePossible(account, archiveFolderName);
    }

    private boolean isSpamActionAvailableForWear(Account account) {
        String spamFolderName = account.getSpamFolderName();
        return spamFolderName != null && !K9.confirmSpam() && isMovePossible(account, spamFolderName);
    }

    private boolean isMovePossible(Account account, String destinationFolderName) {
        if (K9.FOLDER_NONE.equalsIgnoreCase(destinationFolderName)) {
            return false;
        }

        MessagingController controller = MessagingController.getInstance(context);
        return controller.isMoveCapable(account);
    }
}
