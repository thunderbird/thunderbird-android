package com.fsck.k9.notification;


import com.fsck.k9.Account;


class NotificationIds {
    private static final int OFFSET_SEND_FAILED_NOTIFICATION = 0;
    private static final int OFFSET_CERTIFICATE_ERROR_INCOMING = 1;
    private static final int OFFSET_CERTIFICATE_ERROR_OUTGOING = 2;
    private static final int OFFSET_AUTHENTICATION_ERROR_INCOMING = 3;
    private static final int OFFSET_AUTHENTICATION_ERROR_OUTGOING = 4;
    private static final int OFFSET_FETCHING_MAIL = 5;
    private static final int OFFSET_NEW_MAIL_SUMMARY = 6;

    private static final int OFFSET_NEW_MAIL_STACKED = 7;

    private static final int NUMBER_OF_DEVICE_NOTIFICATIONS = 7;
    private static final int NUMBER_OF_STACKED_NOTIFICATIONS = NotificationData.MAX_NUMBER_OF_STACKED_NOTIFICATIONS;
    private static final int NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT = NUMBER_OF_DEVICE_NOTIFICATIONS +
            NUMBER_OF_STACKED_NOTIFICATIONS;
            

    public static int getNewMailSummaryNotificationId(Account account) {
        return getBaseNotificationId(account) + OFFSET_NEW_MAIL_SUMMARY;
    }

    public static int getNewMailStackedNotificationId(Account account, int index) {
        if (index < 0 || index >= NUMBER_OF_STACKED_NOTIFICATIONS) {
            throw new IndexOutOfBoundsException("Invalid value: " + index);
        }

        return getBaseNotificationId(account) + OFFSET_NEW_MAIL_STACKED + index;
    }

    public static int getFetchingMailNotificationId(Account account) {
        return getBaseNotificationId(account) + OFFSET_FETCHING_MAIL;
    }

    public static int getSendFailedNotificationId(Account account) {
        return getBaseNotificationId(account) + OFFSET_SEND_FAILED_NOTIFICATION;
    }

    public static int getCertificateErrorNotificationId(Account account, boolean incoming) {
        int offset = incoming ? OFFSET_CERTIFICATE_ERROR_INCOMING : OFFSET_CERTIFICATE_ERROR_OUTGOING;
        return getBaseNotificationId(account) + offset;
    }

    public static int getAuthenticationErrorNotificationId(Account account, boolean incoming) {
        int offset = incoming ? OFFSET_AUTHENTICATION_ERROR_INCOMING : OFFSET_AUTHENTICATION_ERROR_OUTGOING;
        return getBaseNotificationId(account) + offset;
    }

    private static int getBaseNotificationId(Account account) {
        return account.getAccountNumber() * NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT;
    }
}
