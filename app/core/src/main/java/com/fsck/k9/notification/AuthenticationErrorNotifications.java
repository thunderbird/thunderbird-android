package com.fsck.k9.notification;


import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.app.NotificationManagerCompat;

import com.fsck.k9.Account;

import static com.fsck.k9.notification.NotificationHelper.NOTIFICATION_LED_BLINK_FAST;
import static com.fsck.k9.notification.NotificationHelper.NOTIFICATION_LED_FAILURE_COLOR;


class AuthenticationErrorNotifications {
    private final NotificationHelper notificationHelper;
    private final NotificationActionCreator actionCreator;
    private final NotificationResourceProvider resourceProvider;


    public AuthenticationErrorNotifications(NotificationHelper notificationHelper,
            NotificationActionCreator actionCreator, NotificationResourceProvider resourceProvider) {
        this.notificationHelper = notificationHelper;
        this.actionCreator = actionCreator;
        this.resourceProvider = resourceProvider;
    }

    public void showAuthenticationErrorNotification(Account account, boolean incoming) {
        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, incoming);

        PendingIntent editServerSettingsPendingIntent = createContentIntent(account, incoming);
        String title = resourceProvider.authenticationErrorTitle();
        String text =  resourceProvider.authenticationErrorBody(account.getDescription());

        NotificationCompat.Builder builder = notificationHelper
                .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
                .setSmallIcon(resourceProvider.getIconWarning())
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(editServerSettingsPendingIntent)
                .setStyle(new BigTextStyle().bigText(text))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ERROR);

        notificationHelper.configureNotification(builder, null, null,
                NOTIFICATION_LED_FAILURE_COLOR,
                NOTIFICATION_LED_BLINK_FAST, true);

        getNotificationManager().notify(notificationId, builder.build());
    }

    public void clearAuthenticationErrorNotification(Account account, boolean incoming) {
        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, incoming);
        getNotificationManager().cancel(notificationId);
    }

    PendingIntent createContentIntent(Account account, boolean incoming) {
        return incoming ? actionCreator.getEditIncomingServerSettingsIntent(account) :
                actionCreator.getEditOutgoingServerSettingsIntent(account);
    }

    private NotificationManagerCompat getNotificationManager() {
        return notificationHelper.getNotificationManager();
    }
}
