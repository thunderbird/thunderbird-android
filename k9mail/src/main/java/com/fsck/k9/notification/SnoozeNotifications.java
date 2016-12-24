package com.fsck.k9.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.NotificationSetting;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mailstore.LocalMessage;

import java.util.concurrent.TimeUnit;

/**
 * Handles posting snoozed messages as notifications.
 */
public class SnoozeNotifications extends MailNotifications {

    private static final String TAG = "SnoozeNotifications";
    private static final boolean DEBUG = false;

    private static int curSnoozeIdx = 0;

    private NotificationContentCreator contentCreator;

    SnoozeNotifications(NotificationController controller, NotificationActionCreator actionCreator) {
        super(controller, actionCreator);

        contentCreator = new NotificationContentCreator(controller.getContext());
    }

    public static SnoozeNotifications newInstance(NotificationController controller,
                                                   NotificationActionCreator actionCreator) {
        return new SnoozeNotifications(controller, actionCreator);
    }

    public synchronized void showSnoozeNotification(MessageReference msg) {
        LocalMessage localMessage = msg.restoreToLocalMessage(context);
        Account account = msg.restoreAccount(context);

        if (localMessage == null || account == null) {
            Log.e(TAG, "Failed to recreate snooze message");
            return;
        }

        showSnoozeNotification(account, localMessage);
    }


    public synchronized void showSnoozeNotification(Account account, LocalMessage localMessage) {

        NotificationContent content = contentCreator.createFromMessage(account, localMessage);

        NotificationData notificationData = new NotificationData(account);
        AddNotificationResult result = notificationData.addNotificationContent(content);

        int notificationId = NotificationIds.getNewSnoozedMessageId(account, curSnoozeIdx++);
        Notification notification = buildSnoozeNotification(account, notificationData, false, notificationId);

        if (DEBUG) {
            Log.d(TAG, "showSnoozeNotification id: " + notificationId);
        }

        controller.getNotificationManager().notify(notificationId, notification);
    }

    protected Notification buildSnoozeNotification(Account account, NotificationData notificationData,
                                                boolean silent, int notificationId) {

        NotificationCompat.Builder builder;

        boolean disableBigStyle = isPrivacyModeActive() || !NotificationController.platformSupportsExtendedNotifications();
        NotificationHolder holder = notificationData.getHolderForLatestNotification();
        builder = createBigTextStyleSummaryNotification(account, holder, !disableBigStyle, notificationId);

        builder.setPriority(NotificationCompat.PRIORITY_HIGH);

        boolean ringAndVibrate = !silent;

        NotificationSetting notificationSetting = account.getNotificationSetting();
        controller.configureNotification(
                builder,
                (notificationSetting.shouldRing()) ? notificationSetting.getRingtone() : null,
                (notificationSetting.shouldVibrate()) ? notificationSetting.getVibration() : null,
                (notificationSetting.isLed()) ? notificationSetting.getLedColor() : null,
                NotificationController.NOTIFICATION_LED_BLINK_SLOW,
                ringAndVibrate);

        builder.setSmallIcon(R.drawable.ic_action_snooze_light);

        return builder.build();
    }

    protected NotificationCompat.Builder createBigTextStyleSummaryNotification(Account account,
                                                                               NotificationHolder holder,
                                                                               boolean enableBigStyle,
                                                                               int notificationId) {

        NotificationCompat.Builder builder = createBigTextStyleNotification(account, holder, notificationId, null, enableBigStyle);

        NotificationContent content = holder.content;
        addReplyAction(builder, content, notificationId);
        addSnoozeAction(builder, content, notificationId);

        return builder;
    }

    private void addSnoozeAction(NotificationCompat.Builder builder, NotificationContent content, int notificationId) {
        int icon = getSnoozeActionIcon();
        String title = context.getString(R.string.in_one_hour);

        PendingIntent pendingIntent = createSnoozePendingIntent(
                        content.messageReference,
                        notificationId,
                        DEBUG ? TimeUnit.MINUTES.toMillis(1) : TimeUnit.HOURS.toMillis(1)
        );

        builder.addAction(icon, title, pendingIntent);
    }

    private PendingIntent createSnoozePendingIntent(MessageReference messageReference, int notificationId, long snoozeDurationInMs) {
        Intent intent = new Intent(context, NotificationPublisher.class);
        intent.setAction(NotificationPublisher.ACTION_SNOOZE_AGAIN);
        NotificationPublisher.safelyAddMessageRefToIntent(intent, messageReference);
        intent.putExtra(NotificationPublisher.EXTRA_SNOOZE_DURATION_IN_MS, snoozeDurationInMs);
        intent.putExtra(NotificationPublisher.EXTRA_ORIGIN_NOTIFICATION_ID, notificationId);

        return PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private int getSnoozeActionIcon() {
        return R.drawable.ic_action_snooze_light;
    }
}
