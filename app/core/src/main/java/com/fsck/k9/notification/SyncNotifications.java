package com.fsck.k9.notification;


import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.fsck.k9.Account;
import com.fsck.k9.mailstore.LocalFolder;

import static com.fsck.k9.notification.NotificationHelper.NOTIFICATION_LED_BLINK_FAST;


class SyncNotifications {
    private static final boolean NOTIFICATION_LED_WHILE_SYNCING = false;


    private final NotificationHelper notificationHelper;
    private final NotificationActionCreator actionBuilder;
    private final NotificationResourceProvider resourceProvider;


    public SyncNotifications(NotificationHelper notificationHelper, NotificationActionCreator actionBuilder,
                             NotificationResourceProvider resourceProvider) {
        this.notificationHelper = notificationHelper;
        this.actionBuilder = actionBuilder;
        this.resourceProvider = resourceProvider;
    }

    public void showSendingNotification(Account account) {
        String accountName = notificationHelper.getAccountName(account);
        String title = resourceProvider.sendingMailTitle();
        String tickerText = resourceProvider.sendingMailBody(accountName);

        int notificationId = NotificationIds.getFetchingMailNotificationId(account);
        String outboxFolder = account.getOutboxFolder();
        PendingIntent showMessageListPendingIntent = actionBuilder.createViewFolderPendingIntent(
                account, outboxFolder, notificationId);

        NotificationCompat.Builder builder = notificationHelper.createNotificationBuilder(account,
                NotificationChannelManager.ChannelType.MISCELLANEOUS)
                .setSmallIcon(resourceProvider.getIconSendingMail())
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .setTicker(tickerText)
                .setContentTitle(title)
                .setContentText(accountName)
                .setContentIntent(showMessageListPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (NOTIFICATION_LED_WHILE_SYNCING) {
            notificationHelper.configureNotification(builder, null, null,
                    account.getNotificationSetting().getLedColor(),
                    NOTIFICATION_LED_BLINK_FAST, true);
        }

        getNotificationManager().notify(notificationId, builder.build());
    }

    public void clearSendingNotification(Account account) {
        int notificationId = NotificationIds.getFetchingMailNotificationId(account);
        getNotificationManager().cancel(notificationId);
    }

    public void showFetchingMailNotification(Account account, LocalFolder folder) {
        String accountName = account.getDescription();
        String folderServerId = folder.getServerId();
        String folderName = folder.getName();

        String tickerText = resourceProvider.checkingMailTicker(accountName, folderName);
        String title = resourceProvider.checkingMailTitle();
        //TODO: Use format string from resources
        String text = accountName + resourceProvider.checkingMailSeparator() + folderName;

        int notificationId = NotificationIds.getFetchingMailNotificationId(account);
        PendingIntent showMessageListPendingIntent = actionBuilder.createViewFolderPendingIntent(
                account, folderServerId, notificationId);

        NotificationCompat.Builder builder = notificationHelper.createNotificationBuilder(account,
                NotificationChannelManager.ChannelType.MISCELLANEOUS)
                .setSmallIcon(resourceProvider.getIconCheckingMail())
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .setTicker(tickerText)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(showMessageListPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_SERVICE);

        if (NOTIFICATION_LED_WHILE_SYNCING) {
            notificationHelper.configureNotification(builder, null, null,
                    account.getNotificationSetting().getLedColor(),
                    NOTIFICATION_LED_BLINK_FAST, true);
        }

        getNotificationManager().notify(notificationId, builder.build());
    }

    public void clearFetchingMailNotification(Account account) {
        int notificationId = NotificationIds.getFetchingMailNotificationId(account);
        getNotificationManager().cancel(notificationId);
    }

    private NotificationManagerCompat getNotificationManager() {
        return notificationHelper.getNotificationManager();
    }
}
