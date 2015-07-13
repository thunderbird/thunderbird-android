package com.fsck.k9.notification;


import java.util.ArrayList;
import java.util.List;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationHideSubject;
import com.fsck.k9.NotificationSetting;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;

import static com.fsck.k9.notification.NotificationController.NOTIFICATION_LED_BLINK_SLOW;
import static com.fsck.k9.notification.NotificationController.platformSupportsExtendedNotifications;


class DeviceNotifications extends BaseNotifications {
    private final WearNotifications wearNotifications;
    private final LockScreenNotification lockScreenNotification;


    DeviceNotifications(NotificationController controller, NotificationActionCreator actionCreator,
            LockScreenNotification lockScreenNotification, WearNotifications wearNotifications) {
        super(controller, actionCreator);
        this.wearNotifications = wearNotifications;
        this.lockScreenNotification = lockScreenNotification;
    }

    public static DeviceNotifications newInstance(NotificationController controller,
            NotificationActionCreator actionCreator, WearNotifications wearNotifications) {
        LockScreenNotification lockScreenNotification = LockScreenNotification.newInstance(controller);
        return new DeviceNotifications(controller, actionCreator, lockScreenNotification, wearNotifications);
    }

    public Notification buildSummaryNotification(Account account, NotificationsHolder notificationsHolder,
            boolean silent) {
        int unreadMessageCount = notificationsHolder.getUnreadMessageCount();

        NotificationCompat.Builder builder;
        if (isPrivacyModeActive() || !platformSupportsExtendedNotifications()) {
            builder = createSimpleSummaryNotification(account, unreadMessageCount);
        } else if (notificationsHolder.isSingleMessageNotification()) {
            NotificationHolder holder = notificationsHolder.getHolderForLatestNotification();
            builder = createBigTextStyleSummaryNotification(account, holder);
        } else {
            builder = createInboxStyleSummaryNotification(account, notificationsHolder, unreadMessageCount);
        }

        if (notificationsHolder.containsStarredMessages()) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        int notificationId = NotificationIds.getNewMailNotificationId(account);
        PendingIntent deletePendingIntent = actionCreator.createDismissAllMessagesPendingIntent(
                account, notificationId);
        builder.setDeleteIntent(deletePendingIntent);

        lockScreenNotification.configureLockScreenNotification(builder, notificationsHolder);

        boolean ringAndVibrate = false;
        if (!silent && !account.isRingNotified()) {
            account.setRingNotified(true);
            ringAndVibrate = true;
        }

        NotificationSetting notificationSetting = account.getNotificationSetting();
        controller.configureNotification(
                builder,
                (notificationSetting.shouldRing()) ? notificationSetting.getRingtone() : null,
                (notificationSetting.shouldVibrate()) ? notificationSetting.getVibration() : null,
                (notificationSetting.isLed()) ? notificationSetting.getLedColor() : null,
                NOTIFICATION_LED_BLINK_SLOW,
                ringAndVibrate);

        return builder.build();
    }

    private NotificationCompat.Builder createSimpleSummaryNotification(Account account, int unreadMessageCount) {
        String accountName = controller.getAccountName(account);
        CharSequence newMailText = context.getString(R.string.notification_new_title);
        String unreadMessageCountText = context.getString(R.string.notification_new_one_account_fmt,
                unreadMessageCount, accountName);

        int notificationId = NotificationIds.getNewMailNotificationId(account);
        PendingIntent contentIntent = actionCreator.createViewFolderListPendingIntent(account, notificationId);

        return createAndInitializeNotificationBuilder(account)
                .setNumber(unreadMessageCount)
                .setTicker(newMailText)
                .setContentTitle(unreadMessageCountText)
                .setContentText(newMailText)
                .setContentIntent(contentIntent);
    }

    private NotificationCompat.Builder createBigTextStyleSummaryNotification(Account account,
            NotificationHolder holder) {

        int notificationId = NotificationIds.getNewMailNotificationId(account);
        Builder builder = createBigTextStyleNotification(account, holder, notificationId)
                .setGroupSummary(true);

        NotificationContent content = holder.content;
        addReplyAction(builder, content, notificationId);
        addMarkAsReadAction(builder, content, notificationId);
        addDeleteAction(builder, content, notificationId);

        return builder;
    }

