package com.fsck.k9.notification;


class RemoveNotificationResult {
    private final NotificationHolder notificationHolder;
    private final int notificationId;
    private final boolean unknownNotification;


    private RemoveNotificationResult(NotificationHolder notificationHolder, int notificationId,
            boolean unknownNotification) {
        this.notificationHolder = notificationHolder;
        this.notificationId = notificationId;
        this.unknownNotification = unknownNotification;
    }

    public static RemoveNotificationResult createNotification(NotificationHolder notificationHolder) {
        return new RemoveNotificationResult(notificationHolder, notificationHolder.notificationId, false);
    }

    public static RemoveNotificationResult cancelNotification(int notificationId) {
        return new RemoveNotificationResult(null, notificationId, false);
    }

    public static RemoveNotificationResult unknownNotification() {
        return new RemoveNotificationResult(null, 0, true);
    }

    public boolean shouldCreateNotification() {
        return notificationHolder != null;
    }

    public int getNotificationId() {
        if (isUnknownNotification()) {
            throw new IllegalStateException("getNotificationId() can only be called when " +
                    "isUnknownNotification() returns false");
        }

        return notificationId;
    }

    public boolean isUnknownNotification() {
        return unknownNotification;
    }

    public NotificationHolder getNotificationHolder() {
        if (!shouldCreateNotification()) {
            throw new IllegalStateException("getNotificationHolder() can only be called when " +
                    "shouldCreateNotification() returns true");
        }

        return notificationHolder;
    }
}
