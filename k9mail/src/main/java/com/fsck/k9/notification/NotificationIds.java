package com.fsck.k9.notification;


import com.fsck.k9.Account;


class NotificationIds {
    private static final int MAX_NUMBER_OF_ACCOUNTS = 1000;
    private static final int DEFAULT_NOTIFICATION_OFFSET = 0;

    private static final int BASE_ID_FETCHING_MAIL = -5000;
    private static final int BASE_ID_SEND_FAILED_NOTIFICATION = -1500;
    private static final int BASE_ID_CERTIFICATE_ERROR_INCOMING = -2000;
    private static final int BASE_ID_CERTIFICATE_ERROR_OUTGOING = -2500;


    public static int getNewMailNotificationId(Account account) {
        return getNewMailNotificationId(account, DEFAULT_NOTIFICATION_OFFSET);
    }

    public static int getNewMailNotificationId(Account account, int offset) {
        return account.getAccountNumber() + MAX_NUMBER_OF_ACCOUNTS * offset;
    }

    public static int getFetchingMailNotificationId(Account account) {
        return getNotificationId(account, BASE_ID_FETCHING_MAIL);
    }

    public static int getSendFailedNotificationId(Account account) {
        return getNotificationId(account, BASE_ID_SEND_FAILED_NOTIFICATION);
    }

    public static int getCertificateErrorNotificationId(Account account, boolean incoming) {
        int baseNotificationId = incoming ? BASE_ID_CERTIFICATE_ERROR_INCOMING : BASE_ID_CERTIFICATE_ERROR_OUTGOING;
        return getNotificationId(account, baseNotificationId);
    }

    private static int getNotificationId(Account account, int baseNotificationId) {
        return baseNotificationId + account.getAccountNumber();
    }
}
