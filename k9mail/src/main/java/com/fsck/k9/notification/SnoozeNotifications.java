package com.fsck.k9.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.NotificationSetting;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mailstore.LocalMessage;

import static com.fsck.k9.notification.NotificationController.NOTIFICATION_LED_BLINK_SLOW;
import static com.fsck.k9.notification.NotificationController.platformSupportsExtendedNotifications;

/**
 * Created by tyler on 12/10/16.
 */

public class SnoozeNotifications extends MailNotifications {

    private static final String TAG = "SnoozeNotifications";

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

        controller.getNotificationManager().notify(notificationId, notification);
    }

    protected Notification buildSnoozeNotification(Account account, NotificationData notificationData,
                                                boolean silent, int notificationId) {

        NotificationCompat.Builder builder;

        boolean disableBigStyle = isPrivacyModeActive() || !platformSupportsExtendedNotifications();
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
                NOTIFICATION_LED_BLINK_SLOW,
                ringAndVibrate);

        builder.setSmallIcon(R.drawable.ic_action_snooze_light);

        return builder.build();
    }

    protected NotificationCompat.Builder createBigTextStyleSummaryNotification(Account account,
                                                                               NotificationHolder holder,
                                                                               boolean enableBigStyle,
                                                                               int notificationId) {

        NotificationCompat.Builder builder = createBigTextStyleNotification(account, holder, notificationId, enableBigStyle)
                ;//TODO(Tf): ??? .setGroupSummary(true);

        NotificationContent content = holder.content;
        addReplyAction(builder, content, notificationId);
        //addSnoozeAction(builder, content, notificationId);

        return builder;
    }

    private void addSnoozeAction(NotificationCompat.Builder builder, NotificationContent content, int notificationId) {
        int icon = getSnoozeActionIcon();
        String title = context.getString(R.string.snooze_action);

        MessageReference messageReference = content.messageReference;

        // TODO(tf): create way to launch ChooseSnooze
        PendingIntent pendingIntent =
                actionCreator.createReplyPendingIntent(messageReference, notificationId);

        builder.addAction(icon, title, pendingIntent);
    }

    private int getSnoozeActionIcon() {
        return R.drawable.ic_action_snooze_light;
    }
}
