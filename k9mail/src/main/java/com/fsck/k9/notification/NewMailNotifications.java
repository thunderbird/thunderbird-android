package com.fsck.k9.notification;


import android.app.Notification;
import android.support.v4.app.NotificationManagerCompat;
import android.util.SparseArray;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationHideSubject;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mailstore.LocalMessage;


class NewMailNotifications {
    private final NotificationController controller;
    private final NotificationContentCreator contentCreator;
    private final DeviceNotifications deviceNotifications;
    private final WearNotifications wearNotifications;
    private final SparseArray<NotificationsHolder> notifications = new SparseArray<NotificationsHolder>();
    private final Object lock = new Object();


    NewMailNotifications(NotificationController controller, NotificationContentCreator contentCreator,
            DeviceNotifications deviceNotifications, WearNotifications wearNotifications) {
        this.controller = controller;
        this.deviceNotifications = deviceNotifications;
        this.wearNotifications = wearNotifications;
        this.contentCreator = contentCreator;
    }

    public static NewMailNotifications newInstance(NotificationController controller,
            NotificationActionCreator actionCreator) {
        NotificationContentCreator contentCreator = new NotificationContentCreator(controller.getContext());
        WearNotifications wearNotifications = new WearNotifications(controller, actionCreator);
        DeviceNotifications deviceNotifications = DeviceNotifications.newInstance(
                controller, actionCreator, wearNotifications);
        return new NewMailNotifications(controller, contentCreator, deviceNotifications, wearNotifications);
    }

    public void addNewMailNotification(Account account, LocalMessage message, int unreadMessageCount) {
        NotificationContent content = contentCreator.createFromMessage(account, message);

        synchronized (lock) {
            NotificationsHolder notificationsHolder = getOrCreateNotificationsHolder(account, unreadMessageCount);
            AddNotificationResult result = notificationsHolder.addNotificationContent(content);

            if (result.shouldCancelNotification()) {
                int notificationId = result.getNotificationId();
                cancelNotification(notificationId);
            }

            createStackedNotification(account, result.getNotificationHolder());
            createSummaryNotification(account, notificationsHolder, false);
        }
    }

    public void removeNewMailNotification(Account account, MessageReference messageReference) {
        synchronized (lock) {
            NotificationsHolder notificationsHolder = getNotificationsHolder(account);
            if (notificationsHolder == null) {
                return;
            }

            RemoveNotificationResult result = notificationsHolder.removeNotificationForMessage(messageReference);
            if (result.isUnknownNotification()) {
                return;
            }

            cancelNotification(result.getNotificationId());

            if (result.shouldCreateNotification()) {
                createStackedNotification(account, result.getNotificationHolder());
            }

            updateSummaryNotification(account, notificationsHolder);
        }
    }

    public void clearNewMailNotifications(Account account) {
        NotificationsHolder notificationsHolder;
        synchronized (lock) {
            notificationsHolder = removeNotificationsHolder(account);
        }

        if (notificationsHolder == null) {
            return;
        }

        for (int notificationId : notificationsHolder.getActiveNotificationIds()) {
            cancelNotification(notificationId);
        }

        int notificationId = NotificationIds.getNewMailNotificationId(account);
        cancelNotification(notificationId);
    }

    private NotificationsHolder getOrCreateNotificationsHolder(Account account, int unreadMessageCount) {
        NotificationsHolder notificationsHolder = getNotificationsHolder(account);
        if (notificationsHolder != null) {
            return notificationsHolder;
        }

        int accountNumber = account.getAccountNumber();
        NotificationsHolder newNotificationHolder = createNotificationsHolder(account, unreadMessageCount);
        notifications.put(accountNumber, newNotificationHolder);

        return newNotificationHolder;
    }

    private NotificationsHolder getNotificationsHolder(Account account) {
        int accountNumber = account.getAccountNumber();
        return notifications.get(accountNumber);
    }

    private NotificationsHolder removeNotificationsHolder(Account account) {
        int accountNumber = account.getAccountNumber();
        NotificationsHolder notificationsHolder = notifications.get(accountNumber);
        notifications.remove(accountNumber);
        return notificationsHolder;
    }

    NotificationsHolder createNotificationsHolder(Account account, int unreadMessageCount) {
        NotificationsHolder notificationsHolder = new NotificationsHolder(account);
        notificationsHolder.setUnreadMessageCount(unreadMessageCount);
        return notificationsHolder;
    }

    private void cancelNotification(int notificationId) {
        getNotificationManager().cancel(notificationId);
    }

    private void updateSummaryNotification(Account account, NotificationsHolder notificationsHolder) {
        if (notificationsHolder.getNewMessagesCount() == 0) {
            clearNewMailNotifications(account);
        } else {
            createSummaryNotification(account, notificationsHolder, true);
        }
    }

    private void createSummaryNotification(Account account, NotificationsHolder notificationsHolder, boolean silent) {
        Notification notification = deviceNotifications.buildSummaryNotification(account, notificationsHolder, silent);
        int notificationId = NotificationIds.getNewMailNotificationId(account);

        getNotificationManager().notify(notificationId, notification);
    }

    private void createStackedNotification(Account account, NotificationHolder holder) {
        if (isPrivacyModeEnabled()) {
            return;
        }

        Notification notification = wearNotifications.buildStackedNotification(account, holder);
        int notificationId = holder.notificationId;

        getNotificationManager().notify(notificationId, notification);
    }

    private boolean isPrivacyModeEnabled() {
        return K9.getNotificationHideSubject() != NotificationHideSubject.NEVER;
    }

    private NotificationManagerCompat getNotificationManager() {
        return controller.getNotificationManager();
    }
}