    private NotificationCompat.Builder createInboxStyleSummaryNotification(Account account,
            NotificationsHolder notificationsHolder, int unreadMessageCount) {

        NotificationHolder latestNotification = notificationsHolder.getHolderForLatestNotification();

        int newMessagesCount = notificationsHolder.getNewMessagesCount();
        String accountName = controller.getAccountName(account);
        String title = context.getResources().getQuantityString(R.plurals.notification_new_messages_title,
                newMessagesCount, newMessagesCount);
        String summary = (notificationsHolder.hasAdditionalMessages()) ?
                context.getString(R.string.notification_additional_messages,
                        notificationsHolder.getAdditionalMessagesCount(), accountName) :
                accountName;

        NotificationCompat.Builder builder = createAndInitializeNotificationBuilder(account)
                .setNumber(unreadMessageCount)
                .setTicker(latestNotification.content.summary)
                .setGroup(NOTIFICATION_GROUP_KEY)
                .setGroupSummary(true)
                .setContentTitle(title)
                .setSubText(accountName);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle(builder)
                .setBigContentTitle(title)
                .setSummaryText(summary);

        for (NotificationContent content : notificationsHolder.getContentForSummaryNotification()) {
            style.addLine(content.summary);
        }

        builder.setStyle(style);

        addMarkAllAsReadAction(builder, notificationsHolder);
        addDeleteAllAction(builder, notificationsHolder);

        wearNotifications.addSummaryActions(builder, notificationsHolder);

        int notificationId = NotificationIds.getNewMailNotificationId(account);
        List<MessageReference> messageReferences = notificationsHolder.getAllMessageReferences();
        PendingIntent contentIntent = actionCreator.createViewMessagesPendingIntent(
                account, messageReferences, notificationId);
        builder.setContentIntent(contentIntent);

        return builder;
    }

    private void addMarkAsReadAction(Builder builder, NotificationContent content, int notificationId) {
        int icon = getMarkAsReadActionIcon();
        String title = context.getString(R.string.notification_action_mark_as_read);


        MessageReference messageReference = content.messageReference;
        PendingIntent action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference, notificationId);

        builder.addAction(icon, title, action);
    }

    private void addMarkAllAsReadAction(Builder builder, NotificationsHolder notificationsHolder) {
        int icon = getMarkAsReadActionIcon();
        String title = context.getString(R.string.notification_action_mark_as_read);

        Account account = notificationsHolder.getAccount();
        ArrayList<MessageReference> messageReferences = notificationsHolder.getAllMessageReferences();
        int notificationId = NotificationIds.getNewMailNotificationId(account);
        PendingIntent markAllAsReadPendingIntent =
                actionCreator.createMarkAllAsReadPendingIntent(account, messageReferences, notificationId);

        builder.addAction(icon, title, markAllAsReadPendingIntent);
    }

    private void addDeleteAllAction(Builder builder, NotificationsHolder notificationsHolder) {
        int icon = getDeleteActionIcon();
        String title = context.getString(R.string.notification_action_delete);

        Account account = notificationsHolder.getAccount();
        int notificationId = NotificationIds.getNewMailNotificationId(account);
        ArrayList<MessageReference> messageReferences = notificationsHolder.getAllMessageReferences();
        PendingIntent action = actionCreator.createDeleteAllPendingIntent(account, messageReferences, notificationId);

        builder.addAction(icon, title, action);
    }

    private void addDeleteAction(Builder builder, NotificationContent content, int notificationId) {
        if (!isDeleteActionEnabled()) {
            return;
        }

        int icon = getDeleteActionIcon();
        String title = context.getString(R.string.notification_action_delete);

        MessageReference messageReference = content.messageReference;
        PendingIntent action = actionCreator.createDeleteMessagePendingIntent(messageReference, notificationId);

        builder.addAction(icon, title, action);
    }

    private void addReplyAction(Builder builder, NotificationContent content, int notificationId) {
        int icon = getReplyActionIcon();
        String title = context.getString(R.string.notification_action_reply);

        MessageReference messageReference = content.messageReference;
        PendingIntent replyToMessagePendingIntent =
                actionCreator.createReplyPendingIntent(messageReference, notificationId);

        builder.addAction(icon, title, replyToMessagePendingIntent);
    }

    private boolean isPrivacyModeActive() {
        KeyguardManager keyguardService = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        boolean privacyModeAlwaysEnabled = K9.getNotificationHideSubject() == NotificationHideSubject.ALWAYS;
        boolean privacyModeEnabledWhenLocked = K9.getNotificationHideSubject() == NotificationHideSubject.WHEN_LOCKED;
        boolean screenLocked = keyguardService.inKeyguardRestrictedInputMode();

        return privacyModeAlwaysEnabled || (privacyModeEnabledWhenLocked && screenLocked);
    }

    private int getMarkAsReadActionIcon() {
        return controller.platformSupportsVectorDrawables() ?
                R.drawable.ic_action_mark_as_read_dark_vector : R.drawable.ic_action_mark_as_read_dark;
    }

    private int getDeleteActionIcon() {
        return NotificationController.platformSupportsLockScreenNotifications() ?
                R.drawable.ic_action_delete_dark_vector : R.drawable.ic_action_delete_dark;
    }

    private int getReplyActionIcon() {
        return controller.platformSupportsVectorDrawables() ?
                R.drawable.ic_action_single_message_options_dark_vector :
                R.drawable.ic_action_single_message_options_dark;
    }
}
