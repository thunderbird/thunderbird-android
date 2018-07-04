package com.fsck.k9.notification;


import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationManagerCompat;

import com.fsck.k9.Account;
import com.fsck.k9.core.R;

import static com.fsck.k9.notification.NotificationHelper.NOTIFICATION_LED_BLINK_FAST;
import static com.fsck.k9.notification.NotificationHelper.NOTIFICATION_LED_FAILURE_COLOR;


class AuthenticationErrorNotifications {
    private final NotificationHelper notificationHelper;
    private final NotificationActionCreator actionCreator;


    public AuthenticationErrorNotifications(NotificationHelper notificationHelper,
            NotificationActionCreator actionCreator) {
        this.notificationHelper = notificationHelper;
        this.actionCreator = actionCreator;
    }

    public void showAuthenticationErrorNotification(Account account, boolean incoming) {
        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, incoming);
        Context context = notificationHelper.getContext();

        PendingIntent editServerSettingsPendingIntent = createContentIntent(account, incoming);
        String title = context.getString(R.string.notification_authentication_error_title);
        String text = context.getString(R.string.notification_authentication_error_text, account.getDescription());

        NotificationCompat.Builder builder = notificationHelper.createNotificationBuilder()
                .setSmallIcon(R.drawable.notification_icon_warning)
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
