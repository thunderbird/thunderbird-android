package com.fsck.k9.notification;


class AddNotificationResult {
    private final NotificationHolder notificationHolder;
    private final boolean cancelNotificationBeforeReuse;


    private AddNotificationResult(NotificationHolder notificationHolder,
            boolean cancelNotificationBeforeReuse) {
        this.notificationHolder = notificationHolder;
        this.cancelNotificationBeforeReuse = cancelNotificationBeforeReuse;
    }

    public static AddNotificationResult newNotification(NotificationHolder notificationHolder) {
        return new AddNotificationResult(notificationHolder, false);
    }

    public static AddNotificationResult replaceNotification(NotificationHolder notificationHolder) {
        return new AddNotificationResult(notificationHolder, true);
    }

    public boolean shouldCancelNotification() {
        return cancelNotificationBeforeReuse;
    }

    public int getNotificationId() {
        if (!shouldCancelNotification()) {
            throw new IllegalStateException("getNotificationId() can only be called when " +
                    "shouldCancelNotification() returns true");
        }

        return notificationHolder.notificationId;
    }

    public NotificationHolder getNotificationHolder() {
        return notificationHolder;
    }
}
